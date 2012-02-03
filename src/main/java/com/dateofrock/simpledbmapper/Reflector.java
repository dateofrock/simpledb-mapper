/*
 *	Copyright 2012 Takehito Tanabe (dateofrock at gmail dot com)
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package com.dateofrock.simpledbmapper;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;
import com.dateofrock.simpledbmapper.s3.S3TaskResult;
import com.dateofrock.simpledbmapper.s3.S3TaskResult.Operation;
import com.dateofrock.simpledbmapper.util.IOUtils;

/**
 * {@link SimpleDBMapper}のためのリフレクションユーティリティ。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
class Reflector {

	List<Field> getDeclaredSuperFields(final Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		Field[] thisFields = clazz.getDeclaredFields();
		for (Field field : thisFields) {
			fields.add(field);
		}
		Class<?> cls = clazz;
		while (true) {
			Class<?> superClass = cls.getSuperclass();
			if (Object.class.equals(superClass)) {
				break;
			}
			thisFields = superClass.getDeclaredFields();
			for (Field field : thisFields) {
				fields.add(field);
			}
			cls = superClass;
		}
		return fields;
	}

	Field findFieldByAttributeName(Class<?> clazz, String attributeName) {
		List<Field> fields = getDeclaredSuperFields(clazz);
		for (Field field : fields) {
			// SimpleDBAttribute
			SimpleDBAttribute attribute = field.getAnnotation(SimpleDBAttribute.class);
			if (attribute != null) {
				if (attribute.attributeName().equals(attributeName)) {
					return field;
				}
			}
			//
			SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
			if (blob != null) {
				if (blob.attributeName().equals(attributeName)) {
					return field;
				}
			}
			SimpleDBVersionAttribute version = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (version != null) {
				if (version.attributeName().equals(attributeName)) {
					return field;
				}
			}
		}
		return null;
	}

	Field findItemNameField(Class<?> clazz) {
		List<Field> fields = getDeclaredSuperFields(clazz);
		for (Field field : fields) {
			SimpleDBItemName sdbItemName = field.getAnnotation(SimpleDBItemName.class);
			if (sdbItemName != null) {
				return field;
			}
		}
		return null;
	}

	Field findVersionAttributeField(Class<?> clazz) {
		List<Field> fields = getDeclaredSuperFields(clazz);
		for (Field field : fields) {
			SimpleDBVersionAttribute versionAttribute = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (versionAttribute != null) {
				return field;
			}
		}
		return null;
	}

	List<Field> findBlobFields(Class<?> clazz) {
		List<Field> list = new ArrayList<Field>();
		List<Field> fields = getDeclaredSuperFields(clazz);
		for (Field field : fields) {
			SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
			if (blob != null) {
				list.add(field);
			}
		}
		return list;
	}

	/**
	 * @throws SimpleDBMappingException
	 *             SimpleDBEntityアノテーションがない場合
	 */
	SimpleDBEntity getEntityAnnotation(Class<?> clazz) {
		SimpleDBEntity entity = clazz.getAnnotation(SimpleDBEntity.class);
		if (entity == null) {
			throw new SimpleDBMappingException(clazz + "は@SimpleDBEntityアノテーションがありません");
		}
		return entity;
	}

	String findDomainName(Class<?> clazz) {
		return getEntityAnnotation(clazz).domainName();
	}

	String findS3BucketName(Class<?> clazz) {
		return clazz.getAnnotation(SimpleDBEntity.class).s3BucketName();
	}

	String findS3KeyPrefix(Class<?> clazz) {
		return clazz.getAnnotation(SimpleDBEntity.class).s3KeyPrefix();
	}

	<T> void setFieldValueByAttribute(AmazonS3 s3, Class<T> clazz, T instance, Attribute attribute) {
		String attributeName = attribute.getName();
		String attributeValue = attribute.getValue();

		// version
		Field versionField = this.findVersionAttributeField(clazz);
		if (versionField != null) {
			String versionAttributeName = versionField.getAnnotation(SimpleDBVersionAttribute.class).attributeName();
			if (attributeName.equals(versionAttributeName)) {
				Long version = new Long(attributeValue);
				try {
					versionField.set(instance, version);
				} catch (Exception e) {
					throw new SimpleDBMappingException("versionセットに失敗", e);
				}
				return;
			}
		}

		List<Field> fields = getDeclaredSuperFields(clazz);
		for (Field field : fields) {
			try {
				// attribute/blob
				// TODO Blobのダウンロードは平行処理にしたい
				setAttributeAndBlobValueToField(s3, instance, field, attributeName, attributeValue);
			} catch (Exception e) {
				throw new SimpleDBMappingException("fieldのセットに失敗", e);
			}

		}
	}

	@SuppressWarnings("unchecked")
	private <T> void setAttributeAndBlobValueToField(AmazonS3 s3, T instance, Field field, String attributeName,
			String attributeValue) throws IllegalAccessException, ParseException {
		Class<?> type;
		type = field.getType();

		// SimpleDBAttribute
		SimpleDBAttribute sdbAttrAnnotation = field.getAnnotation(SimpleDBAttribute.class);
		if (sdbAttrAnnotation != null && sdbAttrAnnotation.attributeName().equals(attributeName)) {
			if (Set.class.isAssignableFrom(type)) {
				// Set
				Set<?> s = (Set<?>) field.get(instance);
				ParameterizedType genericType = (ParameterizedType) field.getGenericType();
				Class<?> setClass = (Class<?>) genericType.getActualTypeArguments()[0];
				if (Number.class.isAssignableFrom(setClass)) {
					// SetのメンバーがNumberの場合
					if (s == null) {
						Set<Number> newSet = new HashSet<Number>();
						field.set(instance, newSet);
						s = (Set<Number>) field.get(instance);
					}
					Number n = null;
					if (isIntegerType(setClass)) {
						n = new Integer(attributeValue);
					} else if (isFloatType(setClass)) {
						n = new Float(attributeValue);
					} else if (isLongType(setClass)) {
						n = new Long(attributeValue);
					}
					((Set<Number>) s).add(n);
					return;
				} else if (isStringType(setClass)) {
					// SetのメンバーがStringの場合
					if (s == null) {
						Set<String> newSet = new HashSet<String>();
						field.set(instance, newSet);
						s = (Set<String>) field.get(instance);
					}
					((Set<String>) s).add(attributeValue);
					return;
				} else {
					// FIXME
					throw new IllegalStateException("SetのgenericTypeはNumberかStringのみサポートしています");
				}
			} else if (isDateType(type)) {
				Date parsedDate = SimpleDBUtils.decodeDate(attributeValue);
				field.set(instance, parsedDate);
				return;
			} else if (isStringType(type)) {
				field.set(instance, attributeValue);
				return;
			} else if (isIntegerType(type)) {
				field.set(instance, new Integer(attributeValue));
				return;
			} else if (isFloatType(type)) {
				field.set(instance, new Float(attributeValue));
				return;
			} else if (isLongType(type)) {
				field.set(instance, new Long(attributeValue));
				return;
			} else if (isBooleanType(type)) {
				field.set(instance, new Boolean(attributeValue));
			} else {
				throw new SimpleDBMappingException("サポートしていない型です。" + type);
			}
		}

		// SimpleDBBlob
		SimpleDBBlob sdbBlobAnnotation = field.getAnnotation(SimpleDBBlob.class);
		if (sdbBlobAnnotation != null && sdbBlobAnnotation.attributeName().equals(attributeName)) {
			S3TaskResult taskResult = new S3TaskResult(Operation.DOWNLOAD, attributeName, null, null);
			taskResult.setSimpleDBAttributeValue(attributeValue);
			S3Object s3Obj = s3.getObject(taskResult.getBucketName(), taskResult.getKey());
			InputStream input = s3Obj.getObjectContent();
			if (isStringType(type)) {
				// FIXME encoding決めうち
				String stringValue = IOUtils.readString(input, "UTF-8");
				field.set(instance, stringValue);
			} else if (isPrimitiveByteArrayType(type)) {
				byte[] bytes = IOUtils.readBytes(input);
				field.set(instance, bytes);
			}
		}
	}

	String formattedString(Object object) {
		String itemNameInQuery = null;
		if (object instanceof String) {// String
			itemNameInQuery = (String) object;
		} else if (object instanceof Integer) {// Integer
			itemNameInQuery = SimpleDBUtils.encodeZeroPadding((Integer) object, SimpleDBEntity.MAX_NUMBER_DIGITS);
		} else if (object instanceof Long) {// Long
			itemNameInQuery = SimpleDBUtils.encodeZeroPadding((Long) object, SimpleDBEntity.MAX_NUMBER_DIGITS);
		} else if (object instanceof Float) {// Float
			itemNameInQuery = SimpleDBUtils.encodeZeroPadding((Float) object, SimpleDBEntity.MAX_NUMBER_DIGITS);
		} else {
			throw new SimpleDBMappingException("itemNameはStringかIntegerかLong、Floatのどれかである必要があります。" + object);
		}
		return itemNameInQuery;
	}

	String getItemNameAsSimpleDBFormat(Object object, Field itemNameField) {
		String itemName = null;
		try {
			Class<?> itemNameType = itemNameField.getType();
			if (isStringType(itemNameType)) {
				itemName = (String) itemNameField.get(object);
			} else if (isIntegerType(itemNameType)) {
				itemName = SimpleDBUtils.encodeZeroPadding((Integer) itemNameField.get(object),
						SimpleDBEntity.MAX_NUMBER_DIGITS);
			} else if (isFloatType(itemNameType)) {
				itemName = SimpleDBUtils.encodeZeroPadding((Float) itemNameField.get(object),
						SimpleDBEntity.MAX_NUMBER_DIGITS);
			} else if (isLongType(itemNameType)) {
				itemName = SimpleDBUtils.encodeZeroPadding((Long) itemNameField.get(object),
						SimpleDBEntity.MAX_NUMBER_DIGITS);
			} else {
				throw new SimpleDBMappingException(itemNameField + "はサポートしていない型です。");
			}
		} catch (Exception e) {
			throw new SimpleDBMappingException(object + "itemNameFieldから値を取得に失敗", e);
		}
		return itemName;
	}

	boolean isDateType(Class<?> type) {
		return Date.class.isAssignableFrom(type);
	}

	boolean isStringType(Class<?> type) {
		return String.class.isAssignableFrom(type);
	}

	boolean isBooleanType(Class<?> type) {
		return Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type);
	}

	boolean isLongType(Class<?> type) {
		return Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type);
	}

	boolean isFloatType(Class<?> type) {
		return Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type);
	}

	boolean isIntegerType(Class<?> type) {
		return Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type);
	}

	boolean isPrimitiveByteArrayType(Class<?> type) {
		return type.getSimpleName().equals("byte[]");
	}

}

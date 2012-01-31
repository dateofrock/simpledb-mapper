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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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

/**
 * {@link SimpleDBMapper}のためのリフレクションユーティリティ。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
class Refrector {

	Field findItemNameField(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			SimpleDBItemName sdbItemName = field.getAnnotation(SimpleDBItemName.class);
			if (sdbItemName != null) {
				return field;
			}
		}
		return null;
	}

	Field findVersionAttributeField(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
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
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
			if (blob != null) {
				list.add(field);
			}
		}
		return list;
	}

	String findDomainName(Class<?> clazz) {
		SimpleDBEntity entity = clazz.getAnnotation(SimpleDBEntity.class);
		if (entity == null) {
			throw new SimpleDBMappingException(clazz + "は@SimpleDBEntityアノテーションがありません");
		}
		return entity.domainName();
	}

	String findVersionAttributeName(Class<?> clazz) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			SimpleDBVersionAttribute versionAttribute = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (versionAttribute != null) {
				return versionAttribute.attributeName();
			}
		}
		throw new SimpleDBMappingException(clazz + "は@SimpleDBVersionAttributeアノテーションがありません");
	}

	<T> void setFieldValueByAttribute(AmazonS3 s3, Class<T> clazz, T instance, Attribute attribute) {
		String attributeName = attribute.getName();
		String attributeValue = attribute.getValue();

		// version
		if (attributeName.equals(this.findVersionAttributeName(clazz))) {
			Long version = new Long(attributeValue);
			Field versionField = this.findVersionAttributeField(clazz);
			try {
				versionField.set(instance, version);
			} catch (Exception e) {
				throw new SimpleDBMappingException("versionセットに失敗", e);
			}
			return;
		}

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			try {
				setFieldValue(s3, instance, field, attributeName, attributeValue);
			} catch (Exception e) {
				throw new SimpleDBMappingException("fieldのセットに失敗", e);
			}

		}
	}

	@SuppressWarnings("unchecked")
	private <T> void setFieldValue(AmazonS3 s3, T instance, Field field, String attributeName, String attributeValue)
			throws IllegalAccessException, ParseException {
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
				// String
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
				InputStreamReader reader = null;
				StringWriter writer = null;
				try {
					reader = new InputStreamReader(input, "UTF-8");
					writer = new StringWriter();
					int c;
					while ((c = reader.read()) != -1) {
						writer.write(c);
					}
					writer.flush();
					String stringValue = writer.toString();
					field.set(instance, stringValue);
				} catch (IOException e) {
					throw new SimpleDBMapperS3HandleException("S3よりオブジェクト読み込み失敗(String)", e);
				} finally {
					try {
						reader.close();
						writer.close();
					} catch (Exception ignore) {
					}
				}

			} else if (isPrimitiveByteArrayType(type)) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int c;
				try {
					while ((c = input.read()) != -1) {
						out.write(c);
					}
					out.flush();
					byte[] bytes = out.toByteArray();
					field.set(instance, bytes);
				} catch (IOException e) {
					throw new SimpleDBMapperS3HandleException("S3よりオブジェクト読み込み失敗(byte[])", e);
				} finally {
					try {
						out.close();
					} catch (IOException ignore) {
					}
				}

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

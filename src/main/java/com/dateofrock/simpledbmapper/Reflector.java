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

import static com.amazonaws.services.simpledb.util.SimpleDBUtils.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpledb.model.Attribute;
import com.dateofrock.simpledbmapper.s3.S3TaskResult;
import com.dateofrock.simpledbmapper.s3.S3TaskResult.Operation;
import com.dateofrock.simpledbmapper.util.IOUtils;

/**
 * {@link SimpleDBMapper}のためのリフレクションユーティリティ。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
class Reflector {

	Set<Field> listAllFields(final Class<?> clazz) {
		Set<Field> fields = new HashSet<Field>();
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
		for (Field field : listAllFields(clazz)) {
			// SimpleDBAttribute
			SimpleDBAttribute attr = field.getAnnotation(SimpleDBAttribute.class);
			if (attr != null) {
				if (attr.attributeName().isEmpty()) {
					if (field.getName().equals(attributeName)) {
						return field;
					}
				}
				if (attr.attributeName().equals(attributeName)) {
					return field;
				}
			}
			// SimpleDBBlob
			SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
			if (blob != null) {
				if (blob.attributeName().isEmpty()) {
					if (field.getName().equals(attributeName)) {
						return field;
					}
				}
				if (blob.attributeName().equals(attributeName)) {
					return field;
				}
			}
			// SimpleDBVersionAttribute
			SimpleDBVersionAttribute version = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (version != null) {
				if (version.attributeName().isEmpty()) {
					if (field.getName().equals(attributeName)) {
						return field;
					}
				}
				if (version.attributeName().equals(attributeName)) {
					return field;
				}
			}
		}
		return null;
	}

	Field findItemNameField(Class<?> clazz) {
		for (Field field : listAllFields(clazz)) {
			SimpleDBItemName sdbItemName = field.getAnnotation(SimpleDBItemName.class);
			if (sdbItemName != null) {
				return field;
			}
		}
		return null;
	}

	Field findVersionAttributeField(Class<?> clazz) {
		for (Field field : listAllFields(clazz)) {
			SimpleDBVersionAttribute versionAttribute = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (versionAttribute != null) {
				return field;
			}
		}
		return null;
	}

	Set<Field> findBlobFields(Class<?> clazz) {
		Set<Field> list = new HashSet<Field>();
		for (Field field : listAllFields(clazz)) {
			SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
			if (blob != null) {
				list.add(field);
			}
		}
		return list;
	}

	String getAttributeName(Field field) {
		// SimpleDBAttribute
		SimpleDBAttribute attr = field.getAnnotation(SimpleDBAttribute.class);
		String attributeName = null;
		if (attr != null) {
			attributeName = attr.attributeName();
			if (attributeName.isEmpty()) {
				return field.getName();
			}
			return attributeName;
		}
		// SimpleDBBlob
		SimpleDBBlob blob = field.getAnnotation(SimpleDBBlob.class);
		if (blob != null) {
			attributeName = blob.attributeName();
			if (attributeName.isEmpty()) {
				return field.getName();
			}
			return attributeName;
		}
		//
		SimpleDBVersionAttribute version = field.getAnnotation(SimpleDBVersionAttribute.class);
		if (version != null) {
			attributeName = version.attributeName();
			if (attributeName.isEmpty()) {
				return field.getName();
			}
			return attributeName;
		}
		return null;
	}

	SimpleDBDomain getDomainAnnotation(Class<?> clazz) {
		SimpleDBDomain entity = clazz.getAnnotation(SimpleDBDomain.class);
		if (entity == null) {
			throw new SimpleDBMapperException(clazz + " has no @SimpleDBDomain annotation");
		}
		return entity;
	}

	String getDomainName(Class<?> clazz) {
		return getDomainAnnotation(clazz).domainName();
	}

	String getS3BucketName(Class<?> clazz) {
		return getDomainAnnotation(clazz).s3BucketName();
	}

	String getS3KeyPrefix(Class<?> clazz) {
		return getDomainAnnotation(clazz).s3KeyPrefix();
	}

	String getS3ContentType(Field blobField) {
		SimpleDBBlob blob = blobField.getAnnotation(SimpleDBBlob.class);
		if (blob == null) {
			throw new SimpleDBMapperException(blobField + " has not @SimpleDBBlob annotation");
		}
		return blob.contentType();
	}

	<T> void setFieldValueFromAttribute(AmazonS3 s3, Class<T> clazz, T instance, Attribute attribute) {
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
					throw new SimpleDBMapperException("failed to set version", e);
				}
				return;
			}
		}

		Set<Field> fields = listAllFields(clazz);
		for (Field field : fields) {
			try {
				// attribute/blob
				// TODO Blobのダウンロードは平行処理にしたい
				setAttributeAndBlobValueToField(s3, instance, field, attributeName, attributeValue);
			} catch (Exception e) {
				throw new SimpleDBMapperException("failed to set field value", e);
			}

		}
	}

	@SuppressWarnings("unchecked")
	<T> void setAttributeAndBlobValueToField(AmazonS3 s3, T instance, Field field, String attributeName,
			String attributeValue) throws IllegalAccessException, ParseException {
		Class<?> type;
		type = field.getType();

		// SimpleDBAttribute
		SimpleDBAttribute sdbAttrAnnotation = field.getAnnotation(SimpleDBAttribute.class);
		if (sdbAttrAnnotation != null && getAttributeName(field).equals(attributeName)) {
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
					throw new SimpleDBMapperUnsupportedTypeException(s.toString() + " genericType: " + setClass
							+ " is not supported.");
				}
			} else if (isDateType(type)) {
				Date parsedDate = decodeDate(attributeValue);
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
				throw new SimpleDBMapperException("サポートしていない型です。" + type);
			}
		}

		// SimpleDBBlob
		SimpleDBBlob sdbBlobAnnotation = field.getAnnotation(SimpleDBBlob.class);
		if (sdbBlobAnnotation != null && getAttributeName(field).equals(attributeName)) {
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

	String encodeObjectAsSimpleDBFormat(Object object) {
		String value = null;
		Class<?> type = object.getClass();
		if (isStringType(type)) {
			value = (String) object;
		} else if (isIntegerType(type)) {
			value = encodeZeroPadding((Integer) object, SimpleDBDomain.MAX_NUMBER_DIGITS);
		} else if (isLongType(type)) {
			value = encodeZeroPadding((Long) object, SimpleDBDomain.MAX_NUMBER_DIGITS);
		} else if (isFloatType(type)) {
			value = encodeZeroPadding((Float) object, SimpleDBDomain.MAX_NUMBER_DIGITS);
		} else if (isDateType(type)) {
			value = encodeDate((Date) object);
		} else if (isBooleanType(type)) {
			value = Boolean.toString((Boolean) object);
		} else {
			throw new SimpleDBMapperUnsupportedTypeException(type + " is not supprted.");
		}
		return value;
	}

	String encodeItemNameAsSimpleDBFormat(Object object, Field itemNameField) {
		Class<?> type = itemNameField.getType();
		if (!isItemNameSupportedType(type)) {
			throw new SimpleDBMapperUnsupportedTypeException(type + " is not supprted.");
		}
		Object itemNameFieldValue = null;
		try {
			itemNameFieldValue = itemNameField.get(object);
		} catch (Exception e) {
			throw new SimpleDBMapperException(e);
		}
		String itemName = null;
		itemName = encodeObjectAsSimpleDBFormat(itemNameFieldValue);
		return itemName;
	}

	Object decodeItemNameFromSimpleDBFormat(Class<?> type, String itemName) {
		if (isIntegerType(type)) {
			return decodeZeroPaddingInt(itemName);
		} else if (isFloatType(type)) {
			return decodeZeroPaddingFloat(itemName);
		} else if (isLongType(type)) {
			return decodeZeroPaddingLong(itemName);
		} else if (isStringType(type)) {
			return itemName;
		}
		throw new SimpleDBMapperUnsupportedTypeException(type + " is not supprted.");
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

	boolean isAttributeField(Field field) {
		return (field.getAnnotation(SimpleDBAttribute.class) != null);
	}

	boolean isBlobField(Field field) {
		return (field.getAnnotation(SimpleDBBlob.class) != null);
	}

	boolean isItemNameSupportedType(Class<?> type) {
		return (isStringType(type) || isLongType(type) || isFloatType(type) || isIntegerType(type));
	}

	boolean isAttributeSupprtedType(Class<?> type) {
		return (isDateType(type) || isStringType(type) || isBooleanType(type) || isLongType(type) || isFloatType(type) || isIntegerType(type));
	}
}

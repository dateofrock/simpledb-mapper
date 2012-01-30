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
package com.dateofrock.aws.simpledb.datamodeling;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

/**
 * {@link SimpleDBMapper}のためのリフレクションユーティリティ。
 * 
 * @author dateofrock
 */
class Refrector {

	<T> Field findItemNameField(Class<T> clazz) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			SimpleDBItemName sdbItemName = field.getAnnotation(SimpleDBItemName.class);
			if (sdbItemName != null) {
				return field;
			}
		}
		return null;
	}

	<T> Field findVersionAttributeField(Class<T> clazz) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			SimpleDBVersionAttribute versionAttribute = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (versionAttribute != null) {
				return field;
			}
		}
		return null;
	}

	<T> void setFieldValueByAttribute(Class<T> clazz, T instance, Attribute attribute) {
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

		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			try {
				setFieldValue(instance, field, attributeName, attributeValue);
			} catch (Exception e) {
				throw new SimpleDBMappingException("fieldのセットに失敗", e);
			}

		}
	}

	@SuppressWarnings("unchecked")
	private <T> void setFieldValue(T instance, Field field, String attributeName, String attributeValue)
			throws IllegalAccessException, ParseException {
		Class<?> type;
		type = field.getType();

		// attributes
		SimpleDBAttribute sdbAttributeAnnotation = field.getAnnotation(SimpleDBAttribute.class);
		if (sdbAttributeAnnotation == null) {
			return;
		}
		if (sdbAttributeAnnotation.attributeName().equals(attributeName)) {
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

	<T> String findDomainName(Class<T> clazz) {
		SimpleDBEntity entity = clazz.getAnnotation(SimpleDBEntity.class);
		if (entity == null) {
			throw new SimpleDBMappingException(clazz + "は@SimpleDBEntityアノテーションがありません");
		}
		return entity.domainName();
	}

	<T> String findVersionAttributeName(Class<T> clazz) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			SimpleDBVersionAttribute versionAttribute = field.getAnnotation(SimpleDBVersionAttribute.class);
			if (versionAttribute != null) {
				return versionAttribute.attributeName();
			}
		}
		throw new SimpleDBMappingException(clazz + "は@SimpleDBVersionAttributeアノテーションがありません");
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

}

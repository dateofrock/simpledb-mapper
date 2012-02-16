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

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import model.BadTypeBooleanItemNameModel;
import model.BadTypeDateItemNameModel;
import model.Book;
import model.BookSubClass;
import model.FloatItemNameModel;
import model.IntegerItemNameModel;
import model.LongItemNameModel;
import model.StringItemNameModel;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class ReflectorTest {
	Reflector reflector;

	@Before
	public void setUp() throws Exception {
		this.reflector = new Reflector();
	}

	@Test
	public void listAllFields() throws Exception {
		Set<Field> expected = new HashSet<Field>();
		expected.add(Book.class.getDeclaredField("id"));
		expected.add(Book.class.getDeclaredField("title"));
		expected.add(Book.class.getDeclaredField("isbn"));
		expected.add(Book.class.getDeclaredField("authors"));
		expected.add(Book.class.getDeclaredField("publishedAt"));
		expected.add(Book.class.getDeclaredField("price"));
		expected.add(Book.class.getDeclaredField("height"));
		expected.add(Book.class.getDeclaredField("width"));
		expected.add(Book.class.getDeclaredField("available"));
		expected.add(Book.class.getDeclaredField("review"));
		expected.add(Book.class.getDeclaredField("coverImage"));
		expected.add(Book.class.getDeclaredField("version"));
		expected.add(BookSubClass.class.getDeclaredField("tags"));
		Set<Field> fields = this.reflector.listAllFields(BookSubClass.class);
		assertEquals(expected, fields);
	}

	@Test
	public void findFieldByAttributeName() throws Exception {
		Field expected = Book.class.getField("title");
		Field actual = this.reflector.findFieldByAttributeName(Book.class, "title");
		assertEquals(expected, actual);
		expected = Book.class.getField("publishedAt");
		actual = this.reflector.findFieldByAttributeName(Book.class, "publishedAt");
		assertEquals(expected, actual);
		expected = Book.class.getField("review");
		actual = this.reflector.findFieldByAttributeName(Book.class, "review");
		assertEquals(expected, actual);
		expected = Book.class.getField("version");
		actual = this.reflector.findFieldByAttributeName(Book.class, "version");
		assertEquals(expected, actual);
	}

	@Test
	public void testFindItemNameField() throws Exception {
		Field field = Book.class.getField("id");
		assertEquals(field, this.reflector.findItemNameField(Book.class));
	}

	@Test
	public void findVersionAttributeField() throws Exception {
		Field field = Book.class.getField("version");
		assertEquals(field, this.reflector.findVersionAttributeField(Book.class));
	}

	@Test
	public void findBlobFields() throws Exception {
		Set<Field> fields = new HashSet<Field>();
		fields.add(Book.class.getField("review"));
		fields.add(Book.class.getField("coverImage"));
		assertEquals(fields, this.reflector.findBlobFields(Book.class));
	}

	@Test
	public void getDomainAnnotation() {
		SimpleDBDomain domain = Book.class.getAnnotation(SimpleDBDomain.class);
		assertEquals(domain, this.reflector.getDomainAnnotation(Book.class));
	}

	@Test
	public void getDomainName() {
		assertEquals("SimpleDBMapper-Book", this.reflector.getDomainName(Book.class));
	}

	@Test
	public void getS3BucketName() {
		assertEquals("dateofrock-testing", this.reflector.getS3BucketName(Book.class));
	}

	@Test
	public void getS3KeyPrefix() {
		assertEquals("simpledb-mapper/", this.reflector.getS3KeyPrefix(Book.class));
	}

	// TODO
	// @Test
	// public void setFieldValueByAttribute() {
	// // this.reflector.setFieldValueByAttribute(s3, clazz, instance,
	// // attribute)
	// }

	// TODO
	// @Test
	// public void setAttributeAndBlobValueToField() {
	// // this.reflector.setAttributeAndBlobValueToField(s3, instance, field,
	// // attributeName, attributeValue);
	// }

	@Test
	public void encodeObjectAsSimpleDBFormat() throws Exception {
		String value = this.reflector.encodeObjectAsSimpleDBFormat("hoge");
		assertEquals("hoge", value);
		value = this.reflector.encodeObjectAsSimpleDBFormat(1);
		assertEquals("0000000001", value);
		value = this.reflector.encodeObjectAsSimpleDBFormat(123456L);
		assertEquals("0000123456", value);
		value = this.reflector.encodeObjectAsSimpleDBFormat(12.345f);
		assertEquals("0000000012.345", value);
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
		Date date = df.parse("2012/01/20 00:00:00 +0900");
		value = this.reflector.encodeObjectAsSimpleDBFormat(date);
		assertEquals("2012-01-20T00:00:00.000+09:00", value);
		assertEquals("true", this.reflector.encodeObjectAsSimpleDBFormat(true));
		try {
			this.reflector.encodeObjectAsSimpleDBFormat(new HashSet<Object>());
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}
	}

	@Test
	public void encodeItemNameAsSimpleDBFormat() throws Exception {
		StringItemNameModel stringItem = new StringItemNameModel();
		stringItem.itemName = "hoge";
		String value = this.reflector.encodeItemNameAsSimpleDBFormat(stringItem,
				StringItemNameModel.class.getDeclaredField("itemName"));
		assertEquals("hoge", value);

		IntegerItemNameModel integerItem = new IntegerItemNameModel();
		integerItem.itemName = 1;
		value = this.reflector.encodeItemNameAsSimpleDBFormat(integerItem,
				IntegerItemNameModel.class.getDeclaredField("itemName"));
		assertEquals("0000000001", value);

		LongItemNameModel longItem = new LongItemNameModel();
		longItem.itemName = 123456L;
		value = this.reflector.encodeItemNameAsSimpleDBFormat(longItem,
				LongItemNameModel.class.getDeclaredField("itemName"));
		assertEquals("0000123456", value);

		FloatItemNameModel floatItem = new FloatItemNameModel();
		floatItem.itemName = 12.345f;
		value = this.reflector.encodeItemNameAsSimpleDBFormat(floatItem,
				FloatItemNameModel.class.getDeclaredField("itemName"));
		assertEquals("0000000012.345", value);

		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
		Date date = df.parse("2012/01/20 00:00:00 +0900");
		BadTypeDateItemNameModel dateItem = new BadTypeDateItemNameModel();
		dateItem.itemName = date;
		try {
			this.reflector.encodeItemNameAsSimpleDBFormat(dateItem,
					BadTypeDateItemNameModel.class.getDeclaredField("itemName"));
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}

		BadTypeBooleanItemNameModel boolItem = new BadTypeBooleanItemNameModel();
		boolItem.itemName = true;
		try {
			this.reflector.encodeItemNameAsSimpleDBFormat(boolItem,
					BadTypeBooleanItemNameModel.class.getDeclaredField("itemName"));
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}

		try {
			this.reflector.encodeObjectAsSimpleDBFormat(new HashSet<Object>());
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}
	}

	@Test
	public void decodeItemNameFromSimpleDBFormat() throws Exception {
		Object value = this.reflector.decodeItemNameFromSimpleDBFormat(String.class, "hoge");
		assertEquals("hoge", value);
		value = this.reflector.decodeItemNameFromSimpleDBFormat(Integer.class, "0000000001");
		assertEquals(1, value);
		value = this.reflector.decodeItemNameFromSimpleDBFormat(Long.class, "0000123456");
		assertEquals(123456L, value);
		value = this.reflector.decodeItemNameFromSimpleDBFormat(Float.class, "0000000012.345");
		assertEquals(12.345f, value);

		try {
			value = this.reflector.decodeItemNameFromSimpleDBFormat(Date.class, "2012-01-20T00:00:00.000+09:00");
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}

		try {
			this.reflector.decodeItemNameFromSimpleDBFormat(Boolean.class, "true");
			fail("must throw SimpleDBMapperUnsupportedTypeException");
		} catch (Exception e) {
			assertEquals(SimpleDBMapperUnsupportedTypeException.class, e.getClass());
		}
	}

	@Test
	public void isDateType() {
		assertTrue(this.reflector.isDateType(Date.class));
		assertTrue(this.reflector.isDateType(Timestamp.class));
		assertTrue(this.reflector.isDateType(Time.class));
	}

	@Test
	public void isStringType() {
		assertTrue(this.reflector.isStringType(String.class));
		assertFalse(this.reflector.isStringType(CharSequence.class));
	}

	@Test
	public void isBooleanType() {
		assertTrue(this.reflector.isBooleanType(boolean.class));
		assertTrue(this.reflector.isBooleanType(Boolean.class));
	}

	@Test
	public void isLongType() {
		assertTrue(this.reflector.isLongType(long.class));
		assertTrue(this.reflector.isLongType(Long.class));
		assertFalse(this.reflector.isLongType(int.class));
		assertFalse(this.reflector.isLongType(Integer.class));
		assertFalse(this.reflector.isLongType(float.class));
		assertFalse(this.reflector.isLongType(Float.class));
		assertFalse(this.reflector.isLongType(double.class));
		assertFalse(this.reflector.isLongType(Double.class));
		assertFalse(this.reflector.isLongType(BigInteger.class));
		assertFalse(this.reflector.isLongType(BigDecimal.class));
		assertFalse(this.reflector.isLongType(Number.class));
	}

	@Test
	public void isFloatType() {
		assertFalse(this.reflector.isFloatType(long.class));
		assertFalse(this.reflector.isFloatType(Long.class));
		assertFalse(this.reflector.isFloatType(int.class));
		assertFalse(this.reflector.isFloatType(Integer.class));
		assertTrue(this.reflector.isFloatType(float.class));
		assertTrue(this.reflector.isFloatType(Float.class));
		assertFalse(this.reflector.isFloatType(double.class));
		assertFalse(this.reflector.isFloatType(Double.class));
		assertFalse(this.reflector.isFloatType(BigInteger.class));
		assertFalse(this.reflector.isFloatType(BigDecimal.class));
		assertFalse(this.reflector.isFloatType(Number.class));
	}

	@Test
	public void isIntegerType() {
		assertFalse(this.reflector.isIntegerType(long.class));
		assertFalse(this.reflector.isIntegerType(Long.class));
		assertTrue(this.reflector.isIntegerType(int.class));
		assertTrue(this.reflector.isIntegerType(Integer.class));
		assertFalse(this.reflector.isIntegerType(float.class));
		assertFalse(this.reflector.isIntegerType(Float.class));
		assertFalse(this.reflector.isIntegerType(double.class));
		assertFalse(this.reflector.isIntegerType(Double.class));
		assertFalse(this.reflector.isIntegerType(BigInteger.class));
		assertFalse(this.reflector.isIntegerType(BigDecimal.class));
		assertFalse(this.reflector.isIntegerType(Number.class));
	}

	@Test
	public void isPrimitiveByteArrayType() {
		assertTrue(this.reflector.isPrimitiveByteArrayType(byte[].class));
		assertFalse(this.reflector.isPrimitiveByteArrayType(byte.class));
		assertFalse(this.reflector.isPrimitiveByteArrayType(char[].class));
	}

	@Test
	public void getAttributeName() throws Exception {
		Field field = Book.class.getDeclaredField("title");
		String attributeName = this.reflector.getAttributeName(field);
		assertEquals("title", attributeName);
		field = Book.class.getDeclaredField("publishedAt");
		attributeName = this.reflector.getAttributeName(field);
		assertEquals("publishedAt", attributeName);
		field = Book.class.getDeclaredField("review");
		attributeName = this.reflector.getAttributeName(field);
		assertEquals("review", attributeName);
		field = Book.class.getDeclaredField("coverImage");
		attributeName = this.reflector.getAttributeName(field);
		assertEquals("coverImage", attributeName);
	}

	@Test
	public void isItemNameSupportedType() {
		assertFalse(this.reflector.isItemNameSupportedType(boolean.class));
		assertFalse(this.reflector.isItemNameSupportedType(Boolean.class));
		assertTrue(this.reflector.isItemNameSupportedType(String.class));
		assertTrue(this.reflector.isItemNameSupportedType(int.class));
		assertTrue(this.reflector.isItemNameSupportedType(Integer.class));
		assertTrue(this.reflector.isItemNameSupportedType(float.class));
		assertTrue(this.reflector.isItemNameSupportedType(Float.class));
		assertTrue(this.reflector.isItemNameSupportedType(long.class));
		assertTrue(this.reflector.isItemNameSupportedType(Long.class));
		assertFalse(this.reflector.isItemNameSupportedType(double.class));
		assertFalse(this.reflector.isItemNameSupportedType(Double.class));
		assertFalse(this.reflector.isItemNameSupportedType(Date.class));
		assertFalse(this.reflector.isItemNameSupportedType(BigInteger.class));
		assertFalse(this.reflector.isItemNameSupportedType(BigDecimal.class));
		assertFalse(this.reflector.isItemNameSupportedType(Number.class));
		assertFalse(this.reflector.isItemNameSupportedType(byte[].class));
	}

	@Test
	public void isAttributeSupprtedType() {
		assertTrue(this.reflector.isAttributeSupprtedType(boolean.class));
		assertTrue(this.reflector.isAttributeSupprtedType(Boolean.class));
		assertTrue(this.reflector.isAttributeSupprtedType(String.class));
		assertTrue(this.reflector.isAttributeSupprtedType(int.class));
		assertTrue(this.reflector.isAttributeSupprtedType(Integer.class));
		assertTrue(this.reflector.isAttributeSupprtedType(float.class));
		assertTrue(this.reflector.isAttributeSupprtedType(Float.class));
		assertTrue(this.reflector.isAttributeSupprtedType(long.class));
		assertTrue(this.reflector.isAttributeSupprtedType(Long.class));
		assertFalse(this.reflector.isAttributeSupprtedType(double.class));
		assertFalse(this.reflector.isAttributeSupprtedType(Double.class));
		assertTrue(this.reflector.isAttributeSupprtedType(Date.class));
		assertFalse(this.reflector.isAttributeSupprtedType(BigInteger.class));
		assertFalse(this.reflector.isAttributeSupprtedType(BigDecimal.class));
		assertFalse(this.reflector.isAttributeSupprtedType(Number.class));
		assertFalse(this.reflector.isAttributeSupprtedType(byte[].class));
	}
}

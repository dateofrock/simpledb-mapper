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
import java.util.HashSet;
import java.util.Set;

import model.Book;

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

	// @Test
	// public void testListAllFields() {
	// fail("Not yet implemented");
	// }

	@Test
	public void testFindFieldByAttributeName() throws Exception {
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
	public void testFindVersionAttributeField() throws Exception {
		Field field = Book.class.getField("version");
		assertEquals(field, this.reflector.findVersionAttributeField(Book.class));
	}

	@Test
	public void testFindBlobFields() throws Exception {
		Set<Field> fields = new HashSet<Field>();
		fields.add(Book.class.getField("review"));
		fields.add(Book.class.getField("coverImage"));
		assertEquals(fields, this.reflector.findBlobFields(Book.class));
	}

	@Test
	public void testGetDomainAnnotation() {
		SimpleDBDomain domain = Book.class.getAnnotation(SimpleDBDomain.class);
		assertEquals(domain, this.reflector.getDomainAnnotation(Book.class));
	}

	@Test
	public void testGetDomainName() {
		assertEquals("SimpleDBMapper-Book", this.reflector.getDomainName(Book.class));
	}

	@Test
	public void testGetS3BucketName() {
		assertEquals("dateofrock-testing", this.reflector.getS3BucketName(Book.class));
	}

	@Test
	public void testGetS3KeyPrefix() {
		assertEquals("simpledb-mapper/", this.reflector.getS3KeyPrefix(Book.class));
	}

	//
	// @Test
	// public void testSetFieldValueByAttribute() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testToStringAsSimpleDBFormat() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testGetItemNameAsSimpleDBFormat() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsDateType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsStringType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsBooleanType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsLongType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsFloatType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsIntegerType() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testIsPrimitiveByteArrayType() {
	// fail("Not yet implemented");
	// }

	@Test
	public void testGetAttributeName() throws Exception {
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

}

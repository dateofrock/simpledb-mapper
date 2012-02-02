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

import static com.dateofrock.simpledbmapper.query.ComparisonOperator.Equals;
import static com.dateofrock.simpledbmapper.query.ComparisonOperator.Like;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import model.Book;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.dateofrock.simpledbmapper.query.ComparisonOperator;
import com.dateofrock.simpledbmapper.query.Condition;
import com.dateofrock.simpledbmapper.query.Ordering;
import com.dateofrock.simpledbmapper.query.QueryExpression;
import com.dateofrock.simpledbmapper.query.Sort;
import com.dateofrock.simpledbmapper.util.IOUtils;

/**
 * 注意：このテストを実行すると、実際にSimpleDB/S3にアクセスします。
 * 
 * APIのエンドポイントは {@link SimpleDBMapperTest#simpleDBAPIEndPoint}で設定します。 ドメインは
 * {@link Book#id}の{@link SimpleDBItemName}アノテーションで指定されています。Blob用のS3バケットは
 * {@link Book#review}と{@link Book#coverImage}に{@link SimpleDBBlob}
 * アノテーションで指定されています。
 * 
 * これらの値を書き換えない場合は、最悪実在するSimpleDBのドメインやS3バケットの上書きをしてしまう可能性があるのでご注意ください。
 * 
 * @author dateofrock
 */
public class SimpleDBMapperTest {

	String simpleDBAPIEndPoint = "sdb.ap-northeast-1.amazonaws.com";

	private SimpleDBMapper mapper;

	@Before
	public void setUp() throws Exception {
		AWSCredentials cred = new PropertiesCredentials(
				SimpleDBMapperTest.class.getResourceAsStream("/AwsCredentials.properties"));

		AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
		sdb.setEndpoint(this.simpleDBAPIEndPoint);
		AmazonS3 s3 = new AmazonS3Client(cred);

		this.mapper = new SimpleDBMapper(sdb, s3);

		// this.mapper.dropDomainIfEmpty(Book.class);
		// this.mapper.createDomain(Book.class);

		List<Book> allBooks = this.mapper.selectAll(Book.class);
		for (Book book : allBooks) {
			this.mapper.delete(book);
		}
	}

	@Test
	public void test() throws Exception {
		Book book1 = newBook1(1000L);
		Book book2 = newBook2(2000L);
		this.mapper.save(book1);

		Book fetchedBook = this.mapper.load(Book.class, book1.id);
		assertBook(book1, fetchedBook);

		this.mapper.save(book2);
		fetchedBook = this.mapper.load(Book.class, book2.id);
		assertBook(book2, fetchedBook);

		book2.authors.remove("恥 晒");
		this.mapper.save(book2);
		fetchedBook = this.mapper.load(Book.class, book2.id);
		assertBook(book2, fetchedBook);

		QueryExpression expression = new QueryExpression(new Condition("title", Equals, "ドン引きの美学"));
		int count = this.mapper.count(Book.class, expression);
		assertEquals(1, count);

		expression = new QueryExpression(new Condition("title", Like, "%美学"));
		count = this.mapper.count(Book.class, expression);
		assertEquals(1, count);

		expression = new QueryExpression(new Condition("publishedAt", ComparisonOperator.GreaterThan,
				toDate("2000-1-1 00:00:00")));
		Sort sort = new Sort(Ordering.ASC, "publishedAt");
		expression.setSort(sort);

		List<Book> books = this.mapper.select(Book.class, expression);
		assertBook(book1, books.get(0));
		assertBook(book2, books.get(1));

		sort = new Sort(Ordering.DESC, "publishedAt");
		expression.setSort(sort);

		books = this.mapper.select(Book.class, expression);
		assertBook(book1, books.get(1));
		assertBook(book2, books.get(0));

		expression = new QueryExpression(new Condition("publishedAt", ComparisonOperator.GreaterThan,
				toDate("2000-1-1 00:00:00")));
		expression
				.addAndCondtion(new Condition("publishedAt", ComparisonOperator.LessThan, toDate("2100-1-1 00:00:00")));
		sort = new Sort(Ordering.ASC, "publishedAt");
		expression.setSort(sort);
		books = this.mapper.select(Book.class, expression);
		assertBook(book1, books.get(0));
		assertBook(book2, books.get(1));

		this.mapper.delete(book1);
		count = this.mapper.countAll(Book.class);
		assertEquals(1, count);

		this.mapper.delete(book2);
		count = this.mapper.countAll(Book.class);
		assertEquals(0, count);

	}

	@Test
	public void testManyBooks() throws Exception {
		List<Book> manyBooks = new ArrayList<Book>();
		for (int i = 0; i < 10; i++) {
			Book book = newBook1((long) i);
			book.price += i;
			manyBooks.add(book);
			this.mapper.save(book);
		}

		assertEquals(10, this.mapper.countAll(Book.class));

		QueryExpression expression = new QueryExpression(
				new Condition("itemName()", ComparisonOperator.IsNotNull, null));
		expression.setLimit(5);
		expression.setSort(new Sort("itemName()"));

		List<Book> fetchedBooks = this.mapper.select(Book.class, expression);
		while (this.mapper.hasNext()) {
			fetchedBooks.addAll(this.mapper.select(Book.class, expression));
		}

		List<Long> itemNameList = new ArrayList<Long>();
		for (Book book : fetchedBooks) {
			itemNameList.add(book.id);
		}
		Collections.sort(itemNameList);
		assertEquals(10, itemNameList.size());
		Long expectedItemName = 0L;
		for (Long itemName : itemNameList) {
			assertEquals(expectedItemName, itemName);
			expectedItemName += 1;
		}
	}

	private void assertBook(Book book, Book fetchedBook) {
		assertEquals(book.id, fetchedBook.id);
		assertEquals(book.isbn, fetchedBook.isbn);
		assertEquals(book.price, fetchedBook.price);
		assertEquals(book.publishedAt, fetchedBook.publishedAt);
		assertEquals(book.authors.size(), fetchedBook.authors.size());
		assertEquals(book.authors, fetchedBook.authors);
		assertEquals(book.width, fetchedBook.width);
		assertEquals(book.height, fetchedBook.height);
		assertEquals(book.available, fetchedBook.available);
		assertEquals(book.review, fetchedBook.review);
		assertArrayEquals(book.coverImage, fetchedBook.coverImage);
		assertEquals(book.version, fetchedBook.version);
	}

	private Date toDate(String value) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return sdf.parse(value);
	}

	private String readUTF8String(String resourceName) {
		InputStream instr = this.getClass().getResourceAsStream(resourceName);
		return IOUtils.readString(instr, "UTF-8");
	}

	private byte[] readBytes(String resourceName) {
		InputStream instr = this.getClass().getResourceAsStream(resourceName);
		return IOUtils.readBytes(instr);
	}

	private Book newBook1(Long itemName) throws Exception {
		Book book = new Book();
		book.id = itemName;
		book.title = "ドン引きの美学";
		book.authors = new HashSet<String>();
		book.authors.add("氷点空気愛好会");
		book.price = 1280;
		book.publishedAt = toDate("2012-1-20 00:00:00");
		book.isbn = "1234567890";
		book.width = 18.2f;
		book.height = 25.6f;
		book.available = true;
		book.review = readUTF8String("/book1.review.txt");
		book.coverImage = readBytes("/book1.cover.jpg");
		return book;
	}

	private Book newBook2(Long itemName) throws Exception {
		Book book = new Book();
		book.id = itemName;
		book.title = "スベらないプレゼン";
		book.authors = new HashSet<String>();
		book.authors.add("恥 忍");
		book.authors.add("恥 晒");
		book.price = 480;
		book.publishedAt = toDate("2015-3-10 00:00:00");
		book.isbn = "0987654321";
		book.width = 18.2f;
		book.height = 23.0f;
		book.available = false;
		book.review = readUTF8String("/book2.review.txt");
		book.coverImage = readBytes("/book2.cover.jpg");
		return book;
	}

}

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

import static com.dateofrock.aws.simpledb.datamodeling.query.ComparisonOperator.Equals;
import static com.dateofrock.aws.simpledb.datamodeling.query.ComparisonOperator.Like;
import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import model.Book;

import org.junit.Before;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.dateofrock.aws.simpledb.datamodeling.query.ComparisonOperator;
import com.dateofrock.aws.simpledb.datamodeling.query.Condition;
import com.dateofrock.aws.simpledb.datamodeling.query.QueryExpression;
import com.dateofrock.aws.simpledb.datamodeling.query.Sort;

/**
 * 注意：このテストを実行すると、実際にSimpleDBにアクセスします。
 * 
 * APIのエンドポイントは {@link SimpleDBMapperTest#simpleDBAPIEndPoint}で設定します。 ドメインは
 * {@link Book#id}の{@link SimpleDBItemName}アノテーションで指定されています。
 * 
 * これらの値を書き換えない場合は、東京リージョンでテストを行い、ドメイン名はBookを使います。
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

		this.mapper = new SimpleDBMapper(sdb);
	}

	// @Test
	public void test() throws Exception {
		Book book1 = newBook1(123L);
		Book book2 = newBook2(456L);
		this.mapper.save(book1);
		Book fetchedBook = this.mapper.load(Book.class, book1.id, true);
		assertBook(book1, fetchedBook);

		this.mapper.save(book2);
		fetchedBook = this.mapper.load(Book.class, book2.id, true);
		assertBook(book2, fetchedBook);

		book1.authors.remove("著者A");
		this.mapper.save(book1);
		fetchedBook = this.mapper.load(Book.class, book1.id, true);
		assertBook(book1, fetchedBook);

		QueryExpression expression = new QueryExpression(new Condition("title", Equals, "すごい本"));
		int count = this.mapper.count(Book.class, expression, true);
		assertEquals(1, count);

		expression = new QueryExpression(new Condition("title", Like, "%本"));
		Sort sort = new Sort("title");
		expression.setSort(sort);

		List<Book> books = this.mapper.query(Book.class, expression, true);
		assertBook(book1, books.get(1));
		assertBook(book2, books.get(0));

		expression = new QueryExpression(new Condition("publishedAt", ComparisonOperator.IsNotNull, null));
		sort = new Sort("publishedAt");
		expression.setSort(sort);

		books = this.mapper.query(Book.class, expression, true);
		assertBook(book1, books.get(0));
		assertBook(book2, books.get(1));

		count = this.mapper.count(Book.class, expression, true);
		assertEquals(2, count);

		this.mapper.delete(book1);
		count = this.mapper.countAll(Book.class, true);
		assertEquals(1, count);

		this.mapper.delete(book2);
		count = this.mapper.countAll(Book.class, true);
		assertEquals(0, count);

	}

	// @Test
	// public void testManyBooks() throws Exception {
	// List<Book> manyBooks = new ArrayList<Book>();
	// for (int i = 0; i < 2600; i++) {
	// Book book = newBook1((long) i);
	// book.price += i;
	// manyBooks.add(book);
	// this.mapper.save(book);
	// }
	//
	// assertEquals(2600, this.mapper.countAll(Book.class, true));
	//
	// QueryExpression expression = new QueryExpression(
	// new Condition("itemName()", ComparisonOperator.IsNotNull, null));
	// expression.setLimit(2500);
	// expression.setSort(new Sort("itemName()"));
	//
	// List<Book> fetchedBooks = this.mapper.query(Book.class, expression,
	// true);
	// if (this.mapper.hasNext()) {
	// fetchedBooks.addAll(this.mapper.query(Book.class, expression, true));
	// }
	//
	// List<Long> itemNameList = new ArrayList<Long>();
	// for (Book book : fetchedBooks) {
	// itemNameList.add(book.id);
	// }
	// Collections.sort(itemNameList);
	// assertEquals(2600, itemNameList.size());
	// Long expectedItemName = 0L;
	// for (Long itemName : itemNameList) {
	// assertEquals(expectedItemName, itemName);
	// expectedItemName += 1;
	// }
	// }

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
		assertEquals(book.version, fetchedBook.version);
	}

	private Date toDate(String value) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return sdf.parse(value);
	}

	private Book newBook1(Long itemName) throws ParseException {
		Book book = new Book();
		book.id = itemName;
		book.title = "面白い本";
		book.authors = new HashSet<String>();
		book.authors.add("著者A");
		book.authors.add("著者B");
		book.price = 1280;
		book.publishedAt = toDate("2012-1-20 00:00:00");
		book.isbn = "1234567890";
		book.width = 18.2f;
		book.height = 25.6f;
		book.available = true;
		return book;
	}

	private Book newBook2(Long itemName) throws ParseException {
		Book book = new Book();
		book.id = itemName;
		book.title = "すごい本";
		book.authors = new HashSet<String>();
		book.authors.add("著者X");
		book.price = 480;
		book.publishedAt = toDate("2015-3-10 00:00:00");
		book.isbn = "0987654321";
		book.width = 18.2f;
		book.height = 23.0f;
		book.available = false;
		return book;
	}

}

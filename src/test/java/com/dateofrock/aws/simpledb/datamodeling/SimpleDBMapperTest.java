package com.dateofrock.aws.simpledb.datamodeling;

import static com.dateofrock.aws.simpledb.datamodeling.query.ComparisonOperator.Equals;
import static com.dateofrock.aws.simpledb.datamodeling.query.ComparisonOperator.Like;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import model.Book;

import org.junit.Before;
import org.junit.Test;

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
	private Book book1, book2;

	@Before
	public void setUp() throws Exception {
		AWSCredentials cred = new PropertiesCredentials(
				SimpleDBMapperTest.class.getResourceAsStream("/AwsCredentials.properties"));
		AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
		sdb.setEndpoint(this.simpleDBAPIEndPoint);

		this.mapper = new SimpleDBMapper(sdb);
		this.book1 = new Book();
		this.book1.id = 123L;
		this.book1.title = "面白い本";
		this.book1.authors = new HashSet<String>();
		this.book1.authors.add("著者A");
		this.book1.authors.add("著者B");
		this.book1.price = 1280;
		this.book1.publishedAt = toDate("2012-1-20 00:00:00");
		this.book1.isbn = "1234567890";
		this.book1.width = 18.2f;
		this.book1.height = 25.6f;
		this.book1.available = true;

		this.book2 = new Book();
		this.book2.id = 456L;
		this.book2.title = "すごい本";
		this.book2.authors = new HashSet<String>();
		this.book2.authors.add("著者X");
		this.book2.price = 480;
		this.book2.publishedAt = toDate("2015-3-10 00:00:00");
		this.book2.isbn = "0987654321";
		this.book2.width = 18.2f;
		this.book2.height = 23.0f;
		this.book2.available = false;
	}

	@Test
	public void test() throws Exception {
		this.mapper.save(this.book1);
		Book fetchedBook = this.mapper.load(Book.class, this.book1.id, true);
		assertBook(this.book1, fetchedBook);

		this.mapper.save(this.book2);
		fetchedBook = this.mapper.load(Book.class, this.book2.id, true);
		assertBook(this.book2, fetchedBook);

		this.book1.authors.remove("著者A");
		this.mapper.save(this.book1);
		fetchedBook = this.mapper.load(Book.class, this.book1.id, true);
		assertBook(this.book1, fetchedBook);

		QueryExpression expression = new QueryExpression(new Condition("title", Equals, "すごい本"));
		int count = this.mapper.count(Book.class, expression, true);
		assertEquals(1, count);

		expression = new QueryExpression(new Condition("title", Like, "%本"));
		expression.addAndCondtion(new Condition("itemName()", ComparisonOperator.IsNotNull, null));
		Sort sort = new Sort("itemName()");
		expression.setSort(sort);

		List<Book> books = this.mapper.query(Book.class, expression, true);
		assertBook(this.book1, books.get(0));
		assertBook(this.book2, books.get(1));

		count = this.mapper.count(Book.class, expression, true);
		assertEquals(2, count);

		this.mapper.delete(this.book1);
		count = this.mapper.countAll(Book.class, true);
		assertEquals(1, count);
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
		assertEquals(book.version, fetchedBook.version);
	}

	private Date toDate(String value) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return sdf.parse(value);
	}

}

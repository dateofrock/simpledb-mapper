simpledb-mapper
===================
simpledb-mapperは、Amazon SimpleDBのデータマッパーユーティリティです。ドメインをPOJOとして表現し、アノテーションをつけるだけでCRUD操作が可能です。（ただし制限があります）
（執筆中）

Install
----
Mavenのリポジトリを用意してありますので、pom.xmlに以下の記述を追加してください。
（執筆中）


Usage
----

```java
@SimpleDBEntity(domainName = "SimpleDBMapper-Book-Testing")
 public class Book {
 
        @SimpleDBItemName
        public Long id;
 
        @SimpleDBAttribute(attributeName = "title")
        public String title;
 
        @SimpleDBAttribute(attributeName = "isbn")
        public String isbn;
 
        @SimpleDBAttribute(attributeName = "authors")
        public Set<String> authors;
 
        @SimpleDBAttribute(attributeName = "publishedAt")
        public Date publishedAt;
 
        @SimpleDBAttribute(attributeName = "price")
        public Integer price;
 
        @SimpleDBAttribute(attributeName = "height")
        public Float height;
 
        @SimpleDBAttribute(attributeName = "width")
        public Float width;
 
        @SimpleDBAttribute(attributeName = "available")
        public boolean available;
 
        @SimpleDBVersionAttribute
        public Long version;
 
 }
```

```java
 AWSCredentials cred = new PropertiesCredentials(
                SimpleDBMapperTest.class.getResourceAsStream("/AwsCredentials.properties"));
 AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
 sdb.setEndpoint("sdb.ap-northeast-1.amazonaws.com");
 
 SimpleDBMapper mapper = new SimpleDBMapper(sdb);
 
 Book book1 = new Book();
 book1.id = 123L;
 book1.title = "面白い本";
 book1.authors = new HashSet<String>();
 book1.authors.add("著者A");
 book1.authors.add("著者B");
 book1.price = 1280;
 book1.publishedAt = toDate("2012-1-20 00:00:00");
 book1.isbn = "1234567890";
 book1.width = 18.2f;
 book1.height = 25.6f;
 book1.available = true;
 
 mapper.save(book1);
 
 book1.authors.remove("著者A");
 mapper.save(book1);
 Book fetchedBook = mapper.load(Book.class, book1.id, true);
 
 SimpleDBQueryExpression expression = new SimpleDBQueryExpression(new Condition("title", ComparisonOperator.Equals,
                "すごい本"));
 int count = mapper.count(Book.class, expression, true);
 
 expression = new SimpleDBQueryExpression(new Condition("title", ComparisonOperator.Like, "%本"));
 Sort sort = new Sort("title");
 expression.setSort(sort);
 
 List<Book> books = this.mapper.query(Book.class, expression, true);
 
 mapper.delete(book1);
 count = mapper.countAll(Book.class, true);
```


Query
---



Author
======

Takehito Tanabe - [dateofrock](http://blog.dateofrock.com/)
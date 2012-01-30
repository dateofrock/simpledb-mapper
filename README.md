simpledb-mapper
===================
simpledb-mapperは、Amazon SimpleDBのデータマッパーユーティリティです。ドメインをPOJOとして表現し、アノテーションをつけるだけでSimpleDBに永続化が可能です。（いくつか制限があります）
イメージとしてはこのような感じです。

```java
AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);;
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
```

Usage
----
始めに、SimpleDBにドメインを作成します。simpledb-mapperにはドメイン作成機能はありません。AWS Management Consoleを利用するか、APIを利用してあらかじめドメインを作成しておいてください。

SimpleDBに永続化したいモデルをPOJOとして表現し、そこにsimpledb-mapperが用意してあるアノテーションを付け加えます。以下、サンプルとしてBookというPOJOを例に解説します。
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


### @SimpleDBEntity
@SimpleDBEntityアノテーションでは、POJOを永続化したいSimpleDBのドメインを指定します。この例で言うと、「SimpleDBMapper-Book-Testing」に永続化されます。（リレーショナルデータベースで言い換えれば、テーブルの指定に相当します。）

###@SimpleDBItemName
@SimpleDBItemNameアノテーションを指定されたpublicフィールドは、SimpleDBのItemNameにひもづけられます。（リレーショナルデータベースで言い換えれば、プライマリキーに相当します。）
ItemNameとして指定できる型は以下に制約されます。

* java.lang.String
* java.lang.Integer
* java.lang.Float
* java.lang.Long

###@SimpleDBAttribute
@SimpleDBAttributeアノテーションを指定されたpublicフィールドは、SimpleDBのattributeにひもづけられます。（リレーショナルデータベースで言い換えれば、カラムに相当します。）
attributeとして指定できる型は以下に制約されます。

* java.lang.String
* java.lang.Integer
* java.lang.Float
* java.lang.Long
* java.util.Date
* java.util.Set<java.lang.String>
* java.util.Set<java.lang.Integer>
* java.util.Set<java.lang.Float>
* java.util.Set<java.lang.Long>
* java.util.Set<java.util.Date>


###@SimpleDBVersionAttribute
@SimpleDBVersionAttributeアノテーションで指定されたpublicフィールドは、SimpleDBにアイテムをPUT/DELETEする際にトランザクション制御（楽観的ロック）を実現するためのフィールドになります。このフィールドはsimpledb-mapperが内部的に使用するものです。この指定は必須ではありません。
指定できる型は

* java.lang.Long

のみです。


>@SimpleDBItemName、@SimpleDBAttribute、@SimpleDBVersionAttributeのアノテーションはフィールドにのみ対応します。getter/setterメソッドのサポートは現状ありません。



Install
----
Mavenのリポジトリを用意してありますので、pom.xmlに以下の記述を追加してください。

```xml
<repositories>
	<repository>
		<id>dateofrock</id>
		<url>https://s3-ap-northeast-1.amazonaws.com/dateofrock-repository/maven/</url>
	</repository>
</repositories>
<dependencies>
	<dependency>
		<groupId>com.dateofrock.aws</groupId>
		<artifactId>simpledb-mapper</artifactId>
		<version>使用したいバージョン</version>
	</dependency>
</dependencies>
```

なお、simpledb-mapperはAWS SDK for Javaライブラリを利用しています。

Usage
----
はじめにAWSのSDKで用意されている[AmazonSimpleDB](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpledb/AmazonSimpleDB.html)のインスタンスを作ります。その際、使いたいリージョンがUS-EAST以外の場合は、相応のEndPoint指定が必要です。それをsimpledb-mapperのSimpleDBMapperのコンストラクタに渡してください。


```java
AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);;
AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
sdb.setEndpoint("sdb.ap-northeast-1.amazonaws.com"); 
SimpleDBMapper mapper = new SimpleDBMapper(sdb);
```
 
@SimpleDBEntityアノテーションが指定されたPOJOを用意します。simpledb-mapperではItemNameの自動生成をサポートしていませんので、自分で作る必要があります。

```java
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
```
SimpleDBMapper#save()でSimpleDBに永続化されます。

```java
mapper.save(book1);
```

book1のattributeを変更して、再度save()すると、上書き保存されます。（リレーショナルデータベースで言うところのUPDATEになります。） 

```java
book1.authors.remove("著者A");
mapper.save(book1);
```

SimpleDBに永続化されたPOJOを取得したい場合はやり方が二種類あります。一つはItemNameを指定する方法、もう一つはQueryを投げる方法です。両方とも、最後の引数booleanはConsistent Readオプションです。


```java
// ItemNameを指定する方法
Book fetchedBook = mapper.load(Book.class, 123L, true);

// Queryを指定する方法 
Condition condition = new Condition("title", ComparisonOperator.Equals, "すごい本");
QueryExpression expression = new QueryExpression(condition);
Sort sort = new Sort("title");
expression.setSort(sort);
 
List<Book> books = mapper.query(Book.class, expression, true);
```

booksが大量にある場合、QueryExpressionにLimitをセットしてください。セットしない場合はSimpleDBのデフォルト値である100がセットされます。また、セットできる最大値はSimpleDBの制限から2500です。

```java
expression.setLimit(1000);
```

queryを投げた際に、さらに残りのアイテムがある場合は、SimpleDBMapper#hasNext()を呼んでください。つまりコードは以下のようになるでしょう。

```java
List<Book> books = mapper.query(Book.class, expression, true);
if (mapper.hasNext()) {
	books.addAll(Book.class, expression, true);
}
```


削除する場合は、ItemNameに値が入っているPOJOを引数にdelete()を呼びます。

```java
mapper.delete(book1);
```

アイテムのカウントをする事も可能です。条件なしにすべてのアイテムをカウントする方法と条件付きカウントの二種類があります。

```java
// 条件なし
int count = count = mapper.countAll(Book.class, true);
// 条件あり
count = mapper.count(Book.class, expression, true);
```



Limitation
==============

* @SimpleDBAttributeアノテーションはフィールドにのみ指定できます。Javaで一般的なgetter/setterメソッドには非対応です。
* @SimpleDBItemNameの自動発行機能（リレーショナルデータベースで一般的なAUTO INCREMENTやSERIAL的な自動採番機能）はありません。
* BatchPutAttribute/BatchDeleteAttributeはサポートされていません。
* Integer/Float/Doubleにて、負の値はサポートされていません。
* 比較演算子between、in、everyはサポートされていません。（参考：[SimpleDB Developer Guide: Comparison Operators](http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/UsingSelectOperators.html)）
* データセットパーティショニング（ドメイン分割／シャーディング）機能はサポートされていません。（参考：[SimpleDB Developer Guide: Data Set Partitioning](http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/DataSetPartitioning.html)）
* エラーメッセージなどが国際化（英語化）されていません。


Licence
==============
* ソースコードはApache Licence 2.0とし、すべてをgithub上に公開する事とします。（[https://github.com/dateofrock/simpledb-mapper](https://github.com/dateofrock/simpledb-mapper)）
* 当ソフトウェアの動作は保証しません。ご自身の責任と判断でご利用ください。
* 当ソフトウェアを利用することにより発生したいかなる損害も当方は責任を負わないものとします。

Author
==============

Takehito Tanabe - [dateofrock](http://blog.dateofrock.com/)
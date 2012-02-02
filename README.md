simpledb-mapper
===================
simpledb-mapperは、Amazon SimpleDBのデータマッパーユーティリティです。ドメインをPOJOとして表現し、アノテーションをつけるだけでSimpleDBに永続化が可能です。（いくつか制限があります）
イメージとしてはこのような感じです。

```java
AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);;
AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
sdb.setEndpoint("sdb.ap-northeast-1.amazonaws.com");
AmazonS3 s3 = new AmazonS3Client(cred);

SimpleDBMapper mapper = new SimpleDBMapper(sdb, s3);

Book book1 = new Book();
book1.id = 123L;
book1.title = "面白い本";
book1.authors = new HashSet<String>();
book1.authors.add("著者A");
book1.authors.add("著者B");
book1.price = 1280;
book1.publishedAt = new Date();
book1.isbn = "1234567890";
book1.width = 18.2f;
book1.height = 25.6f;
book1.available = true;

mapper.save(book1);
```

Usage
----
始めに、SimpleDBにドメインを作成します。ドメインを作成するメソッドがSimpleDBMapperにあります。

```java
SimpleDBMapper mapper = new SimpleDBMapper(sdb, s3);
mapper.createDomain(Book.class);
```

SimpleDBに永続化したいモデルをPOJOとして表現し、そこにsimpledb-mapperが用意してあるアノテーションを付け加えます。以下、サンプルとしてBookというPOJOを例に解説します。

```java
@SimpleDBEntity(
		domainName = "SimpleDBMapper-Book-Testing", 
		s3BucketName = "simpledbmapper-book-testing", 
		s3KeyPrefix = "simpledb-blob/")
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

	@SimpleDBBlob(attributeName = "review", contentType = "text/plain")
	public String review;

	@SimpleDBBlob(attributeName = "coverImage", contentType = "image/jpeg", fetch = FetchType.LAZY)
	public byte[] coverImage;

	@SimpleDBVersionAttribute
	public Long version;
}
```


### @SimpleDBEntity
永続化したいPOJOの指定です。

<dl>
<dt>domainName</dt><dd>ドメイン名（必須）</dd>
<dt>s3BucketName</dt><dd>BlobをS3に保存する際のバケット名</dd>
<dt>s3KeyPrefix</dt><dd>BlobをS3に保存する際のキーの名前のはじまり部分</dd>
</dl>

この例で言うと、「SimpleDBMapper-Book-Testing」に永続化されます。（リレーショナルデータベースで言い換えれば、テーブルの指定に相当します。）Blobが指定されているreviewとcoverImageは「simpledbmapper-book-testing」というS3バケット内の「sampled-blob/」以下に保存されます。

###@SimpleDBItemName
@SimpleDBItemNameアノテーションを指定されたフィールドは、SimpleDBのItemNameにひもづけられます。（リレーショナルデータベースで言い換えれば、プライマリキーに相当します。）ItemNameとして指定できる型は以下に制約されます。

* java.lang.String
* java.lang.Integer
* java.lang.Float
* java.lang.Long

###@SimpleDBAttribute
@SimpleDBAttributeアノテーションを指定されたフィールドは、SimpleDBのattributeにひもづけられます。（リレーショナルデータベースで言い換えれば、カラムに相当します。）
attributeとして指定できる型は以下に制約されます。

* java.lang.String
* java.lang.Integer
* java.lang.Float
* java.lang.Long
* java.util.Date
* java.util.Set&lt;java.lang.String&gt;
* java.util.Set&lt;java.lang.Integer&gt;
* java.util.Set&lt;java.lang.Float&gt;
* java.util.Set&lt;java.lang.Long&gt;
* java.util.Set&lt;java.util.Date&gt;


###@SimpleDBBlob
@SimpleDBBlobアノテーションで指定されたフィールドは、SimpleDBの制限である1024byteを超える大きなデータを永続化したい場合に使います。データそのものはS3に保存され、SimpleDBのアトリビュートにはその参照情報（バケット名、キーなど）が記載されます。simpledb-mapperはその参照情報をもとに自動的にPOJOにデータをセットします。

指定できる型は

* java.lang.String
* byte[]

のみです。

また、以下を指定する事が可能です。

<dl>
<dt>contentType</dt><dd>S3に保存する際のContent-Type指定</dd>
<dt>fetch</dt><dd>S3より随時データを取得するかどうか。デフォルトでは常に取得しますが、パフォーマンスは大幅に落ちます。指定には、FetchType.EAGERかFetchType.LAZYを指定します。simpledb-mapperは、遅延ロードのような機能はサポートしていません。単に取得しないだけです。</dd>
</dl>

なお、FetchType.LAZYで指定されたフィールドを上書きしてフェッチ対象にするためにはこのようにします。

```java
mapper.addEagerBlobFetch("coverImage");
```

フェッチ対象から再度外すにはこのようにします。

```java
mapper.removeEagerBlobFetch("coverImage");
//もしくは
mapper.resetEagerBlobFetch();
```



###@SimpleDBVersionAttribute
@SimpleDBVersionAttributeアノテーションで指定されたフィールドは、SimpleDBにアイテムをPUT/DELETEする際にトランザクション制御（楽観的ロック）を実現するためのフィールドになります。このフィールドはsimpledb-mapperが内部的に使用するものです。この指定は必須ではありません。
指定できる型は

* java.lang.Long

のみです。



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
		<version>0.6-SNAPSHOT</version>
	</dependency>
</dependencies>
```

なお、simpledb-mapperはAWS SDK for Javaライブラリを利用しています。

Usage
----
はじめにAWSのSDKで用意されている[AmazonSimpleDB](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpledb/AmazonSimpleDB.html)と[AmazonS3](http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3.html)のインスタンスを作ります。SimpleDBで使いたいリージョンがUS-EAST以外の場合は、相応のEndPoint指定が必要です。それらをsimpledb-mapperのSimpleDBMapperのコンストラクタに渡してください。


```java
AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);;
AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
sdb.setEndpoint("sdb.ap-northeast-1.amazonaws.com"); 
AmazonS3 s3 = new AmazonS3Client(cred);

SimpleDBMapper mapper = new SimpleDBMapper(sdb, s3);
```
 
@SimpleDBEntityアノテーションが指定されたPOJOを用意します。simpledb-mapperではItemNameの自動生成をサポートしていませんので、自分で作る必要があります。

```java
Book book = new Book();
book.id = 123L;
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
book.review = readUTF8String("/book2.review.txt"); //大きなテキストなど
book.coverImage = readBytes("/book2.cover.jpg"); //JPEG画像など
```
SimpleDBMapper#save()でSimpleDBに永続化されます。

```java
mapper.save(book);
```

book1のattributeを変更して、再度save()すると、上書き保存されます。（リレーショナルデータベースで言うところのUPDATEになります。） 

```java
book.authors.remove("恥 晒");
mapper.save(book);
```

SimpleDBに永続化されたPOJOを取得したい場合はやり方が二種類あります。一つはItemNameを指定する方法、もう一つはQueryを投げる方法です。

```java
// ItemNameを指定する方法
Book fetchedBook = mapper.load(Book.class, 123L);

// Queryを指定する方法 
Condition condition = new Condition("title", ComparisonOperator.Equals, "すごい本");
QueryExpression expression = new QueryExpression(condition);
Sort sort = new Sort("title");
expression.setSort(sort);
 
List<Book> books = mapper.query(Book.class, expression);
```

booksが大量にある場合、QueryExpressionにLimitをセットしてください。セットしない場合はSimpleDBのデフォルト値である100がセットされます。また、セットできる最大値はSimpleDBの制限から2500です。

```java
expression.setLimit(1000);
```

queryを投げた際に、さらに残りのアイテムがある場合は、SimpleDBMapper#hasNext()を呼んでください。つまりコードは以下のようになるでしょう。

```java
List<Book> books = mapper.query(Book.class, expression);
while (mapper.hasNext()) {
	books.addAll(Book.class, expression);
}
```


削除する場合は、ItemNameに値が入っているPOJOを引数にdelete()を呼びます。

```java
mapper.delete(book);
```

アイテムのカウントをする事も可能です。条件なしにすべてのアイテムをカウントする方法と条件付きカウントの二種類があります。

```java
// 条件なし
int count = mapper.countAll(Book.class, true);
// 条件あり
count = mapper.count(Book.class, expression);
```


Tips
==============
### S3へのアップロード速度を上げる
Blobフィールドが多くあると、S3へのアクセスに時間がかかるケースがあります。その場合はS3に並列で読み書きするのがベストな方法です。

simpledb-mapperでは、S3に書き込む際のスピードを上げるために、並列度の設定をする事が出来ます。（とはいえ、読み込みは未実装です。ごめんなさい。）その設定は、SimpleDBMapperのインスタンスを作る際に、SimpleDBMapperConfigを渡してあげる事です。

```java
SimpleDBMapperConfig config = new SimpleDBMapperConfig();
config.setS3AccessThreadPoolSize(3); //同時にS3にアクセスするスレッド数が3になる。
SimpleDBMapper mapper = new SimpleDBMapper(sub, s3, config);
```

>スレッド数はデフォルトでは「2」です。この数の最適値は一概には言えませんが、POJOに設定してあるBlobの数と一致させるのが一般的です。ただし、その分メモリの消費量も増えますのでご注意ください。

### Consistent Readオプション
SimpleDBにはデータ読み出しの一貫性を保証するConsistent Readオプションがあります。simpledb-mapperのデフォルトではtrueになっていますが、ここをfalseにする事によって読み出しパフォーマンスを上げる事が可能です。これも、SimpleDBMapperConfigにセットします。

```java
config.setConsistentRead(false);
```
> Consistent Readをfalseにすると、SimpleDBより読み出したデータが古い可能性があります。作りによってはアプリケーション内で矛盾が発生する可能性があるので、十分に注意してください。なお、SimpleDBは1秒程度で一貫性が保たれるという事です。（参考「[SimpleDB, SQS, SNS詳細 - AWSマイスターシリーズ](http://www.slideshare.net/kentamagawa/simpledb-sqs-sns-aws)」）


Limitation
==============
* Integer/Float/Doubleにて、負の値はサポートされていません。(Issue: https://github.com/dateofrock/simpledb-mapper/issues/1)
* エラーメッセージなどが国際化（英語化）されていません。(Issue: https://github.com/dateofrock/simpledb-mapper/issues/2)
* 比較演算子between、in、everyはサポートされていません。(Issue: https://github.com/dateofrock/simpledb-mapper/issues/3)（参考：[SimpleDB Developer Guide: Comparison Operators](http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/UsingSelectOperators.html)）
* RDBのO/Rマッパーのように、One to Many、Many to One、Many to Manyのようなリレーションはサポートしていません。
* @SimpleDBItemNameの自動発行機能（リレーショナルデータベースで一般的なAUTO INCREMENTやSERIAL的な自動採番機能）はありません。
* BatchPutAttribute/BatchDeleteAttributeはサポートされていません。
* データセットパーティショニング（ドメイン分割／シャーディング）機能はサポートされていません。（参考：[SimpleDB Developer Guide: Data Set Partitioning](http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/DataSetPartitioning.html)）


Licence
==============
* ソースコードはApache Licence 2.0とし、すべてをgithub上に公開する事とします。（[https://github.com/dateofrock/simpledb-mapper](https://github.com/dateofrock/simpledb-mapper)）
* 当ソフトウェアの動作は保証しません。ご自身の責任と判断でご利用ください。
* 当ソフトウェアを利用することにより発生したいかなる損害も当方は責任を負わないものとします。



Author
==============

Takehito Tanabe - [dateofrock](http://blog.dateofrock.com/)
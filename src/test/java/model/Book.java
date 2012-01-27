package model;

import java.util.Date;
import java.util.Set;

import com.dateofrock.aws.simpledb.datamodeling.SimpleDBAttribute;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBEntity;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBItemName;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBVersionAttribute;

/**
 * テスト用モデル
 * 
 * @author dateofrock
 */
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

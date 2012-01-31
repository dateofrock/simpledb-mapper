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
package model;

import java.util.Date;
import java.util.Set;

import com.dateofrock.simpledbmapper.SimpleDBAttribute;
import com.dateofrock.simpledbmapper.SimpleDBBlob;
import com.dateofrock.simpledbmapper.SimpleDBEntity;
import com.dateofrock.simpledbmapper.SimpleDBItemName;
import com.dateofrock.simpledbmapper.SimpleDBVersionAttribute;

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

	@SimpleDBBlob(attributeName = "review", s3BucketName = "simpledbmapper-book-testing", prefix = "review/")
	public String review;

	@SimpleDBBlob(attributeName = "coverImage", s3BucketName = "simpledbmapper-book-testing", prefix = "coverImage/")
	public byte[] coverImage;

	@SimpleDBVersionAttribute
	public Long version;

}

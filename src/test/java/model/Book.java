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
import com.dateofrock.simpledbmapper.SimpleDBBlob.FetchType;
import com.dateofrock.simpledbmapper.SimpleDBDomain;
import com.dateofrock.simpledbmapper.SimpleDBItemName;
import com.dateofrock.simpledbmapper.SimpleDBVersionAttribute;

/**
 * テスト用モデル
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
@SimpleDBDomain(domainName = "SimpleDBMapper-Book", s3BucketName = "dateofrock-testing", s3KeyPrefix = "simpledb-mapper/")
public class Book {

	@SimpleDBItemName
	public Long id;

	@SimpleDBAttribute
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

	@SimpleDBBlob(contentType = "text/plain")
	public String review;

	@SimpleDBBlob(attributeName = "coverImage", contentType = "image/jpeg", fetch = FetchType.LAZY)
	public byte[] coverImage;

	@SimpleDBVersionAttribute
	public Long version;

}

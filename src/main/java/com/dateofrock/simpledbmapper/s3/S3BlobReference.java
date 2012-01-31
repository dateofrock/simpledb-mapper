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
package com.dateofrock.simpledbmapper.s3;

/**
 * 
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class S3BlobReference {

	private String attributeName;
	private String s3BucketName;
	private String prefix;
	private Object object;

	public S3BlobReference(String attributeName, String s3BucketName, String prefix, Object object) {
		super();
		this.attributeName = attributeName;
		this.s3BucketName = s3BucketName;
		this.prefix = prefix;
		this.object = object;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	public String getS3BucketName() {
		return this.s3BucketName;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public Object getObject() {
		return this.object;
	}

}

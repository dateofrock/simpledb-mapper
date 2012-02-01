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

/**
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class SimpleDBMapperConfig {

	public static final int S3_UPLOAD_THREAD_POOL_SIZE = 3;

	private int s3UploadThreadPoolSize;
	private boolean consistentRead;

	public static final SimpleDBMapperConfig DEFAULT;

	static {
		DEFAULT = new SimpleDBMapperConfig(S3_UPLOAD_THREAD_POOL_SIZE, true);
	}

	public SimpleDBMapperConfig(int s3UploadThreadPoolSize, boolean consistentRead) {
		super();
		this.s3UploadThreadPoolSize = s3UploadThreadPoolSize;
		this.consistentRead = consistentRead;
	}

	public int getS3UploadThreadPoolSize() {
		return s3UploadThreadPoolSize;
	}

	public boolean isConsistentRead() {
		return consistentRead;
	}

}

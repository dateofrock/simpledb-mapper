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

	public static final SimpleDBMapperConfig DEFAULT;

	public static final int DEFAULT_S3_ACCESS_THREAD_POOL_SIZE = 2;
	public static final boolean DEFAULT_CONSISTENT_READ = true;

	private int s3AccessThreadPoolSize;
	private boolean consistentRead;

	static {
		DEFAULT = new SimpleDBMapperConfig();
	}

	public SimpleDBMapperConfig() {
		super();
		this.s3AccessThreadPoolSize = DEFAULT_S3_ACCESS_THREAD_POOL_SIZE;
		this.consistentRead = DEFAULT_CONSISTENT_READ;
	}

	public int geS3AccessThreadPoolSize() {
		return this.s3AccessThreadPoolSize;
	}

	public void setS3AccessThreadPoolSize(int s3UploadThreadPoolSize) {
		this.s3AccessThreadPoolSize = s3UploadThreadPoolSize;
	}

	public boolean isConsistentRead() {
		return this.consistentRead;
	}

	public void setConsistentRead(boolean consistentRead) {
		this.consistentRead = consistentRead;
	}

}

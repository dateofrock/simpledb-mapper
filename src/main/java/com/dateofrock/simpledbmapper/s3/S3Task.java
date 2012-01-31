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

import java.io.InputStream;
import java.util.concurrent.Callable;

import com.amazonaws.services.s3.AmazonS3;
import com.dateofrock.simpledbmapper.s3.S3TaskResult.Operation;

/**
 * 
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class S3Task implements Callable<S3TaskResult> {

	private String simpleDBAttributeName;
	private AmazonS3 s3;
	private InputStream input;
	private String bucketName, key;

	public S3Task(AmazonS3 s3, String simpleDBAttributeName, InputStream input, String bucketName, String key) {
		this.simpleDBAttributeName = simpleDBAttributeName;
		this.s3 = s3;
		this.input = input;
		this.bucketName = bucketName;
		this.key = key;
	}

	@Override
	public S3TaskResult call() throws Exception {
		S3TaskResult taskResult = new S3TaskResult(Operation.UPLOAD, this.simpleDBAttributeName, this.bucketName,
				this.key);
		try {
			this.s3.putObject(this.bucketName, this.key, this.input, null);
			taskResult.setSuccess(true);
		} catch (Exception e) {
			taskResult.setSuccess(false);
			taskResult.setS3Exception(e);
		}
		return taskResult;
	}

}

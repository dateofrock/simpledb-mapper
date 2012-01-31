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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import com.dateofrock.simpledbmapper.SimpleDBMappingException;

/**
 * 
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class S3TaskResult {

	public enum Operation {
		UPLOAD, DOWNLOAD, DELETE
	};

	private Operation operation;
	private boolean success;
	private String simpleDBAttributeName;
	private String bucketName, key;
	private Exception s3Exception;

	public S3TaskResult(Operation operation, String simpleDBAttributeName, String bucketName, String key) {
		super();
		this.operation = operation;
		this.simpleDBAttributeName = simpleDBAttributeName;
		this.bucketName = bucketName;
		this.key = key;
	}

	public String toSimpleDBAttributeValue() {
		Properties prop = new Properties();
		prop.put("bucketName", this.bucketName);
		prop.put("key", this.key);
		prop.put("success", String.valueOf(this.success));
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try {
			prop.store(byteOut, null);
		} catch (IOException e) {
			throw new SimpleDBMappingException("Propertiesのストアに失敗", e);
		} finally {
			try {
				byteOut.close();
			} catch (IOException ignore) {
			}
		}
		return new String(byteOut.toByteArray());
	}

	public void setSimpleDBAttributeValue(String properties) {
		Properties prop = new Properties();
		StringReader reader = new StringReader(properties);
		try {
			prop.load(reader);
		} catch (IOException e) {
			throw new SimpleDBMappingException("Propertiesのロードに失敗", e);
		} finally {
			reader.close();
		}
		this.bucketName = prop.get("bucketName").toString();
		this.key = prop.get("key").toString();
		this.success = new Boolean(prop.get("success").toString());
	}

	public String getSimpleDBAttributeName() {
		return this.simpleDBAttributeName;
	}

	public String getBucketName() {
		return this.bucketName;
	}

	public String getKey() {
		return this.key;
	}

	public Operation getOperation() {
		return this.operation;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Exception getS3Exception() {
		return this.s3Exception;
	}

	public void setS3Exception(Exception s3Exception) {
		this.s3Exception = s3Exception;
	}
}

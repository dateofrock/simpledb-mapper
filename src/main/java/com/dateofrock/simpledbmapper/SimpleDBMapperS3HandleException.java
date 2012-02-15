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

import java.util.Iterator;
import java.util.List;

import com.dateofrock.simpledbmapper.s3.S3TaskResult;

/**
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class SimpleDBMapperS3HandleException extends RuntimeException {

	private static final long serialVersionUID = -3564564793207724066L;
	private String customMessage;

	public SimpleDBMapperS3HandleException(String message, Exception e) {
		super(message, e);
	}

	public SimpleDBMapperS3HandleException(List<S3TaskResult> taskFailures) {
		StringBuilder message = new StringBuilder();
		for (Iterator<S3TaskResult> iter = taskFailures.iterator(); iter.hasNext();) {
			S3TaskResult result = iter.next();
			if (result.isSuccess()) {
				continue;
			}
			message.append("[").append(result.getOperation()).append(" error]");
			message.append("bucketName: ").append(result.getBucketName()).append(", ");
			message.append("key: ").append(result.getKey());
			if (iter.hasNext()) {
				message.append(" ");
			}
		}
		this.customMessage = message.toString();
	}

	@Override
	public String getMessage() {
		if (this.customMessage == null) {
			return super.getMessage();
		}
		return this.customMessage;
	}
}

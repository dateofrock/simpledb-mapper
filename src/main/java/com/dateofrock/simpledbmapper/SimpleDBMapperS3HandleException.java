package com.dateofrock.simpledbmapper;

import java.util.Iterator;
import java.util.List;

import com.dateofrock.simpledbmapper.s3.S3TaskResult;

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

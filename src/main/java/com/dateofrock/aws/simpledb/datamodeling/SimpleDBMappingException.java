package com.dateofrock.aws.simpledb.datamodeling;

/**
 * simpledb-mapperの一般的な実行時例外です。
 * 
 * @author dateofrock
 */
public class SimpleDBMappingException extends RuntimeException {

	private static final long serialVersionUID = 6240920032845919234L;

	public SimpleDBMappingException(String message) {
		super(message);
	}

	public SimpleDBMappingException(String message, Exception e) {
		super(message, e);
	}

	public SimpleDBMappingException(Exception e) {
		super(e);
	}

}

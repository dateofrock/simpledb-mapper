package com.dateofrock.aws.simpledb.datamodeling;

/**
 * 検索した際にあるべきアイテムが見つからなかったときにスローされる例外です。
 * 
 * @author dateofrock
 */
public class SimpleDBMapperNotFoundException extends Exception {

	private static final long serialVersionUID = -9009958880838731912L;

	public SimpleDBMapperNotFoundException(String message) {
		super(message);
	}

}

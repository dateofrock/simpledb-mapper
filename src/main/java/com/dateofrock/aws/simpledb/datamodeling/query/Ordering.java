package com.dateofrock.aws.simpledb.datamodeling.query;

/**
 * ソート順
 * 
 * @author dateofrock
 */
public enum Ordering {

	ASC("asc"), DESC("desc");

	private String value;

	private Ordering(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}

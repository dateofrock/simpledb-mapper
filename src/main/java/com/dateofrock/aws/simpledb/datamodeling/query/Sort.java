package com.dateofrock.aws.simpledb.datamodeling.query;

import com.dateofrock.aws.simpledb.datamodeling.SimpleDBMappingException;

/**
 * select queryのorder byを表現します。
 * 
 * @author dateofrock
 */
public class Sort {

	private Ordering ordering;
	private String attributeName;

	public Sort(String attributeName) {
		this.ordering = Ordering.ASC;
		this.attributeName = attributeName;
	}

	public Sort(Ordering ordering, String attributeName) {
		this.ordering = ordering;
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	String stringExpression() {
		switch (this.ordering) {
		case ASC:
			return "order by " + this.attributeName;
		case DESC:
			return "order by " + this.attributeName + Ordering.DESC.getValue();
		default:
			throw new SimpleDBMappingException("Sorry! not implemented!");
		}
	}
}

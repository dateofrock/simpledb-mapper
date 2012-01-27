package com.dateofrock.aws.simpledb.datamodeling.query;

/**
 * 比較演算子
 * 
 * @author dateofrock
 * @version $Revision$ $Date$
 */
public enum ComparisonOperator {

	Equals("="), NotEquals("!="), //

	GreaterThan(">"), GreaterThanOrEquals(">="), //

	LessThan("<"), LessThanOrEquals("<="), //

	Like("like"), NotLike("not like"), //

	Between("between"), IN("in"), //

	IsNull("is null"), IsNotNull("is not null"), //

	// EVERY("every")//
	;//

	private String value;

	private ComparisonOperator(String value) {
		this.value = value;
	}

	String getValue() {
		return this.value;
	}
}

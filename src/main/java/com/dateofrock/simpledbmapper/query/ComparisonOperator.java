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
package com.dateofrock.simpledbmapper.query;

/**
 * 比較演算子
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public enum ComparisonOperator {

	Equals("="), NotEquals("!="), //

	GreaterThan(">"), GreaterThanOrEquals(">="), //

	LessThan("<"), LessThanOrEquals("<="), //

	Like("like"), NotLike("not like"), //

	// Between("between"), IN("in"), //

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

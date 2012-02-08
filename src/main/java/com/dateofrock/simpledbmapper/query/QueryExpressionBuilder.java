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

import java.util.List;

import com.dateofrock.simpledbmapper.SimpleDBMapper;

/**
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class QueryExpressionBuilder<T> {

	private Class<T> clazz;
	private QueryExpression expression;
	private SimpleDBMapper mapper;

	public QueryExpressionBuilder(Class<T> clazz, SimpleDBMapper mapper) {
		this.clazz = clazz;
		this.mapper = mapper;
	}

	public QueryExpressionBuilder<T> where(String attributeName, ComparisonOperator comparisonOperator,
			Object attributeValue) {
		Condition condition = new Condition(attributeName, comparisonOperator, attributeValue);
		this.expression = new QueryExpression(condition);
		return this;
	}

	public QueryExpressionBuilder<T> and(String attributeName, ComparisonOperator operator, Object attributeValue) {
		Condition cond = new Condition(attributeName, operator, attributeValue);
		this.expression.addAndCondtion(cond);
		return this;
	}

	public QueryExpressionBuilder<T> or(String attributeName, ComparisonOperator operator, Object attributeValue) {
		Condition cond = new Condition(attributeName, operator, attributeValue);
		this.expression.addOrCondition(cond);
		return this;
	}

	public QueryExpressionBuilder<T> intersection(String attributeName, ComparisonOperator operator,
			Object attributeValue) {
		Condition cond = new Condition(attributeName, operator, attributeValue);
		this.expression.addIntersectionCondition(cond);
		return this;
	}

	public QueryExpressionBuilder<T> orderBy(String attributeName) {
		Sort sort = new Sort(attributeName);
		this.expression.setSort(sort);
		return this;
	}

	public QueryExpressionBuilder<T> orderBy(String attributeName, Ordering ordering) {
		Sort sort = new Sort(ordering, attributeName);
		this.expression.setSort(sort);
		return this;
	}

	public QueryExpressionBuilder<T> limit(int limit) {
		this.expression.setLimit(limit);
		return this;
	}

	public QueryExpressionBuilder<T> eagerBlobFetch(String... blobAttributeNames) {
		for (String blobAttributeName : blobAttributeNames) {
			this.mapper.addEagerBlobFetch(blobAttributeName);
		}
		return this;
	}

	// public QueryExpressionBuilder<T> offset(int offset) {
	// return this;
	// }

	public List<T> fetch() {
		return this.mapper.select(this.clazz, this.expression);
	}

	public int count() {
		return this.mapper.count(this.clazz, this.expression);
	}

}

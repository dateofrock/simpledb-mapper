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
package com.dateofrock.aws.simpledb.datamodeling.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dateofrock.aws.simpledb.datamodeling.SimpleDBEntity;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBMappingException;

/**
 * select queryを発行する際のwhere文を表現するクラスです。
 * 
 * @author dateofrock
 */
public class QueryExpression {

	private Condition defaultCondition;

	private Map<String, Condition> conditions;
	private Sort sort;
	private int limit;

	public QueryExpression(Condition condition) {
		this.defaultCondition = condition;
		this.conditions = new TreeMap<String, Condition>();
	}

	public void addAndCondtion(Condition condition) {
		this.conditions.put("and", condition);
	}

	public void addOrCondition(Condition condition) {
		this.conditions.put("or", condition);
	}

	public void addIntersectionCondition(Condition condition) {
		this.conditions.put("intersection", condition);
	}

	public void setSort(Sort sort) {
		this.sort = sort;
	}

	/**
	 * @param limit
	 *            戻り値の最大数。SimpleDBの制限より設定できる最大値は2500（
	 *            {@link SimpleDBEntity#MAX_QUERY_LIMIT}）になります。
	 */
	public void setLimit(int limit) {
		if (limit < 0) {
			throw new IllegalArgumentException("limitは1以上である必要があります");
		}
		if (limit > SimpleDBEntity.MAX_QUERY_LIMIT) {
			String message = String.format("SimpleDBでサポートされる最大Limit数は2500です。指定されたlimit(=%s)は多すぎます。", limit);
			throw new IllegalArgumentException(message);
		}
		this.limit = limit;
	}

	public int getLimit() {
		return this.limit;
	}

	public String whereExpressionString() {
		List<String> attributeNames = new ArrayList<String>();

		StringBuilder expression = new StringBuilder();
		expression.append(this.defaultCondition.expression()).append(" ");
		attributeNames.add(this.defaultCondition.getAttributeName());

		for (String key : this.conditions.keySet()) {
			expression.append(key).append(" ");
			Condition condition = this.conditions.get(key);
			expression.append(condition.expression());
			attributeNames.add(condition.getAttributeName());
		}

		if (this.sort != null) {
			if (!attributeNames.contains(this.sort.getAttributeName())) {
				throw new SimpleDBMappingException(
						"The sort attribute must be present in at least one of the predicates of the expression. sortする場合、conditionにソートするキーを含める必要があります。これはSimpleDBの仕様です。http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/SortingDataSelect.html");
			}
			expression.append(this.sort.stringExpression());
		}
		return expression.toString();
	}

}

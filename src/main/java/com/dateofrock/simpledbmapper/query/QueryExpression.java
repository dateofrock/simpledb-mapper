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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dateofrock.simpledbmapper.SimpleDBDomain;
import com.dateofrock.simpledbmapper.SimpleDBMappingException;

/**
 * select queryを発行する際のwhere文を表現するクラスです。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class QueryExpression {

	private Condition defaultCondition;

	private List<Map<String, Condition>> conditions;
	private Sort sort;
	private int limit;

	public QueryExpression(Condition condition) {
		this.defaultCondition = condition;
		this.conditions = new ArrayList<Map<String, Condition>>();
	}

	public void addAndCondtion(Condition condition) {
		Map<String, Condition> cond = new HashMap<String, Condition>();
		cond.put("and", condition);
		this.conditions.add(cond);
	}

	public void addOrCondition(Condition condition) {
		Map<String, Condition> cond = new HashMap<String, Condition>();
		cond.put("or", condition);
		this.conditions.add(cond);
	}

	public void addIntersectionCondition(Condition condition) {
		Map<String, Condition> cond = new HashMap<String, Condition>();
		cond.put("intersection", condition);
		this.conditions.add(cond);
	}

	public void setSort(Sort sort) {
		this.sort = sort;
	}

	/**
	 * @param limit
	 *            戻り値の最大数。SimpleDBの制限より設定できる最大値は2500（
	 *            {@link SimpleDBDomain#MAX_QUERY_LIMIT}）になります。
	 */
	public void setLimit(int limit) {
		if (limit < 0) {
			throw new IllegalArgumentException("limitは1以上である必要があります");
		}
		if (limit > SimpleDBDomain.MAX_QUERY_LIMIT) {
			String message = String.format("SimpleDBでサポートされる最大Limit数は2500です。指定されたlimit(=%s)は多すぎます。", limit);
			throw new IllegalArgumentException(message);
		}
		this.limit = limit;
	}

	public int getLimit() {
		return this.limit;
	}

	public String describe() {
		List<String> attributeNames = new ArrayList<String>();

		StringBuilder expression = new StringBuilder();
		expression.append(this.defaultCondition.describe()).append(" ");
		attributeNames.add(this.defaultCondition.getAttributeName());

		for (Map<String, Condition> conditionMap : this.conditions) {
			for (String key : conditionMap.keySet()) {
				expression.append(key).append(" ");
				Condition condition = conditionMap.get(key);
				expression.append(condition.describe());
				expression.append(" ");
				attributeNames.add(condition.getAttributeName());
			}
		}

		if (this.sort != null) {
			if (!attributeNames.contains(this.sort.getAttributeName())) {
				throw new SimpleDBMappingException(
						"The sort attribute must be present in at least one of the predicates of the expression. sortする場合、conditionにソートするキーを含める必要があります。これはSimpleDBの仕様です。http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/SortingDataSelect.html");
			}
			expression.append(this.sort.describe());
		}
		return expression.toString();
	}

}

package com.dateofrock.aws.simpledb.datamodeling.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

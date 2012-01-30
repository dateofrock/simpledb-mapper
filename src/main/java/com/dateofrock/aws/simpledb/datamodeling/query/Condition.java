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

import java.util.Date;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBEntity;
import com.dateofrock.aws.simpledb.datamodeling.SimpleDBMappingException;

/**
 * 条件文
 * 
 * @author dateofrock
 */
public class Condition {

	private ComparisonOperator comparisonOperator;
	private String attributeName;
	private Object attributeValue;

	public Condition(String attributeName, ComparisonOperator comparisonOperator, Object attributeValue) {
		this.comparisonOperator = comparisonOperator;
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	String expression() {
		StringBuilder expression = new StringBuilder(this.attributeName);
		expression.append(" ").append(this.comparisonOperator.getValue()).append(" ");
		if (this.attributeValue == null) {
			switch (this.comparisonOperator) {
			case IsNull:
				return expression.toString();
			case IsNotNull:
				return expression.toString();
			default:
				throw new IllegalArgumentException(
						"attributeValueがnullの場合、comparisonOperatorがIsNullもしくはIsNotNullである必要があります。");
			}
		}
		if (this.attributeValue instanceof String) {
			expression.append(SimpleDBUtils.quoteValue((String) this.attributeValue));
		} else if (this.attributeValue instanceof Date) {
			expression.append(SimpleDBUtils.encodeDate((Date) this.attributeValue));
		} else if (this.attributeValue instanceof Integer) {
			expression.append(SimpleDBUtils.encodeZeroPadding((Integer) this.attributeValue,
					SimpleDBEntity.MAX_NUMBER_DIGITS));
		} else if (this.attributeValue instanceof Float) {
			expression.append(SimpleDBUtils.encodeZeroPadding((Float) this.attributeValue,
					SimpleDBEntity.MAX_NUMBER_DIGITS));
		} else if (this.attributeValue instanceof Long) {
			expression.append(SimpleDBUtils.encodeZeroPadding((Long) this.attributeValue,
					SimpleDBEntity.MAX_NUMBER_DIGITS));
		} else {
			throw new SimpleDBMappingException("attributeValueの型が非サポートです。" + this.attributeValue);
		}
		return expression.toString();
	}
}

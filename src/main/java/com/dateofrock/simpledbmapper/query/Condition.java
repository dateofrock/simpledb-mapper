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

import static com.amazonaws.services.simpledb.util.SimpleDBUtils.*;
import static com.dateofrock.simpledbmapper.SimpleDBAttribute.*;

import java.util.Date;

import com.dateofrock.simpledbmapper.SimpleDBMapperException;

/**
 * 条件文
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
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

	public String describe() {
		StringBuilder expression = new StringBuilder();
		if (this.attributeName.equalsIgnoreCase("itemName()")) {
			expression.append(this.attributeName);
		} else {
			expression.append(quoteName(this.attributeName));
		}

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

		expression.append("'");
		if (this.attributeValue instanceof String) {
			expression.append((String) this.attributeValue);
		} else if (this.attributeValue instanceof Date) {
			expression.append(encodeDate((Date) this.attributeValue));
		} else if (this.attributeValue instanceof Integer) {
			expression.append(encodeZeroPadding((Integer) this.attributeValue, DEFAULT_ZERO_PADDING_LENGTH));
		} else if (this.attributeValue instanceof Float) {
			expression.append(encodeZeroPadding((Float) this.attributeValue, DEFAULT_ZERO_PADDING_LENGTH));
		} else if (this.attributeValue instanceof Long) {
			expression.append(encodeZeroPadding((Long) this.attributeValue, DEFAULT_ZERO_PADDING_LENGTH));
		} else if (this.attributeValue instanceof Boolean) {
			expression.append(String.valueOf(this.attributeValue));
		} else {
			throw new SimpleDBMapperException("attributeValueの型が非サポートです。" + this.attributeValue);
		}
		expression.append("'");

		return expression.toString();
	}
}

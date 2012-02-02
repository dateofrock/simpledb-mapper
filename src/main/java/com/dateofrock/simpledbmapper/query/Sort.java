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

import com.amazonaws.services.simpledb.util.SimpleDBUtils;

/**
 * select queryのorder byを表現します。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
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
		StringBuilder expression = new StringBuilder("order by ");
		if (this.attributeName.equalsIgnoreCase("itemName()")) {
			expression.append(this.attributeName);
		} else {
			expression.append(SimpleDBUtils.quoteName(this.attributeName));
		}
		if (this.ordering == Ordering.DESC) {
			expression.append(" ").append(Ordering.DESC.getValue());
		}
		return expression.toString();
	}
}

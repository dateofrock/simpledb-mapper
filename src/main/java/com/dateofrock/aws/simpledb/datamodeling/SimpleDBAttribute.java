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
package com.dateofrock.aws.simpledb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * attributeを指定するためのアノテーションです。フィールドにのみ指定が可能です。
 * 
 * 指定できるクラスは以下に制限されます。
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Long}</li>
 * <li>{@link java.util.Date}</li>
 * <li>{@link java.util.Set}</li>
 * </ul>
 * {@link java.util.Set}で指定できる要素は以下に制限されます。
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Long}</li>
 * <li>{@link java.util.Date}</li>
 * </ul>
 * 
 * 
 * @author dateofrock
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SimpleDBAttribute {

	String attributeName();

}

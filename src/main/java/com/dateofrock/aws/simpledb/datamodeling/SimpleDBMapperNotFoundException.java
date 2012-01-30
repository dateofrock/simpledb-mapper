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

/**
 * 検索した際にあるべきアイテムが見つからなかったときにスローされる例外です。
 * 
 * @author dateofrock
 */
public class SimpleDBMapperNotFoundException extends Exception {

	private static final long serialVersionUID = -9009958880838731912L;

	public SimpleDBMapperNotFoundException(String message) {
		super(message);
	}

}

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
package com.dateofrock.simpledbmapper;

/**
 * simpledb-mapperの一般的な実行時例外です。
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class SimpleDBMapperException extends RuntimeException {

	private static final long serialVersionUID = 6240920032845919234L;

	public SimpleDBMapperException(String message) {
		super(message);
	}

	public SimpleDBMapperException(String message, Exception e) {
		super(message, e);
	}

	public SimpleDBMapperException(Exception e) {
		super(e);
	}

}

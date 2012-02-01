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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Blobのためのアノテーション（SimpleDBでは1024byte以上が扱えないので、それを超えるデータを保存したい場合）
 * 
 * 指定できるクラスは以下です。
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>byte[]</li>
 * </ul>
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SimpleDBBlob {

	String attributeName();

	/**
	 * データを保存したいバケット名
	 */
	String s3BucketName();

	/**
	 * S3キーのプレフィックス
	 */
	String prefix() default "";

	String contentType() default "";
}

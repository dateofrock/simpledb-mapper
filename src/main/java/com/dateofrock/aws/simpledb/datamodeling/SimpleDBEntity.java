package com.dateofrock.aws.simpledb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SimpleDBのドメインとひも付けるためのアノテーションです。
 * 
 * @author dateofrock
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SimpleDBEntity {

	public static final int MAX_NUMBER_DIGITS = 10;

	/**
	 * Maximum items in Select response
	 * 
	 * <a href=
	 * "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/SDBLimits.html"
	 * >AWSドキュメント参照</a>
	 */
	public static final int MAX_QUERY_LIMIT = 2500;

	String domainName();
}

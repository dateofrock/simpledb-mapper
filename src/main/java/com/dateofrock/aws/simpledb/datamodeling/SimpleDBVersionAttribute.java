package com.dateofrock.aws.simpledb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * PUT/DELETE時にトランザクション制御をするためのバージョン属性を指定します。
 * 
 * 指定できるクラスは{@link java.lang.Long}のみです。
 * 
 * @author dateofrock
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SimpleDBVersionAttribute {
	String attributeName() default "version";
}

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

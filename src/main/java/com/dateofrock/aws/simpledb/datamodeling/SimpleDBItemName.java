package com.dateofrock.aws.simpledb.datamodeling;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * itemNameを指定するためのアノテーションです。
 * 
 * 指定できるクラスは以下に制限されます。
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Long}</li>
 * </ul>
 * 
 * @author dateofrock
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleDBItemName {

}

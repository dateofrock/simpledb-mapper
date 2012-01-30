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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;
import com.dateofrock.aws.simpledb.datamodeling.query.QueryExpression;

/**
 * SimpleDBのデータマッパーです。
 * 
 * <pre>
 * &#064;SimpleDBEntity(domainName = &quot;SimpleDBMapper-Book-Testing&quot;)
 * public class Book {
 * 
 * 	&#064;SimpleDBItemName
 * 	public Long id;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;title&quot;)
 * 	public String title;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;isbn&quot;)
 * 	public String isbn;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;authors&quot;)
 * 	public Set&lt;String&gt; authors;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;publishedAt&quot;)
 * 	public Date publishedAt;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;price&quot;)
 * 	public Integer price;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;height&quot;)
 * 	public Float height;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;width&quot;)
 * 	public Float width;
 * 
 * 	&#064;SimpleDBAttribute(attributeName = &quot;available&quot;)
 * 	public boolean available;
 * 
 * 	&#064;SimpleDBVersionAttribute
 * 	public Long version;
 * 
 * }
 * </pre>
 * 
 * <pre>
 * AWSCredentials cred = new PropertiesCredentials(
 * 		SimpleDBMapperTest.class.getResourceAsStream(&quot;/AwsCredentials.properties&quot;));
 * AmazonSimpleDB sdb = new AmazonSimpleDBClient(cred);
 * sdb.setEndpoint(&quot;sdb.ap-northeast-1.amazonaws.com&quot;);
 * 
 * SimpleDBMapper mapper = new SimpleDBMapper(sdb);
 * 
 * Book book1 = new Book();
 * book1.id = 123L;
 * book1.title = &quot;面白い本&quot;;
 * book1.authors = new HashSet&lt;String&gt;();
 * book1.authors.add(&quot;著者A&quot;);
 * book1.authors.add(&quot;著者B&quot;);
 * book1.price = 1280;
 * book1.publishedAt = toDate(&quot;2012-1-20 00:00:00&quot;);
 * book1.isbn = &quot;1234567890&quot;;
 * book1.width = 18.2f;
 * book1.height = 25.6f;
 * book1.available = true;
 * 
 * mapper.save(book1);
 * 
 * book1.authors.remove(&quot;著者A&quot;);
 * mapper.save(book1);
 * Book fetchedBook = mapper.load(Book.class, book1.id, true);
 * 
 * SimpleDBQueryExpression expression = new SimpleDBQueryExpression(new Condition(&quot;title&quot;, ComparisonOperator.Equals,
 * 		&quot;すごい本&quot;));
 * int count = mapper.count(Book.class, expression, true);
 * 
 * expression = new SimpleDBQueryExpression(new Condition(&quot;title&quot;, ComparisonOperator.Like, &quot;%本&quot;));
 * Sort sort = new Sort(&quot;title&quot;);
 * expression.setSort(sort);
 * 
 * List&lt;Book&gt; books = this.mapper.query(Book.class, expression, true);
 * 
 * mapper.delete(book1);
 * count = mapper.countAll(Book.class, true);
 * </pre>
 * 
 * @author dateofrock
 */
public class SimpleDBMapper {

	private AmazonSimpleDB sdb;
	private Refrector refrector;
	private String selectNextToken;

	public SimpleDBMapper(AmazonSimpleDB sdb) {
		this.sdb = sdb;
		this.refrector = new Refrector();
	}

	/**
	 * オブジェクトをSimpleDBに保存します。
	 * 
	 * @param object
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO。
	 *            {@link SimpleDBVersionAttribute}
	 *            がついたフィールドがある場合、トランザクション機能が働きます。（<a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConditionalPut.html"
	 *            >Conditional Put</a>になります。）
	 */
	public <T> void save(T object) {
		Class<?> clazz = object.getClass();
		String domainName = this.refrector.findDomainName(clazz);

		Field itemNameField = this.refrector.findItemNameField(clazz);
		if (itemNameField == null) {
			throw new SimpleDBMappingException(object + "@SimpleDBItemNameアノテーションがありません");
		}

		String itemName = null;
		itemName = this.refrector.getItemNameAsSimpleDBFormat(object, itemNameField);

		Field[] fields = clazz.getFields();
		Map<String, Object> valuesMap = new HashMap<String, Object>();
		for (Field field : fields) {
			SimpleDBAttribute attr = field.getAnnotation(SimpleDBAttribute.class);
			if (attr != null) {
				try {
					valuesMap.put(attr.attributeName(), field.get(object));
				} catch (Exception e) {
					throw new SimpleDBMappingException(e);
				}
			}
		}

		List<String> nullKeys = new ArrayList<String>();
		List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>();
		for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				nullKeys.add(entry.getKey());// 削除対象キーリストに追加
			} else if (value instanceof Set) { // Set
				Set<?> c = (Set<?>) value;
				for (Object val : c) {
					if (val instanceof Integer) {
						attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding(
								(Integer) val, SimpleDBEntity.MAX_NUMBER_DIGITS), true));
					} else if (val instanceof Float) {
						attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding((Float) val,
								SimpleDBEntity.MAX_NUMBER_DIGITS), true));
					} else if (val instanceof Long) {
						attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding((Long) val,
								SimpleDBEntity.MAX_NUMBER_DIGITS), true));
					} else if (val instanceof String) {
						attrs.add(new ReplaceableAttribute(entry.getKey(), (String) val, true));
					}
				}
			} else if (value instanceof Date) {// Date
				Date d = (Date) value;
				attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeDate(d), true));
			} else if (value instanceof Integer) {// Integer or int
				attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding((Integer) value,
						SimpleDBEntity.MAX_NUMBER_DIGITS).toString(), true));
			} else if (value instanceof Float) {// Float or float
				attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding((Float) value,
						SimpleDBEntity.MAX_NUMBER_DIGITS).toString(), true));
			} else if (value instanceof Long) {// Long or long
				attrs.add(new ReplaceableAttribute(entry.getKey(), SimpleDBUtils.encodeZeroPadding((Long) value,
						SimpleDBEntity.MAX_NUMBER_DIGITS).toString(), true));
			} else if (value instanceof String) {// String
				attrs.add(new ReplaceableAttribute(entry.getKey(), (String) value, true));
			} else if (value instanceof Boolean) {// Boolean or boolean
				Boolean b = (Boolean) value;
				attrs.add(new ReplaceableAttribute(entry.getKey(), String.valueOf(b), true));
			} else {
				throw new SimpleDBMappingException("フィールド: " + entry.getKey() + " はサポート対象外の型です。"
						+ entry.getKey().getClass());
			}
		}

		// PutAttribute
		PutAttributesRequest req = new PutAttributesRequest();
		req.setDomainName(domainName);
		req.setItemName(itemName);

		// Versionがあるobjectの場合はConditional PUTする
		Long nowVersion = System.currentTimeMillis();
		Field versionField = this.refrector.findVersionAttributeField(clazz);
		if (versionField != null) {
			try {
				Object versionObject = versionField.get(object);
				String versionAttributeName = this.refrector.findVersionAttributeName(clazz);
				if (versionObject != null) {
					if (versionObject instanceof Long) {
						Long currentVersion = (Long) versionObject;
						UpdateCondition expected = new UpdateCondition();
						expected.setName(versionAttributeName);
						expected.setValue(currentVersion.toString());
						req.setExpected(expected);
					} else {
						throw new SimpleDBMappingException("version属性はLongである必要があります。" + versionField);
					}
				}

				attrs.add(new ReplaceableAttribute(versionAttributeName, nowVersion.toString(), true));
			} catch (Exception e) {
				throw new SimpleDBMappingException("objectからversion取得に失敗: " + object, e);
			}
		}

		req.setAttributes(attrs);
		this.sdb.putAttributes(req);

		// versionをセット
		if (versionField != null) {
			try {
				versionField.set(object, nowVersion);
			} catch (Exception ignore) {
				throw new SimpleDBMappingException("versionの値セットに失敗", ignore);
			}
		}

		// DeleteAttribute
		if (!nullKeys.isEmpty()) {
			DeleteAttributesRequest delReq = new DeleteAttributesRequest();
			delReq.setDomainName(domainName);
			delReq.setItemName(itemName);
			Collection<Attribute> delAttrs = new ArrayList<Attribute>(nullKeys.size());
			for (String nullKey : nullKeys) {
				delAttrs.add(new Attribute(nullKey, null));
			}
			delReq.setAttributes(delAttrs);
			this.sdb.deleteAttributes(delReq);
		}

	}

	/**
	 * オブジェクトをSimpleDBから削除します
	 * 
	 * @param object
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO
	 *            {@link SimpleDBVersionAttribute}
	 *            がついたフィールドがある場合、トランザクション機能が働きます。（<a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConditionalDelete.html"
	 *            >Conditional Delete</a>になります。）
	 */
	public void delete(Object object) {
		String domainName = this.refrector.findDomainName(object.getClass());
		Field itemNameField = this.refrector.findItemNameField(object.getClass());
		String itemName = this.refrector.getItemNameAsSimpleDBFormat(object, itemNameField);

		DeleteAttributesRequest req = new DeleteAttributesRequest(domainName, itemName);

		// versionが入っていたらConditional Delete
		Field versionField = this.refrector.findVersionAttributeField(object.getClass());
		if (versionField != null) {
			try {
				Object versionObject = versionField.get(object);
				String versionAttributeName = this.refrector.findVersionAttributeName(object.getClass());
				if (versionObject != null) {
					if (versionObject instanceof Long) {
						Long currentVersion = (Long) versionObject;
						UpdateCondition expected = new UpdateCondition();
						expected.setName(versionAttributeName);
						expected.setValue(currentVersion.toString());
						req.setExpected(expected);
					} else {
						throw new SimpleDBMappingException("version属性はLongである必要があります。" + versionField);
					}
				}
			} catch (Exception e) {
				throw new SimpleDBMappingException("objectからversion取得に失敗: " + object, e);
			}
		}

		this.sdb.deleteAttributes(req);
	}

	/**
	 * {@link SimpleDBEntity}で指定されたドメイン内のアイテムをすべてカウントします。
	 * 
	 * @param clazz
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 */
	public <T> int countAll(Class<T> clazz, boolean consistentRead) {
		return count(clazz, null, consistentRead);
	}

	/**
	 * {@link SimpleDBEntity}で指定されたドメイン内のアイテムを条件カウントします。
	 * 
	 * @param clazz
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO
	 * @param expression
	 *            where文
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 */
	public <T> int count(Class<T> clazz, QueryExpression expression, boolean consistentRead) {
		String whereExpression = null;
		if (expression != null) {
			whereExpression = expression.whereExpressionString();
		}
		String query = createQuery(clazz, true, whereExpression, 0);
		SelectResult result = this.sdb.select(new SelectRequest(query, consistentRead));
		String countValue = result.getItems().get(0).getAttributes().get(0).getValue();
		return Integer.parseInt(countValue);
	}

	/**
	 * selectを実行します。
	 * 
	 * <a href=
	 * "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/UsingSelect.html"
	 * >AWSドキュメント参照</a>
	 * 
	 * @param clazz
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO
	 * @param expression
	 *            where文
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 * @return 0件の場合は空のListが返ってきます。
	 */
	public <T> List<T> query(Class<T> clazz, QueryExpression expression, boolean consistentRead) {
		String whereExpression = expression.whereExpressionString();
		String query = createQuery(clazz, false, whereExpression, expression.getLimit());

		List<T> objects = fetch(clazz, query, consistentRead);
		return objects;
	}

	/**
	 * @param clazz
	 *            {@link SimpleDBEntity}アノテーションがついたPOJO
	 * @param itemName
	 *            SimpleDBのitemNameで、{@link SimpleDBItemName}で指定した型のオブジェクト
	 * @param consistentRead
	 *            一貫性読み込みオプション。 <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 * @throws SimpleDBMapperNotFoundException
	 *             見つからなかった場合にスローされます
	 */
	public <T> T load(Class<T> clazz, Object itemName, boolean consistentRead) throws SimpleDBMapperNotFoundException {
		String itemNameInQuery = this.refrector.formattedString(itemName);

		String whereExpression = "itemName()=" + SimpleDBUtils.quoteValue(itemNameInQuery);
		String query = createQuery(clazz, false, whereExpression, 0);

		List<T> objects = fetch(clazz, query, consistentRead);
		if (objects.isEmpty()) {
			throw new SimpleDBMapperNotFoundException("見つかりません。" + query);
		}
		// FIXME
		return objects.get(0);
	}

	public boolean hasNext() {
		if (this.selectNextToken != null) {
			return true;
		}
		return false;
	}

	private <T> List<T> fetch(Class<T> clazz, String query, boolean consistentRead) {
		SelectRequest selectRequest = new SelectRequest(query.toString(), consistentRead);
		// TODO nextTokenの保持の仕方
		if (this.selectNextToken != null) {
			selectRequest.setNextToken(this.selectNextToken);
			this.selectNextToken = null;
		}
		SelectResult result = this.sdb.select(selectRequest);
		List<Item> items = result.getItems();
		if (items.isEmpty()) {
			return Collections.emptyList();
		}
		this.selectNextToken = result.getNextToken();

		List<T> objects = new ArrayList<T>();

		Field itemNameField = this.refrector.findItemNameField(clazz);
		// SDBのitemでループ
		for (Item item : items) {
			T instance;
			try {
				instance = clazz.newInstance();
			} catch (Exception e) {
				throw new SimpleDBMappingException(e);
			}

			// ItemNameのセット
			Class<?> type = itemNameField.getType();
			try {
				String itemName = item.getName();
				if (this.refrector.isIntegerType(type)) {
					itemNameField.set(instance, SimpleDBUtils.decodeZeroPaddingInt(itemName));
				} else if (this.refrector.isFloatType(type)) {
					itemNameField.set(instance, SimpleDBUtils.decodeZeroPaddingFloat(itemName));
				} else if (this.refrector.isLongType(type)) {
					itemNameField.set(instance, SimpleDBUtils.decodeZeroPaddingLong(itemName));
				} else if (this.refrector.isStringType(type)) {
					itemNameField.set(instance, itemName);
				} else {
					// FIXME
					throw new SimpleDBMappingException("itemNameはStringかIntegerかLong、Floatのどれかである必要があります。");
				}
			} catch (Exception e) {
				throw new SimpleDBMappingException(e);
			}

			// itemのattributesでループ
			List<Attribute> attrs = item.getAttributes();
			for (Attribute attr : attrs) {
				this.refrector.setFieldValueByAttribute(clazz, instance, attr);
			}

			objects.add(instance);
		}

		return objects;
	}

	private <T> String createQuery(Class<T> clazz, boolean isCount, String whereExpression, int limit) {
		String domainName = this.refrector.findDomainName(clazz);
		StringBuilder query = new StringBuilder("select ");
		if (isCount) {
			query.append("count(*)");
		} else {
			query.append("*");
		}
		query.append(" from ");
		query.append(SimpleDBUtils.quoteName(domainName));
		if (whereExpression != null) {
			query.append(" where ");
			query.append(whereExpression);
		}
		if (limit > 0) {
			query.append(" limit ").append(limit);
		}
		return query.toString();
	}

}

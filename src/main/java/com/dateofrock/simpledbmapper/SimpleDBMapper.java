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

import static com.amazonaws.services.simpledb.util.SimpleDBUtils.*;
import static com.dateofrock.simpledbmapper.SimpleDBDomain.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.NoSuchDomainException;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.dateofrock.simpledbmapper.SimpleDBBlob.FetchType;
import com.dateofrock.simpledbmapper.query.QueryExpression;
import com.dateofrock.simpledbmapper.query.QueryExpressionBuilder;
import com.dateofrock.simpledbmapper.s3.S3BlobReference;
import com.dateofrock.simpledbmapper.s3.S3Task;
import com.dateofrock.simpledbmapper.s3.S3TaskResult;
import com.dateofrock.simpledbmapper.s3.S3TaskResult.Operation;

/**
 * SimpleDBのデータマッパー
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class SimpleDBMapper {

	private static final Log log = LogFactory.getLog(SimpleDBMapper.class);

	private AmazonSimpleDB sdb;
	private AmazonS3 s3;
	private SimpleDBMapperConfig config;

	private Reflector reflector;
	private String selectNextToken;

	private List<String> blobEagerFetchList = new ArrayList<String>();

	public SimpleDBMapper(AmazonSimpleDB sdb, AmazonS3 s3) {
		this.sdb = sdb;
		this.s3 = s3;
		this.config = SimpleDBMapperConfig.DEFAULT;
		this.reflector = new Reflector();
	}

	public SimpleDBMapper(AmazonSimpleDB sdb, AmazonS3 s3, SimpleDBMapperConfig config) {
		this.sdb = sdb;
		this.s3 = s3;
		this.config = config;
		this.reflector = new Reflector();
	}

	public void addEagerBlobFetch(String fieldName) {
		this.blobEagerFetchList.add(fieldName);
	}

	public void removeEagerBlobFetch(String fieldName) {
		this.blobEagerFetchList.remove(fieldName);
	}

	public void resetEagerBlobFetch() {
		this.blobEagerFetchList = new ArrayList<String>();
	}

	/**
	 * 対象ドメインにアイテムが一件もない場合に限ってドメインを削除します
	 * 
	 * @throws SimpleDBMapperNotEmptyException
	 */
	public void dropDomainIfEmpty(Class<?> entityClass) throws SimpleDBMapperNotEmptyException {
		String domainName = getDomainName(entityClass);
		int count = 0;
		try {
			count = this.countAll(entityClass);
		} catch (NoSuchDomainException ignore) {
			return;
		}

		if (count > 0) {
			throw new SimpleDBMapperNotEmptyException(String.format("ドメイン %s には、すでに %s 件のアイテムが登録されているので削除できません",
					domainName, count));
		}
		this.sdb.deleteDomain(new DeleteDomainRequest(domainName));
	}

	/**
	 * SimpleDBのドメイン名を取得します
	 * 
	 * @param entityClass
	 */
	public String getDomainName(Class<?> entityClass) {
		return this.reflector.getDomainName(entityClass);
	}

	/**
	 * SimpleDBのドメインを強制削除します
	 * 
	 * @see AmazonSimpleDB#deleteDomain(DeleteDomainRequest)
	 */
	public void forceDropDomain(Class<?> entityClass) {
		String domainName = getDomainName(entityClass);
		this.sdb.deleteDomain(new DeleteDomainRequest(domainName));
	}

	/**
	 * SimpleDBにドメインを作成します
	 * 
	 * @see AmazonSimpleDB#createDomain(CreateDomainRequest)
	 */
	public void createDomain(Class<?> entityClass) {
		String domainName = getDomainName(entityClass);
		this.sdb.createDomain(new CreateDomainRequest(domainName));

		// ドメインが作成された直後はまだ使えないので、使えるようになるまでしばらく待つ。
		int repeat = 3;
		int wait = 300;
		for (int i = 0; i < repeat; i++) {
			if (isDomainExists(entityClass)) {
				log.debug("domain " + domainName + " has created.");
				return;
			} else {
				try {
					Thread.sleep(wait);
				} catch (InterruptedException ignore) {
					// noop
				}
			}
		}
		log.warn("domain " + domainName + " is not avalilable.");
	}

	public boolean isDomainExists(Class<?> entityClass) {
		String domainName = getDomainName(entityClass);
		try {
			this.sdb.domainMetadata(new DomainMetadataRequest(domainName));
		} catch (NoSuchDomainException e) {
			return false;
		}
		return true;
	}

	/**
	 * オブジェクトをSimpleDBに保存します。
	 * 
	 * @param object
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO。
	 *            {@link SimpleDBVersionAttribute}
	 *            がついたフィールドがある場合、トランザクション機能が働きます。（<a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConditionalPut.html"
	 *            >Conditional Put</a>になります。）
	 */
	public <T> void save(T object) {
		Class<?> clazz = object.getClass();
		String domainName = getDomainName(clazz);

		Field itemNameField = this.reflector.findItemNameField(clazz);
		if (itemNameField == null) {
			throw new SimpleDBMapperException(object + "@SimpleDBItemNameアノテーションがありません");
		}

		String itemName = null;
		itemName = this.reflector.encodeItemNameAsSimpleDBFormat(object, itemNameField);

		Set<Field> allFields = this.reflector.listAllFields(clazz);
		Map<String, Object> attributeMap = new HashMap<String, Object>();
		List<S3BlobReference> blobList = new ArrayList<S3BlobReference>();
		for (Field field : allFields) {
			try {
				String attributeName = this.reflector.getAttributeName(field);
				if (attributeName != null) {
					if (this.reflector.isAttributeField(field)) {
						attributeMap.put(attributeName, field.get(object));
					} else if (this.reflector.isBlobField(field)) {
						String s3BucketName = this.reflector.getS3BucketName(clazz);
						String s3KeyPrefix = this.reflector.getS3KeyPrefix(clazz);
						String s3ContentType = this.reflector.getS3ContentType(field);
						// FIXME
						S3BlobReference s3BlobRef = new S3BlobReference(attributeName, s3BucketName, s3KeyPrefix,
								s3ContentType, field.get(object));
						blobList.add(s3BlobRef);
					}
				}
			} catch (Exception e) {
				throw new SimpleDBMapperException(e);
			}
		}

		List<String> nullKeys = new ArrayList<String>();
		List<ReplaceableAttribute> replacableAttrs = new ArrayList<ReplaceableAttribute>();

		// SimpleDBAttribute
		for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
			String sdbAttributeName = entry.getKey();
			Object sdbValue = entry.getValue();
			if (sdbValue == null) {
				nullKeys.add(sdbAttributeName);// 削除対象キーリストに追加
			} else if (sdbValue instanceof Set) { // Set
				Set<?> c = (Set<?>) sdbValue;
				for (Object val : c) {
					replacableAttrs.add(new ReplaceableAttribute(sdbAttributeName, this.reflector
							.encodeObjectAsSimpleDBFormat(val), true));
				}
			} else {
				replacableAttrs.add(new ReplaceableAttribute(sdbAttributeName, this.reflector
						.encodeObjectAsSimpleDBFormat(sdbValue), true));
			}
		}

		// SimpleDBBlob
		// UploadするBlobをリストアップする
		List<S3Task> uploadTasks = new ArrayList<S3Task>();
		for (S3BlobReference s3BlobRef : blobList) {
			String bucketName = s3BlobRef.getS3BucketName();
			if (bucketName == null) {
				throw new SimpleDBMapperException("Blobにはs3BucketNameの指定が必須です");
			}

			StringBuilder s3Key = new StringBuilder();
			String prefix = s3BlobRef.getPrefix();
			if (prefix == null) {
				throw new SimpleDBMapperException("Blobのprefixにnullは指定できません");
			}
			prefix = prefix.trim();
			s3Key.append(prefix);
			if (!prefix.isEmpty() && !prefix.endsWith("/")) {
				s3Key.append("/");
			}
			s3Key.append(itemName).append("/").append(s3BlobRef.getAttributeName());

			Object blobObject = s3BlobRef.getObject();
			if (blobObject == null) {
				nullKeys.add(s3BlobRef.getAttributeName());
				// その都度Delete Objectする
				// FIXME このタイミングがベストではない。ベストはSDBに対してDeleteAttributeする直後。
				this.s3.deleteObject(bucketName, s3Key.toString());
			} else {
				// アップロード対象のBlobがすでにS3に保管されているものと同じであれば、再アップロードしないようにしたい。
				InputStream input = null;
				if (blobObject instanceof String) {
					// BlobがString
					// FIXME encoding決めうち
					input = new ByteArrayInputStream(((String) blobObject).getBytes(Charset.forName("UTF-8")));
				} else if (blobObject.getClass().getSimpleName().equals("byte[]")) {
					// BlobがByte配列
					input = new ByteArrayInputStream((byte[]) blobObject);
				} else {
					throw new SimpleDBMapperException("Blobに指定できるクラスはStringもしくはbyte[]のみです");
				}
				S3Task uploadTask = new S3Task(this.s3, s3BlobRef.getAttributeName(), input, bucketName,
						s3Key.toString(), s3BlobRef.getContentType());
				uploadTasks.add(uploadTask);
			}
		}

		// PutAttribute
		PutAttributesRequest req = new PutAttributesRequest();
		req.setDomainName(domainName);
		req.setItemName(itemName);

		// Versionがあるobjectの場合はConditional PUTする
		Long nowVersion = System.currentTimeMillis();
		Field versionField = this.reflector.findVersionAttributeField(clazz);
		if (versionField != null) {
			try {
				Object versionObject = versionField.get(object);
				String versionAttributeName = versionField.getAnnotation(SimpleDBVersionAttribute.class)
						.attributeName();
				if (versionObject != null) {
					if (versionObject instanceof Long) {
						Long currentVersion = (Long) versionObject;
						UpdateCondition expected = new UpdateCondition();
						expected.setName(versionAttributeName);
						expected.setValue(currentVersion.toString());
						req.setExpected(expected);
					} else {
						throw new SimpleDBMapperException("version属性はLongである必要があります。" + versionField);
					}
				}

				replacableAttrs.add(new ReplaceableAttribute(versionAttributeName, nowVersion.toString(), true));
			} catch (Exception e) {
				throw new SimpleDBMapperException("objectからversion取得に失敗: " + object, e);
			}
		}

		// S3にアップロード処理
		List<S3TaskResult> taskFailures = new ArrayList<S3TaskResult>();
		ExecutorService executor = Executors.newFixedThreadPool(this.config.geS3AccessThreadPoolSize());
		try {
			List<Future<S3TaskResult>> futures = executor.invokeAll(uploadTasks);
			for (Future<S3TaskResult> future : futures) {
				S3TaskResult result = future.get();
				// SimpleDBに結果を書き込み
				replacableAttrs.add(new ReplaceableAttribute(result.getSimpleDBAttributeName(), result
						.toSimpleDBAttributeValue(), true));
				if (!result.isSuccess()) {
					// Upload失敗
					taskFailures.add(result);
				}
			}
		} catch (Exception e) {
			throw new SimpleDBMapperS3HandleException("S3アップロード操作に失敗", e);
		}

		// UploadTask失敗があれば例外をスロー
		if (!taskFailures.isEmpty()) {
			throw new SimpleDBMapperS3HandleException(taskFailures);
		}

		// SDBにPUT
		req.setAttributes(replacableAttrs);
		this.sdb.putAttributes(req);

		// versionをセット
		if (versionField != null) {
			try {
				versionField.set(object, nowVersion);
			} catch (Exception ignore) {
				throw new SimpleDBMapperException("versionの値セットに失敗", ignore);
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
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
	 *            {@link SimpleDBVersionAttribute}
	 *            がついたフィールドがある場合、トランザクション機能が働きます。（<a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConditionalDelete.html"
	 *            >Conditional Delete</a>になります。）
	 */
	public void delete(Object object) {
		String domainName = this.reflector.getDomainName(object.getClass());
		Field itemNameField = this.reflector.findItemNameField(object.getClass());
		String itemName = this.reflector.encodeItemNameAsSimpleDBFormat(object, itemNameField);

		// S3 Blob削除対象をリストアップ
		GetAttributesResult results = this.sdb.getAttributes(new GetAttributesRequest(domainName, itemName));
		List<Attribute> sdbAllAttrs = results.getAttributes();
		Set<Field> blobFields = this.reflector.findBlobFields(object.getClass());
		List<S3TaskResult> s3TaskResults = new ArrayList<S3TaskResult>();
		for (Field field : blobFields) {
			SimpleDBBlob blobAnnon = field.getAnnotation(SimpleDBBlob.class);
			String attributeName = blobAnnon.attributeName();
			for (Attribute attr : sdbAllAttrs) {
				if (attr.getName().equals(attributeName)) {
					S3TaskResult taskResult = new S3TaskResult(Operation.DELETE, attributeName, null, null);
					taskResult.setSimpleDBAttributeValue(attr.getValue());
					s3TaskResults.add(taskResult);
				}
			}
		}

		DeleteAttributesRequest req = new DeleteAttributesRequest(domainName, itemName);
		// versionが入っていたらConditional Delete
		Field versionField = this.reflector.findVersionAttributeField(object.getClass());
		if (versionField != null) {
			try {
				Object versionObject = versionField.get(object);
				String versionAttributeName = versionField.getAnnotation(SimpleDBVersionAttribute.class)
						.attributeName();
				if (versionObject != null) {
					if (versionObject instanceof Long) {
						Long currentVersion = (Long) versionObject;
						UpdateCondition expected = new UpdateCondition();
						expected.setName(versionAttributeName);
						expected.setValue(currentVersion.toString());
						req.setExpected(expected);
					} else {
						throw new SimpleDBMapperException("version属性はLongである必要があります。" + versionField);
					}
				}
			} catch (Exception e) {
				throw new SimpleDBMapperException("objectからversion取得に失敗: " + object, e);
			}
		}
		this.sdb.deleteAttributes(req);

		// S3削除
		for (S3TaskResult s3TaskResult : s3TaskResults) {
			this.s3.deleteObject(s3TaskResult.getBucketName(), s3TaskResult.getKey());
		}
	}

	/**
	 * {@link SimpleDBDomain}で指定されたドメイン内のアイテムをすべてカウントします。
	 * 
	 * @param clazz
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 */
	public <T> int countAll(Class<T> clazz) {
		return count(clazz, null);
	}

	/**
	 * {@link SimpleDBDomain}で指定されたドメイン内のアイテムを条件カウントします。
	 * 
	 * @param clazz
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
	 * @param expression
	 *            where文
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 */
	public <T> int count(Class<T> clazz, QueryExpression expression) {
		String whereExpression = null;
		if (expression != null) {
			whereExpression = expression.describe();
		}
		String query = createQuery(clazz, true, whereExpression, 0);
		SelectResult result = this.sdb.select(new SelectRequest(query, this.config.isConsistentRead()));
		String countValue = result.getItems().get(0).getAttributes().get(0).getValue();
		return Integer.parseInt(countValue);
	}

	/**
	 * selectを実行します。件数の最大値は2500件です。
	 * 
	 * <a href=
	 * "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/UsingSelect.html"
	 * >AWSドキュメント参照</a>
	 * 
	 * @param clazz
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
	 * @param consistentRead
	 *            一貫性読み込みオプション。
	 * 
	 *            <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 * @return 0件の場合は空のListが返ってきます。
	 */
	public <T> List<T> selectAll(Class<T> clazz) {
		String query = createQuery(clazz, false, null, MAX_QUERY_LIMIT);
		List<T> objects = fetch(clazz, query);
		return objects;
	}

	/**
	 * selectを実行します。
	 * 
	 * <a href=
	 * "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/UsingSelect.html"
	 * >AWSドキュメント参照</a>
	 * 
	 * @param clazz
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
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
	public <T> List<T> select(Class<T> clazz, QueryExpression expression) {
		String whereExpression = expression.describe();
		String query = createQuery(clazz, false, whereExpression, expression.getLimit());
		long t = System.currentTimeMillis();
		List<T> objects = fetch(clazz, query);
		if (log.isDebugEnabled()) {
			log.debug(String.format("fetch time: %s(msec) query: %s", (System.currentTimeMillis() - t), query));
		}
		return objects;
	}

	/**
	 * @param clazz
	 *            {@link SimpleDBDomain}アノテーションがついたPOJO
	 * @param itemName
	 *            SimpleDBのitemNameで、{@link SimpleDBItemName}で指定した型のオブジェクト
	 * @param consistentRead
	 *            一貫性読み込みオプション。 <a href=
	 *            "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/ConsistencySummary.html"
	 *            >AWSドキュメント参照</a>
	 * @throws SimpleDBMapperNotFoundException
	 *             見つからなかった場合にスローされます
	 * @throws SimpleDBMapperUnsupportedTypeException
	 */
	public <T> T load(Class<T> clazz, Object itemName) throws SimpleDBMapperNotFoundException {
		if (!this.reflector.isItemNameSupportedType(itemName.getClass())) {
			throw new SimpleDBMapperUnsupportedTypeException(itemName.getClass() + " is not supported.");
		}

		String itemNameInQuery = this.reflector.encodeObjectAsSimpleDBFormat(itemName);

		String whereExpression = "itemName()=" + quoteValue(itemNameInQuery);
		String query = createQuery(clazz, false, whereExpression, 0);

		List<T> objects = fetch(clazz, query);
		if (objects.isEmpty()) {
			throw new SimpleDBMapperNotFoundException("見つかりません。" + query);
		}
		return objects.get(0);
	}

	public boolean hasNext() {
		if (this.selectNextToken != null) {
			return true;
		}
		return false;
	}

	private <T> List<T> fetch(Class<T> clazz, String query) {
		SelectRequest selectRequest = new SelectRequest(query.toString(), this.config.isConsistentRead());
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
		Field itemNameField = this.reflector.findItemNameField(clazz);
		try {
			// SDBのitemでループ
			for (Item item : items) {
				T instance;
				instance = clazz.newInstance();

				// ItemNameのセット
				Class<?> type = itemNameField.getType();
				String itemName = item.getName();
				itemNameField.set(instance, this.reflector.decodeItemNameFromSimpleDBFormat(type, itemName));

				// itemのattributesでループ
				List<Attribute> attrs = item.getAttributes();
				for (Attribute attr : attrs) {
					String attributeName = attr.getName();
					Field attrField = this.reflector.findFieldByAttributeName(clazz, attributeName);
					if (attrField == null) {
						continue;
					}
					// Blobの場合はLazyFetchをチェック
					SimpleDBBlob blobAnno = attrField.getAnnotation(SimpleDBBlob.class);
					if (blobAnno != null) {
						String fieldName = attrField.getName();
						if (this.blobEagerFetchList.contains(fieldName)) {
							// 実行
							this.reflector.setFieldValueFromAttribute(this.s3, clazz, instance, attr);
						} else {
							FetchType fetchType = blobAnno.fetch();
							if (fetchType == FetchType.EAGER) {
								// 実行
								this.reflector.setFieldValueFromAttribute(this.s3, clazz, instance, attr);
							}
						}
					} else {
						this.reflector.setFieldValueFromAttribute(this.s3, clazz, instance, attr);
					}

				}

				//
				objects.add(instance);
			}

		} catch (Exception e) {
			throw new SimpleDBMapperException(e);
		}

		return objects;
	}

	private <T> String createQuery(Class<T> clazz, boolean isCount, String whereExpression, int limit) {
		String domainName = this.reflector.getDomainName(clazz);
		StringBuilder query = new StringBuilder("select ");
		if (isCount) {
			query.append("count(*)");
		} else {
			query.append("*");
		}
		query.append(" from ");
		query.append(quoteName(domainName));
		if (whereExpression != null) {
			query.append(" where ");
			query.append(whereExpression);
		}
		if (limit > 0) {
			query.append(" limit ").append(limit);
		}
		return query.toString();
	}

	public <T> QueryExpressionBuilder<T> from(Class<T> clazz) {
		return new QueryExpressionBuilder<T>(clazz, this);
	}

}

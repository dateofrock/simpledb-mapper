package com.dateofrock.simpledbmapper;

/**
 * 
 * 
 * @author $LastChangedBy$
 * @author tanabe
 * @version $Revision$ $Date$
 */
public class SimpleDBMapperConfig {

	public static final int S3_UPLOAD_THREAD_POOL_SIZE = 3;

	private int s3UploadThreadPoolSize;
	private boolean consistentRead;

	public static final SimpleDBMapperConfig DEFAULT;

	static {
		DEFAULT = new SimpleDBMapperConfig(S3_UPLOAD_THREAD_POOL_SIZE, true);
	}

	public SimpleDBMapperConfig(int s3UploadThreadPoolSize, boolean consistentRead) {
		super();
		this.s3UploadThreadPoolSize = s3UploadThreadPoolSize;
		this.consistentRead = consistentRead;
	}

	public int getS3UploadThreadPoolSize() {
		return s3UploadThreadPoolSize;
	}

	public boolean isConsistentRead() {
		return consistentRead;
	}

}

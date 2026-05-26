package org.cryptomator.hub.entities.katta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "storage_profile_s3_sts")
@DiscriminatorValue("S3STS")
public final class StorageProfileS3STS extends StorageProfileS3Static {

	//======================================================================
	// (2) STS only: bucket creation (only relevant for Desktop client)
	//======================================================================
	@Column(name = "sts_role_create_bucket_client", nullable = false)
	public String stsRoleCreateBucketClient;

	@Column(name = "sts_role_create_bucket_hub", nullable = false)
	public String stsRoleCreateBucketHub;

	@Column(name = "sts_endpoint")
	public String stsEndpoint = null;

	@Column(name = "bucket_versioning", nullable = false)
	public Boolean bucketVersioning = true;

	@Column(name = "bucket_acceleration")
	public Boolean bucketAcceleration = true;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@Column(name = "sts_role_access_bucket_assume_role_with_web_identity", nullable = false)
	public String stsRoleAccessBucketAssumeRoleWithWebIdentity;

	@Column(name = "sts_role_access_bucket_assume_role_tagged_session")
	public String stsRoleAccessBucketAssumeRoleTaggedSession;

	@Column(name = "sts_duration_seconds")
	public Integer stsDurationSeconds = null;

	@Column(name = "sts_session_tag")
	public String stsSessionTag;
}

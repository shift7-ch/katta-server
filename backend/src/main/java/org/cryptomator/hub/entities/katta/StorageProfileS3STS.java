package org.cryptomator.hub.entities.katta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "storage_profile_s3_sts")
@DiscriminatorValue("S3STS")
public final class StorageProfileS3STS extends StorageProfileS3Static {

	//======================================================================
	// (2) STS only: bucket creation (only relevant for Desktop client)
	//======================================================================
	@Column(name = "region")
	public String region;

	@Column(name = "regions")
	public List<String> regions;

	@Column(name = "bucketPrefix", nullable = false)
	public String bucketPrefix;

	@Column(name = "stsRoleCreateBucketClient", nullable = false)
	public String stsRoleCreateBucketClient;

	@Column(name = "stsRoleCreateBucketHub", nullable = false)
	public String stsRoleCreateBucketHub;

	@Column(name = "stsEndpoint")
	public String stsEndpoint = null;

	@Column(name = "bucketVersioning", nullable = false)
	public Boolean bucketVersioning = true;

	@Column(name = "bucketAcceleration")
	public Boolean bucketAcceleration = true;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@Column(name = "stsRoleAccessBucketAssumeRoleWithWebIdentity", nullable = false)
	public String stsRoleAccessBucketAssumeRoleWithWebIdentity;

	@Column(name = "stsRoleAccessBucketAssumeRoleTaggedSession")
	public String stsRoleAccessBucketAssumeRoleTaggedSession;

	@Column(name = "stsDurationSeconds")
	public Integer stsDurationSeconds = null;

	@Column(name = "stsSessionTag")
	public String stsSessionTag;
}

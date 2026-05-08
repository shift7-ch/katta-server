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
	@Column
	public String region;

	@Column
	public List<String> regions;

	@Column
	public String bucketPrefix;

	@Column
	public String stsRoleCreateBucketClient;

	@Column
	public String stsRoleCreateBucketHub;

	@Column
	public String stsEndpoint = null;

	@Column
	public Boolean bucketVersioning = true;

	@Column
	public Boolean bucketAcceleration = true;

	@Column
	public String bucketEncryption;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@Column
	public String stsRoleAccessBucketAssumeRoleWithWebIdentity;

	@Column
	public String stsRoleAccessBucketAssumeRoleTaggedSession;

	@Column
	public Integer stsDurationSeconds = null;

	@Column
	public String stsSessionTag;
}

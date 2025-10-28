package org.cryptomator.hub.entities.cipherduck;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "storage_profile_s3_sts")
@DiscriminatorValue("S3STS")
public class StorageProfileS3STS extends StorageProfileS3 { // TODO make sealed/final?

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

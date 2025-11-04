package org.cryptomator.hub.entities.cipherduck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "storage_profile_s3_sts")
public class StorageProfileS3STS extends StorageProfileS3Static { // TODO make sealed/final?

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

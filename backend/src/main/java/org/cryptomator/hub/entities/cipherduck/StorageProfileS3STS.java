package org.cryptomator.hub.entities.cipherduck;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "storage_profile_s3_sts")
@DiscriminatorValue("S3STS")
public class StorageProfileS3STS extends StorageProfileS3 { // TODO make sealed/final?

	//======================================================================
	// (2) STS only: bucket creation
	//======================================================================
	@Column
	public String region;

	@Column
	public List<String> regions;

	@Column
	public String bucketPrefix;

	@Column
	public String stsRoleArnClient;

	@Column
	public String stsRoleArnHub;

	@Column
	public String stsEndpoint = null;

	@Column
	public Boolean bucketVersioning = true;

	//----------------------------------------------------------------------
	// (3b) STS client profile custom properties
	//----------------------------------------------------------------------
	@Column
	public String stsRoleArn;

	@Column
	public String stsRoleArn2;

	@Column
	public Integer stsDurationSeconds = null;

	public StorageProfileS3STS toEntity() {
		final StorageProfileS3STS storageProfileS3 = new StorageProfileS3STS();
		storageProfileS3.id = this.id;
		storageProfileS3.name = this.name;
		storageProfileS3.scheme = this.scheme;
		storageProfileS3.hostname = this.hostname;
		storageProfileS3.port = this.port;
		storageProfileS3.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfileS3.region = this.region;
		return storageProfileS3;
	}
}

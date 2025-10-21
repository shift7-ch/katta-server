package org.cryptomator.hub.entities.cipherduck;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "storage_profile_s3")
@DiscriminatorValue("S3")
public class StorageProfileS3 extends StorageProfile {// TODO make sealed?

	//======================================================================
	// (1) STS and permanent:
	// - bucket creation frontend/desktop client (STS)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================
	@Column
	public String scheme;

	@Column
	public String hostname;

	@Column
	public Integer port;

	@Column
	public Boolean withPathStyleAccessEnabled = false;

	@Column
	public String storageClass = "STANDARD";

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
	public String stsRoleArnClient;

	@Column
	public String stsRoleArnHub;

	@Column
	public String stsEndpoint = null;

	@Column
	public Boolean bucketVersioning = true;

	@Column
	public Boolean bucketAcceleration = true;

	@Column
	public String bucketEncryption;
}

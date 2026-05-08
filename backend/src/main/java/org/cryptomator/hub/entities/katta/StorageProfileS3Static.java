package org.cryptomator.hub.entities.katta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "storage_profile_s3_static")
public class StorageProfileS3Static extends StorageProfile {// TODO make sealed?

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
}

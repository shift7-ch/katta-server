package org.cryptomator.hub.entities.cipherduck;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "storage_profile_s3")
@DiscriminatorValue("s3-hub")
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
}

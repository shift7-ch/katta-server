package org.cryptomator.hub.entities.katta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "storage_profile_s3_static")
@DiscriminatorValue("S3STATIC")
public class StorageProfileS3Static extends StorageProfile {

	//======================================================================
	// (1) STS and permanent:
	// - bucket creation frontend/desktop client (STS)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================
	@Column(name = "endpoint")
	public String endpoint;

	@Column(name = "withPathStyleAccessEnabled", nullable = false)
	public Boolean withPathStyleAccessEnabled = false;

	@Column(name = "storageClass", columnDefinition = "storage_class", nullable = false)
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	public S3StorageClass storageClass = S3StorageClass.STANDARD;
}

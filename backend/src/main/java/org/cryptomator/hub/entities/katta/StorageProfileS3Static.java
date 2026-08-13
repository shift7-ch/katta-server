package org.cryptomator.hub.entities.katta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.List;

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
	public @Nullable String endpoint;

	@Column(name = "path_style_access_enabled", nullable = false)
	public boolean pathStyleAccessEnabled = false;

	@Column(name = "storage_class", columnDefinition = "storage_class", nullable = false)
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	public S3StorageClass storageClass = S3StorageClass.STANDARD;

	// bucket creation parameters, relevant for both permanent and STS profiles (desktop client creates buckets in either case)
	@Column(name = "region")
	public @Nullable String region;

	@Column(name = "regions")
	public @Nullable List<String> regions;

	@Column(name = "bucket_prefix", nullable = false)
	public String bucketPrefix;
}

package org.cryptomator.hub.api.cipherduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.cipherduck.StorageProfileS3;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

public sealed class StorageProfileS3Dto extends StorageProfileDto permits StorageProfileS3STSDto {

	public enum S3_STORAGE_CLASSES {
		STANDARD, INTELLIGENT_TIERING, STANDARD_IA, ONEZONE_IA, REDUCED_REDUNDANCY, GLACIER, GLACIER_IR, DEEP_ARCHIVE
	}

	//======================================================================
	// (1) STS and permanent:
	// - bucket creation frontend/desktop client (STS)
	// - template upload (STS and permanent)
	// - client profile (STS and permanent)
	//======================================================================

	@JsonProperty(value = "scheme", defaultValue = "https")
	@Schema(description = "Scheme of S3 endpoint for template upload/bucket creation. Defaults to default for protocol, i.e. https in most cases.", example = "https", nullable = true)
	String scheme;

	@JsonProperty("hostname")
	@Schema(description = "Hostname S3 endpoint for template upload/bucket creation. Defaults to AWS SDK default.", example = "s3-us-gov-west-1.amazonaws.com", nullable = true)
	String hostname;

	@JsonProperty("port")
	@Schema(description = "Port S3 endpoint for template upload/bucket creation. Defaults to default port for scheme.", example = "443", nullable = true)
	Integer port;

	@JsonProperty(value = "withPathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", example = "false", defaultValue = "false")
	Boolean withPathStyleAccessEnabled = false;

	@JsonProperty(value = "storageClass", defaultValue = "STANDARD")
	@Schema(description = "Storage class for upload. Defaults to STANDARD", example = "STANDARD", required = true)
	S3_STORAGE_CLASSES storageClass = S3_STORAGE_CLASSES.STANDARD;

	public StorageProfileS3Dto() {
		// jackson
	}

	public StorageProfileS3Dto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String scheme, final String hostname, final Integer port, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass) {
		super(id, name, protocol, archived);
		this.scheme = scheme;
		this.hostname = hostname;
		this.port = port;
		this.withPathStyleAccessEnabled = withPathStyleAccessEnabled;
		this.storageClass = storageClass;
	}

	static StorageProfileS3Dto fromEntity(final StorageProfileS3 storageProfile) {
		return new StorageProfileS3Dto(storageProfile.id, storageProfile.name, Protocol.s3, storageProfile.archived, storageProfile.scheme, storageProfile.hostname, storageProfile.port, storageProfile.withPathStyleAccessEnabled, S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass));
	}

	public StorageProfileS3 toEntity() {
		final StorageProfileS3 storageProfile = new StorageProfileS3();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.archived = this.archived;
		storageProfile.scheme = this.scheme;
		storageProfile.hostname = this.hostname;
		storageProfile.port = this.port;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = this.storageClass.name();
		return storageProfile;
	}

	public String scheme() {
		return scheme;
	}

	public String hostname() {
		return hostname;
	}

	public Integer port() {
		return port;
	}

	public Boolean withPathStyleAccessEnabled() {
		return withPathStyleAccessEnabled;
	}

	public S3_STORAGE_CLASSES storageClass() {
		return storageClass;
	}
}

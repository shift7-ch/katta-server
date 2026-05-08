package org.cryptomator.hub.api.katta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.katta.StorageClass;
import org.cryptomator.hub.entities.katta.StorageProfileS3Static;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Objects;
import java.util.UUID;

@Schema(title = "StorageProfileS3StaticDto")
public sealed class StorageProfileS3StaticDto extends StorageProfileDto permits StorageProfileS3STSDto {

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
	@Schema(description = "Scheme of S3 endpoint for template upload/bucket creation. Defaults to default for protocol, i.e. https in most cases.", examples = "https", nullable = true)
	String scheme;

	@JsonProperty("hostname")
	@Schema(description = "Hostname S3 endpoint for template upload/bucket creation. Defaults to AWS SDK default.", examples = "s3-us-gov-west-1.amazonaws.com", nullable = true)
	String hostname;

	@JsonProperty("port")
	@Schema(description = "Port S3 endpoint for template upload/bucket creation. Defaults to default port for scheme.", examples = "443", nullable = true)
	Integer port;

	@JsonProperty(value = "withPathStyleAccessEnabled")
	@Schema(description = "Whether to use path style for S3 endpoint for template upload/bucket creation.", examples = "false", defaultValue = "false")
	Boolean withPathStyleAccessEnabled = false;

	@JsonProperty(value = "storageClass", defaultValue = "STANDARD")
	@Schema(description = "Storage class for upload. Defaults to STANDARD", examples = "STANDARD", required = true)
	S3_STORAGE_CLASSES storageClass = S3_STORAGE_CLASSES.STANDARD;

	public StorageProfileS3StaticDto() {
		// jackson
	}

	public StorageProfileS3StaticDto(final UUID id, final String name, final Protocol protocol, final boolean archived, final String scheme, final String hostname, final Integer port, final boolean withPathStyleAccessEnabled, final S3_STORAGE_CLASSES storageClass) {
		super(id, name, protocol, archived);
		this.scheme = scheme;
		this.hostname = hostname;
		this.port = port;
		this.withPathStyleAccessEnabled = withPathStyleAccessEnabled;
		this.storageClass = storageClass;
	}

	static StorageProfileS3StaticDto fromEntity(final StorageProfileS3Static storageProfile) {
		return new StorageProfileS3StaticDto(storageProfile.id, storageProfile.name, Protocol.s3static, storageProfile.archived, storageProfile.scheme, storageProfile.hostname, storageProfile.port, storageProfile.withPathStyleAccessEnabled, S3_STORAGE_CLASSES.valueOf(storageProfile.storageClass.name()));
	}

	public StorageProfileS3Static toEntity() {
		final StorageProfileS3Static storageProfile = new StorageProfileS3Static();
		storageProfile.id = this.id;
		storageProfile.name = this.name;
		storageProfile.archived = this.archived;
		storageProfile.scheme = this.scheme;
		storageProfile.hostname = this.hostname;
		storageProfile.port = this.port;
		storageProfile.withPathStyleAccessEnabled = this.withPathStyleAccessEnabled;
		storageProfile.storageClass = StorageClass.valueOf(this.storageClass.name());
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StorageProfileS3StaticDto that = (StorageProfileS3StaticDto) o;
		return Objects.equals(scheme, that.scheme) && Objects.equals(hostname, that.hostname) && Objects.equals(port, that.port) && Objects.equals(withPathStyleAccessEnabled, that.withPathStyleAccessEnabled) && storageClass == that.storageClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(scheme, hostname, port, withPathStyleAccessEnabled, storageClass);
	}
}

CREATE TYPE storage_class AS ENUM (
    'STANDARD', 'INTELLIGENT_TIERING', 'STANDARD_IA', 'ONEZONE_IA',
    'REDUCED_REDUNDANCY', 'GLACIER', 'GLACIER_IR', 'DEEP_ARCHIVE'
);

CREATE TABLE "storage_profile"
(
	"id"        UUID    NOT NULL,
	"name"      VARCHAR NOT NULL,
	"archived"  bool    NOT NULL,
	"protocol"  VARCHAR NOT NULL,

	CONSTRAINT "STORAGE_PROFILE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_CHK_PROTOCOL" CHECK ("protocol" = 'S3STATIC' OR "protocol" = 'S3STS')
);

CREATE TABLE "storage_profile_s3_static"
(
	"id"        UUID NOT NULL,

	-- (1) bucket creation, template upload and client profile
	"endpoint"  VARCHAR,
	"path_style_access_enabled"
				bool NOT NULL,
	"storage_class" storage_class NOT NULL,

	-- bucket creation (desktop client), relevant for both permanent and STS profiles
	"region"    VARCHAR,
	"regions"   text[],
	"bucket_prefix" VARCHAR NOT NULL,

	CONSTRAINT "STORAGE_PROFILE_S3_STATIC_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_S3_STATIC_FK" FOREIGN KEY ("id") REFERENCES "storage_profile" ("id") ON DELETE CASCADE
);

CREATE TABLE "storage_profile_s3_sts"
(
	"id"        UUID    NOT NULL,

	-- (2) STS only (only relevant for Desktop client)
	"sts_role_create_bucket_client" VARCHAR NOT NULL,
	"sts_role_create_bucket_hub"    VARCHAR NOT NULL,
	"sts_endpoint" VARCHAR,

	-- (3b) client profile custom properties
	"sts_role_access_bucket_assume_role_with_web_identity" VARCHAR NOT NULL,
	"sts_role_access_bucket_assume_role_tagged_session"   VARCHAR,
	"sts_duration_seconds" INT4,
	"sts_session_tag"      VARCHAR,

	CONSTRAINT "STORAGE_PROFILE_S3_STS_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_S3_STS_FK" FOREIGN KEY ("id") REFERENCES "storage_profile_s3_static" ("id") ON DELETE CASCADE
);

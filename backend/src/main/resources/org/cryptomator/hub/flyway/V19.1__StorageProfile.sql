CREATE TABLE "storage_profile_s3"
(
	"id"                UUID NOT NULL,
   "name"              VARCHAR,

	-- (1) bucket creation, template upload and client profile
    "scheme"            VARCHAR,
    "hostname"          VARCHAR,
    "port"              INT4,
    "withPathStyleAccessEnabled"
                        bool NOT NULL,
    "storageClass" VARCHAR NOT NULL,

  -- (2) bucket creation only (only relevant for Desktop client)
    "region"            VARCHAR,
    "regions"           text[],
    "bucketPrefix"      VARCHAR NOT NULL,
    "stsRoleCreateBucketClient"  VARCHAR NOT NULL,
    "stsRoleCreateBucketHub"     VARCHAR NOT NULL,
    "stsEndpoint"       VARCHAR,
    "bucketVersioning"  bool NOT NULL,
    "bucketAcceleration" bool,
    "bucketEncryption" VARCHAR NOT NULL,

  -- (3) client profile
  -- (3a) client profile attributes
    "protocol"           VARCHAR NOT NULL,
    "archived"           bool NOT NULL,


	CONSTRAINT "STORAGE_PROFILE_S3_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_S3_CHK_STORAGE_CLASS" CHECK ("storageClass" = 'STANDARD' OR "storageClass" = 'INTELLIGENT_TIERING' OR "storageClass" = 'STANDARD_IA' OR "storageClass" = 'ONEZONE_IA' OR "storageClass" = 'GLACIER' OR "storageClass" = 'GLACIER_IR' OR "storageClass" = 'DEEP_ARCHIVE'),
	CONSTRAINT "STORAGE_PROFILE_S3_CHK_BUCKET_ENCRYPTION" CHECK ("bucketEncryption" = 'NONE' OR "bucketEncryption" = 'SSE_AES256' OR "bucketEncryption" = 'SSE_KMS_DEFAULT')
);

CREATE TABLE "storage_profile_s3_sts"
(
    "id"                UUID NOT NULL,
    "name"              VARCHAR,

  -- (1) bucket creation, template upload and client profile
    "scheme"            VARCHAR,
    "hostname"          VARCHAR,
    "port"              INT4,
    "withPathStyleAccessEnabled"
                        bool NOT NULL,
    "storageClass" VARCHAR NOT NULL,

  -- (2) bucket creation only (only relevant for Desktop client)
    "region"            VARCHAR,
    "regions"           text[],
    "bucketPrefix"      VARCHAR NOT NULL,
    "stsRoleCreateBucketClient"  VARCHAR NOT NULL,
    "stsRoleCreateBucketHub"     VARCHAR NOT NULL,
    "stsEndpoint"       VARCHAR,
    "bucketVersioning"  bool NOT NULL,
    "bucketAcceleration" bool,
    "bucketEncryption" VARCHAR NOT NULL,

  -- (3) client profile
  -- (3a) client profile attributes
    "protocol"           VARCHAR NOT NULL,
    "archived"           bool NOT NULL,
  -- (3b) client profile custom properties
    "stsRoleAccessBucketAssumeRoleWithWebIdentity"  VARCHAR NOT NULL,
    "stsRoleAccessBucketAssumeRoleTaggedSession"    VARCHAR,
    "stsDurationSeconds" INT4,
    "stsSessionTag"      VARCHAR,

  CONSTRAINT "STORAGE_PROFILE_S3_STS_PK" PRIMARY KEY ("id"),
  CONSTRAINT "STORAGE_PROFILE_S3_STS_CHK_STORAGE_CLASS" CHECK ("storageClass" = 'STANDARD' OR "storageClass" = 'INTELLIGENT_TIERING' OR "storageClass" = 'STANDARD_IA' OR "storageClass" = 'ONEZONE_IA' OR "storageClass" = 'GLACIER' OR "storageClass" = 'GLACIER_IR' OR "storageClass" = 'DEEP_ARCHIVE'),
  CONSTRAINT "STORAGE_PROFILE_S3_STS_CHK_BUCKET_ENCRYPTION" CHECK ("bucketEncryption" = 'NONE' OR "bucketEncryption" = 'SSE_AES256' OR "bucketEncryption" = 'SSE_KMS_DEFAULT')
);
CREATE TABLE "storage_profile"
(
	"id"                UUID NOT NULL,
    "name"              VARCHAR,

    -- (3) client profile
    -- (3a) client profile attributes
    "protocol"           VARCHAR NOT NULL,
    "archived"           bool NOT NULL,

	CONSTRAINT "STORAGE_PROFILE_PK" PRIMARY KEY ("id")
	--CONSTRAINT "AUTHORITY_CHK_TYPE" CHECK ("type" = 'USER' OR "type" = 'GROUP')
);

CREATE TABLE "storage_profile_s3"
(
	"id"                UUID NOT NULL,

	-- (1) bucket creation, template upload and client profile
    "scheme"            VARCHAR,
    "hostname"          VARCHAR,
    "port"              INT4,
    "withPathStyleAccessEnabled"
                        bool NOT NULL,

	CONSTRAINT "STORAGE_PROFILE_S3_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_S3_FK_STORAGE_PROFILE" FOREIGN KEY ("id") REFERENCES "storage_profile" ("id") ON DELETE CASCADE
);

CREATE TABLE "storage_profile_s3_sts"
(
    "id"                UUID NOT NULL,

    -- (2) bucket creation only (i.e. STS-case)
    "region"            VARCHAR,
    "regions"           text[],
    "bucketPrefix"      VARCHAR NOT NULL,
    "stsRoleArnClient"  VARCHAR NOT NULL,
    "stsRoleArnHub"     VARCHAR NOT NULL,
    "stsEndpoint"       VARCHAR,
    "bucketVersioning" bool NOT NULL,

    -- (3b) client profile custom properties
    "stsRoleArn"         VARCHAR NOT NULL,
    "stsRoleArn2"        VARCHAR,
    "stsDurationSeconds" INT4,

	CONSTRAINT "STORAGE_PROFILE_S3_STS_PK" PRIMARY KEY ("id"),
	CONSTRAINT "STORAGE_PROFILE_S3_STS_FK_STORAGE_PROFILE_S3" FOREIGN KEY ("id") REFERENCES "storage_profile_s3" ("id") ON DELETE CASCADE
);
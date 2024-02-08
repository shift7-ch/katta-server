CREATE TABLE "storage_profile"
(
	"id"                UUID NOT NULL,
    "name"              VARCHAR,

    -- (1) bucket creation, template upload and client profile
    "scheme"            VARCHAR,
    "hostname"          VARCHAR,
    "port"              INT4,
    "region"            VARCHAR,
    "regions"           text[],
    "withPathStyleAccessEnabled"
                        bool,

    -- (2) bucket creation only (i.e. STS-case)
    "bucketPrefix"      VARCHAR,
    "stsRoleArnClient"  VARCHAR,
    "stsRoleArnHub"     VARCHAR,
    "stsEndpoint"       VARCHAR,
    "bucketVersioning" bool,


	-- (3) client profile
	-- (3a) client profile attributes
    "protocol"           VARCHAR NOT NULL,

    -- (3b) client profile custom properties
    "stsRoleArn"         VARCHAR,
    "stsRoleArn2"        VARCHAR,
    "stsDurationSeconds" INT4,

	CONSTRAINT "STORAGE_PROFILE_PK" PRIMARY KEY ("id")
);
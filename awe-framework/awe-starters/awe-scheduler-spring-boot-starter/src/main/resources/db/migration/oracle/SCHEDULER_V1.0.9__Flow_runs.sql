-- ------------------------------------------------------
--  Flow runs: run, run parameters and join state tables,
--  plus the run id of each task execution (additive, nullable)
-- ------------------------------------------------------
CREATE TABLE "AWESCHRUN"
(
    "IDERUN" VARCHAR2(36 BYTE) NOT NULL,
    "IDETSKROO" NUMBER(*, 0) NOT NULL,
    "EXETSKROO" NUMBER(*, 0) NOT NULL,
    "LCHBY" VARCHAR2(200 BYTE),
    "INIDAT" timestamp NOT NULL,
    "ENDDAT" timestamp,
    "STA" NUMBER(*, 0) NOT NULL,
    PRIMARY KEY ("IDERUN")
);

CREATE TABLE "AWESCHRUNPAR"
(
    "IDERUN" VARCHAR2(36 BYTE) NOT NULL,
    "NAM" VARCHAR2(40 BYTE) NOT NULL,
    "VAL" VARCHAR2(400 BYTE),
    PRIMARY KEY ("IDERUN", "NAM")
);

CREATE TABLE "AWESCHRUNTSK"
(
    "IDERUN" VARCHAR2(36 BYTE) NOT NULL,
    "IDETSK" NUMBER(*, 0) NOT NULL,
    "STA" VARCHAR2(10 BYTE) NOT NULL,
    "EXETSK" NUMBER(*, 0),
    "UPDDAT" timestamp NOT NULL,
    PRIMARY KEY ("IDERUN", "IDETSK")
);

ALTER TABLE "AWESCHEXE" ADD ("IDERUN" VARCHAR2(36 BYTE));
CREATE INDEX "AWESCHEXEI2" ON "AWESCHEXE" ("IDERUN");

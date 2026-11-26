-- ------------------------------------------------------
--  Webhook launch
--  AweSchTskHok: the hook of a WEBHOOK task (one per task). The secret is not stored: SecPrp names the property that holds it.
--  AweSchHokMsg: the inbox of received webhooks, deduplicated by source (hook key) and message id.
--  AweSchTskPar.WhkPtr: JSON pointer of the webhook data that gives the value of a parameter.
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
CREATE TABLE "AWESCHTSKHOK"
(
    "IDETSK" NUMBER(*, 0)        NOT NULL,
    "HOKKEY" VARCHAR2(64 BYTE)  NOT NULL,
    "ACT"    NUMBER(*, 0)        NOT NULL,
    "SECPRP" VARCHAR2(200 BYTE) NOT NULL,
    "TYPFLT" VARCHAR2(200 BYTE),
    CONSTRAINT "PK_AWESCHTSKHOK" PRIMARY KEY ("IDETSK"),
    CONSTRAINT "UK_AWESCHTSKHOK" UNIQUE ("HOKKEY")
);

CREATE TABLE "AWESCHHOKMSG"
(
    "SRC"    VARCHAR2(64 BYTE)   NOT NULL,
    "MSGID"  VARCHAR2(256 BYTE)  NOT NULL,
    "RCVDAT" timestamp          NOT NULL,
    "TYP"    VARCHAR2(200 BYTE),
    "PAY"    CLOB               NOT NULL,
    "STA"    VARCHAR2(10 BYTE)   NOT NULL,
    "ATT"    NUMBER(*, 0)                 NOT NULL,
    "NXTDAT" timestamp          NOT NULL,
    "PRCDAT" timestamp,
    "ERR"    VARCHAR2(2000 BYTE),
    "IDETSK" NUMBER(*, 0),
    "EXETSK" NUMBER(*, 0),
    "IDERUN" VARCHAR2(36 BYTE),
    CONSTRAINT "PK_AWESCHHOKMSG" PRIMARY KEY ("SRC", "MSGID")
);

CREATE INDEX "AWESCHHOKMSGI1" ON "AWESCHHOKMSG" ("STA", "NXTDAT");

ALTER TABLE "AWESCHTSKPAR" ADD "WHKPTR" VARCHAR2(250 BYTE);

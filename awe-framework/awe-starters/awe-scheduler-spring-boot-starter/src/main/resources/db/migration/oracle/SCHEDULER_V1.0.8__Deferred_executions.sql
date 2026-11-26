-- ------------------------------------------------------
--  Deferred executions
--  AweSchExeExt: one row per execution that answered "accepted, I finish later" (deferred completion).
--  The row stays after the execution is closed: it tells a deferred execution from one that never was.
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
CREATE TABLE "AWESCHEXEEXT"
(
    "IDETSK" NUMBER(*, 0)       NOT NULL,
    "EXETSK" NUMBER(*, 0)       NOT NULL,
    "EXTREF" VARCHAR2(256 BYTE),
    "DFRDAT" timestamp          NOT NULL,
    "DUEDAT" timestamp          NOT NULL,
    CONSTRAINT "PK_AWESCHEXEEXT" PRIMARY KEY ("IDETSK", "EXETSK")
);

-- ------------------------------------------------------
--  Trace of the deferred executions (#988)
--  AweSchExeExt: completion date and source (API, DEADLINE, USER) and outcome of the cancellation notice
--  AweSchTsk / HISAweSchTsk: optional maintain that is notified when a deferred execution is cancelled
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
ALTER TABLE "AWESCHEXEEXT"
    ADD (
    "CMPDAT" timestamp,
    "CMPSRC" VARCHAR2(10 BYTE),
    "CNCSTA" VARCHAR2(10 BYTE),
    "CNCERR" VARCHAR2(2000 BYTE)
);

ALTER TABLE "AWESCHTSK"
    ADD (
    "CNCMNT" VARCHAR2(200 BYTE)
);

ALTER TABLE "HISAWESCHTSK"
    ADD (
    "CNCMNT" VARCHAR2(200 BYTE)
);

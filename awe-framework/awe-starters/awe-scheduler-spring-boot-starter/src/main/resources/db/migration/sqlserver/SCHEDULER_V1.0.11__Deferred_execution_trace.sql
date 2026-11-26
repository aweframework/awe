-- ------------------------------------------------------
--  Trace of the deferred executions (#988)
--  AweSchExeExt: completion date and source (API, DEADLINE, USER) and outcome of the cancellation notice
--  AweSchTsk / HISAweSchTsk: optional maintain that is notified when a deferred execution is cancelled
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
ALTER TABLE AweSchExeExt ADD CmpDat DATETIME2(3);
ALTER TABLE AweSchExeExt ADD CmpSrc VARCHAR(10);
ALTER TABLE AweSchExeExt ADD CncSta VARCHAR(10);
ALTER TABLE AweSchExeExt ADD CncErr VARCHAR(2000);
ALTER TABLE AweSchTsk ADD CncMnt VARCHAR(200);
ALTER TABLE HISAweSchTsk ADD CncMnt VARCHAR(200);

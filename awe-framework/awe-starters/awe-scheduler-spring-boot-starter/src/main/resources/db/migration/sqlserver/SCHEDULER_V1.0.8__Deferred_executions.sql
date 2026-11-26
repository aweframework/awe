-- ------------------------------------------------------
--  Deferred executions
--  AweSchExeExt: one row per execution that answered "accepted, I finish later" (deferred completion).
--  The row stays after the execution is closed: it tells a deferred execution from one that never was.
--  Additive and backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
CREATE TABLE AweSchExeExt
(
    IdeTsk INT          not NULL,
    ExeTsk INT          not NULL,
    ExtRef VARCHAR(256),
    DfrDat DATETIME2(3) not NULL,
    DueDat DATETIME2(3) not NULL,
    CONSTRAINT PK_AWESCHEXEEXT PRIMARY KEY (IdeTsk, ExeTsk)
);

-- ------------------------------------------------------
--  DDL for Table AweSchExeLog
--  Bounded task execution log window
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchExeLog
(
    IdeTsk INTEGER      not NULL,
    ExeTsk INTEGER      not NULL,
    Src    VARCHAR(1)   not NULL,
    Sec    VARCHAR(1)   not NULL,
    Slt    INTEGER      not NULL,
    LinNum INTEGER      not NULL,
    LinTxt VARCHAR(4000),
    LogDat TIMESTAMP(3) not NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS PK_AWESCHEXELOG ON AweSchExeLog (IdeTsk, ExeTsk, Src, Sec, Slt);
CREATE INDEX IF NOT EXISTS AWESCHEXELOGI1 ON AweSchExeLog (IdeTsk, ExeTsk, LogDat);
ALTER TABLE AweSchExeLog ADD PRIMARY KEY (IdeTsk, ExeTsk, Src, Sec, Slt);

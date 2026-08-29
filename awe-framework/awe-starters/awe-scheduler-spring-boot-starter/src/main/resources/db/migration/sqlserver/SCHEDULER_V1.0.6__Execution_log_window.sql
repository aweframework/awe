--------------------------------------------------------
--  DDL for Table AweSchExeLog
--  Bounded task execution log window
--------------------------------------------------------
CREATE TABLE AweSchExeLog
(
    IdeTsk INT           not NULL,
    ExeTsk INT           not NULL,
    Src    VARCHAR(1)    not NULL,
    Sec    VARCHAR(1)    not NULL,
    Slt    INT           not NULL,
    LinNum INT           not NULL,
    LinTxt VARCHAR(4000),
    LogDat DATETIME2(3)  not NULL
);

CREATE INDEX AweSchExeLogI1 ON AweSchExeLog (IdeTsk, ExeTsk, LogDat);

ALTER TABLE AweSchExeLog
    ADD CONSTRAINT UN_AweSchExeLog UNIQUE (IdeTsk, ExeTsk, Src, Sec, Slt);
ALTER TABLE AweSchExeLog
    ADD PRIMARY KEY (IdeTsk, ExeTsk, Src, Sec, Slt);

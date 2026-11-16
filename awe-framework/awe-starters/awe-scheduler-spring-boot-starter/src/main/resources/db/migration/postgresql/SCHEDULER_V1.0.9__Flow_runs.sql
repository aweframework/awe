-- ------------------------------------------------------
--  Flow runs: run, run parameters and join state tables,
--  plus the run id of each task execution (additive, nullable)
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweSchRun
(
    IdeRun VARCHAR(36) NOT NULL,
    IdeTskRoo INTEGER NOT NULL,
    ExeTskRoo INTEGER NOT NULL,
    LchBy VARCHAR(200),
    IniDat TIMESTAMP(3) NOT NULL,
    EndDat TIMESTAMP(3),
    Sta INTEGER NOT NULL,
    PRIMARY KEY (IdeRun)
);

CREATE TABLE IF NOT EXISTS AweSchRunPar
(
    IdeRun VARCHAR(36) NOT NULL,
    Nam VARCHAR(40) NOT NULL,
    Val VARCHAR(400),
    PRIMARY KEY (IdeRun, Nam)
);

CREATE TABLE IF NOT EXISTS AweSchRunTsk
(
    IdeRun VARCHAR(36) NOT NULL,
    IdeTsk INTEGER NOT NULL,
    Sta VARCHAR(10) NOT NULL,
    ExeTsk INTEGER,
    UpdDat TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (IdeRun, IdeTsk)
);

ALTER TABLE AweSchExe ADD COLUMN IdeRun VARCHAR(36);
CREATE INDEX IF NOT EXISTS AWESCHEXEI2 ON AweSchExe (IdeRun);

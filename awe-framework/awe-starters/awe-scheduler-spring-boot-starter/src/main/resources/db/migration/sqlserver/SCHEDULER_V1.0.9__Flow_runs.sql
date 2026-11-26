-- ------------------------------------------------------
--  Flow runs: run, run parameters and join state tables,
--  plus the run id of each task execution (additive, nullable)
-- ------------------------------------------------------
CREATE TABLE AweSchRun
(
    IdeRun VARCHAR(36) NOT NULL,
    IdeTskRoo INT NOT NULL,
    ExeTskRoo INT NOT NULL,
    LchBy VARCHAR(200),
    IniDat DATETIME2(3) NOT NULL,
    EndDat DATETIME2(3),
    Sta INT NOT NULL,
    PRIMARY KEY (IdeRun)
);

CREATE TABLE AweSchRunPar
(
    IdeRun VARCHAR(36) NOT NULL,
    Nam VARCHAR(40) NOT NULL,
    Val VARCHAR(400),
    PRIMARY KEY (IdeRun, Nam)
);

CREATE TABLE AweSchRunTsk
(
    IdeRun VARCHAR(36) NOT NULL,
    IdeTsk INT NOT NULL,
    Sta VARCHAR(10) NOT NULL,
    ExeTsk INT,
    UpdDat DATETIME2(3) NOT NULL,
    PRIMARY KEY (IdeRun, IdeTsk)
);

ALTER TABLE AweSchExe ADD IdeRun VARCHAR(36);
CREATE INDEX AweSchExeI2 ON AweSchExe (IdeRun);

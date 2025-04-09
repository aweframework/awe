-- ------------------------------------------------------
--  UPDATE HISope columns IN audit tables
-- ------------------------------------------------------
ALTER TABLE HISAweSchCal MODIFY HISope CHAR(100) NULL;
ALTER TABLE HISAweSchCalDat MODIFY HISope CHAR(100) NULL;
ALTER TABLE HISAweSchSrv MODIFY HISope CHAR(100) NULL;
ALTER TABLE HISAweSchTsk MODIFY HISope CHAR(100) NULL;
ALTER TABLE HISAweSchTskLch MODIFY HISope CHAR(100) NULL;
ALTER TABLE HISAweSchTskPar MODIFY HISope CHAR(100) NULL;
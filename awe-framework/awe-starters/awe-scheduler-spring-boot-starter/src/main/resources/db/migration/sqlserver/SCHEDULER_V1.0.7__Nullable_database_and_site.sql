-- ------------------------------------------------------
--  Set db and site as nullables in AweSchCal, HISAweSchCal, AweSchTsk and HISAweSchTsk (issue #757)
--  Re-runnable: each column is altered only while it is still NOT NULL
-- ------------------------------------------------------
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('AweSchCal') AND name = 'db' AND is_nullable = 0)
    ALTER TABLE AweSchCal ALTER COLUMN db VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('AweSchCal') AND name = 'site' AND is_nullable = 0)
    ALTER TABLE AweSchCal ALTER COLUMN site VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HISAweSchCal') AND name = 'db' AND is_nullable = 0)
    ALTER TABLE HISAweSchCal ALTER COLUMN db VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HISAweSchCal') AND name = 'site' AND is_nullable = 0)
    ALTER TABLE HISAweSchCal ALTER COLUMN site VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('AweSchTsk') AND name = 'db' AND is_nullable = 0)
    ALTER TABLE AweSchTsk ALTER COLUMN db VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('AweSchTsk') AND name = 'site' AND is_nullable = 0)
    ALTER TABLE AweSchTsk ALTER COLUMN site VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HISAweSchTsk') AND name = 'db' AND is_nullable = 0)
    ALTER TABLE HISAweSchTsk ALTER COLUMN db VARCHAR(200) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('HISAweSchTsk') AND name = 'site' AND is_nullable = 0)
    ALTER TABLE HISAweSchTsk ALTER COLUMN site VARCHAR(200) NULL;

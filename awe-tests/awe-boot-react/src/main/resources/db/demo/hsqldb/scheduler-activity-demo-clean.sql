-- Demo data of the scheduler Activity screen (awe-boot-react, hot-reload profile only): removal.
-- Removes every demo row: tasks 9012-9020, servers 9001-9004, calendars 9001-9003 with their dates and anything the
-- tasks produced (executions, deferred rows, flow runs, execution log lines), including the executions launched later
-- from the screen. Run on its own to remove the demo data from a local database; SchedulerActivityDemoData runs it
-- first on every start.

DELETE FROM AweSchExeLog WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchExeExt WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchRunTsk WHERE IdeRun IN (SELECT IdeRun FROM AweSchRun WHERE IdeTskRoo BETWEEN 9012 AND 9020);
DELETE FROM AweSchRunPar WHERE IdeRun IN (SELECT IdeRun FROM AweSchRun WHERE IdeTskRoo BETWEEN 9012 AND 9020);
DELETE FROM AweSchRun WHERE IdeTskRoo BETWEEN 9012 AND 9020;
DELETE FROM AweSchExe WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchTskFilMod WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchTskDpn WHERE IdeTsk BETWEEN 9012 AND 9020 OR IdePrn BETWEEN 9012 AND 9020;
DELETE FROM AweSchTskHok WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchTskPar WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchTskLch WHERE IdeTsk BETWEEN 9012 AND 9020;
DELETE FROM AweSchTsk WHERE Ide BETWEEN 9012 AND 9020;
DELETE FROM AweSchSrv WHERE Ide BETWEEN 9001 AND 9004;
DELETE FROM AweSchCalDat WHERE IdeCal BETWEEN 9001 AND 9003;
DELETE FROM AweSchCal WHERE Ide BETWEEN 9001 AND 9003;

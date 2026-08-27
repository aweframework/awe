-- ------------------------------------------------------
--  Unify FTP credentials into the scheduler server row
--  Carries the per-task launcher credentials over to the ftp server they
--  point at, then drops the now-unused launcher credential columns.
--  Winner on conflict: the lowest AweSchTskLch.Ide with a non-empty user.
--  Servers that already carry credentials are left untouched.
-- ------------------------------------------------------
UPDATE AweSchSrv
SET SshUsr = (SELECT Lch.SrvUsr
              FROM AweSchTskLch Lch
              WHERE Lch.IdSrv = AweSchSrv.Ide
                AND Lch.SrvUsr IS NOT NULL
                AND Lch.SrvUsr <> ''
                AND Lch.Ide = (SELECT MIN(Win.Ide)
                               FROM AweSchTskLch Win
                               WHERE Win.IdSrv = AweSchSrv.Ide
                                 AND Win.SrvUsr IS NOT NULL
                                 AND Win.SrvUsr <> '')),
    SshPwd = (SELECT Lch.SrvPwd
              FROM AweSchTskLch Lch
              WHERE Lch.IdSrv = AweSchSrv.Ide
                AND Lch.SrvUsr IS NOT NULL
                AND Lch.SrvUsr <> ''
                AND Lch.Ide = (SELECT MIN(Win.Ide)
                               FROM AweSchTskLch Win
                               WHERE Win.IdSrv = AweSchSrv.Ide
                                 AND Win.SrvUsr IS NOT NULL
                                 AND Win.SrvUsr <> ''))
WHERE LOWER(AweSchSrv.Pro) = 'ftp'
  AND (AweSchSrv.SshUsr IS NULL OR AweSchSrv.SshUsr = '')
  AND (AweSchSrv.SshPwd IS NULL OR AweSchSrv.SshPwd = '')
  AND EXISTS (SELECT 1
              FROM AweSchTskLch Chk
              WHERE Chk.IdSrv = AweSchSrv.Ide
                AND Chk.SrvUsr IS NOT NULL
                AND Chk.SrvUsr <> '');

ALTER TABLE AweSchTskLch DROP COLUMN SrvUsr;
ALTER TABLE AweSchTskLch DROP COLUMN SrvPwd;
ALTER TABLE HISAweSchTskLch DROP COLUMN SrvUsr;
ALTER TABLE HISAweSchTskLch DROP COLUMN SrvPwd;

-- ------------------------------------------------------
--  Add SSH credential columns to scheduler server table
--  Additive, nullable, backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
ALTER TABLE AweSchSrv ADD COLUMN SshUsr VARCHAR(200);
ALTER TABLE AweSchSrv ADD COLUMN SshPwd VARCHAR(200);
ALTER TABLE AweSchSrv ADD COLUMN SshKey VARCHAR(4000);
ALTER TABLE AweSchSrv ADD COLUMN SshKeyPass VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD COLUMN SshUsr VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD COLUMN SshPwd VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD COLUMN SshKey VARCHAR(4000);
ALTER TABLE HISAweSchSrv ADD COLUMN SshKeyPass VARCHAR(200);

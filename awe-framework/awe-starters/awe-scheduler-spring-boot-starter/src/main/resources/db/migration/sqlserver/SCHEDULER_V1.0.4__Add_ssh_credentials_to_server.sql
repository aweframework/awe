-- ------------------------------------------------------
--  Add SSH credential columns to scheduler server table
--  Additive, nullable, backward-compatible (existing rows unaffected)
-- ------------------------------------------------------
ALTER TABLE AweSchSrv ADD SshUsr VARCHAR(200);
ALTER TABLE AweSchSrv ADD SshPwd VARCHAR(200);
ALTER TABLE AweSchSrv ADD SshKey VARCHAR(4000);
ALTER TABLE AweSchSrv ADD SshKeyPass VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD SshUsr VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD SshPwd VARCHAR(200);
ALTER TABLE HISAweSchSrv ADD SshKey VARCHAR(4000);
ALTER TABLE HISAweSchSrv ADD SshKeyPass VARCHAR(200);

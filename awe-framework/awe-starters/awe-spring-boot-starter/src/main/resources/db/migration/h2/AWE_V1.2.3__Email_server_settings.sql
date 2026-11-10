-- ------------------------------------------------------
--  Email servers (#562, #870): longer server name, user and password, security mode (NONE, STARTTLS, SSL) and the
--  Spring Security OAuth2 client registration used to get the SMTP token. Ath keeps its values (0 none, 1 user
--  and password) and adds 2 (OAuth2). The script can be applied again: it only widens shorter columns and only adds
--  missing ones.
-- ------------------------------------------------------
ALTER TABLE AweEmlSrv ALTER COLUMN SrvNam SET DATA TYPE varchar(100);
ALTER TABLE AweEmlSrv ALTER COLUMN EmlUsr SET DATA TYPE varchar(255);
ALTER TABLE AweEmlSrv ALTER COLUMN EmlPwd SET DATA TYPE varchar(512);
ALTER TABLE AweEmlSrv ADD COLUMN IF NOT EXISTS SecMod varchar(10) NULL;  --  Security mode: NONE, STARTTLS or SSL (NULL is NONE)
ALTER TABLE AweEmlSrv ADD COLUMN IF NOT EXISTS OauReg varchar(100) NULL;  --  OAuth2 client registration id
ALTER TABLE HISAweEmlSrv ALTER COLUMN SrvNam SET DATA TYPE varchar(100);
ALTER TABLE HISAweEmlSrv ALTER COLUMN EmlUsr SET DATA TYPE varchar(255);
ALTER TABLE HISAweEmlSrv ALTER COLUMN EmlPwd SET DATA TYPE varchar(512);
ALTER TABLE HISAweEmlSrv ADD COLUMN IF NOT EXISTS SecMod varchar(10) NULL;  --  Security mode: NONE, STARTTLS or SSL (NULL is NONE)
ALTER TABLE HISAweEmlSrv ADD COLUMN IF NOT EXISTS OauReg varchar(100) NULL;  --  OAuth2 client registration id

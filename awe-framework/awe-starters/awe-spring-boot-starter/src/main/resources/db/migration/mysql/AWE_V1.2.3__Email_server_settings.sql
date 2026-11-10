-- ------------------------------------------------------
--  Email servers (#562, #870): longer server name, user and password, security mode (NONE, STARTTLS, SSL) and the
--  Spring Security OAuth2 client registration used to get the SMTP token. Ath keeps its values (0 none, 1 user
--  and password) and adds 2 (OAuth2). The script can be applied again: it only widens shorter columns and only adds
--  missing ones.
-- ------------------------------------------------------
SET @aweemlsrv_srvnam = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE AweEmlSrv MODIFY SrvNam varchar(100) NOT NULL', 'DO 0')
                         FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweemlsrv' AND LOWER(column_name) = 'srvnam'
                           AND character_maximum_length < 100);
PREPARE aweemlsrv_srvnam FROM @aweemlsrv_srvnam;
EXECUTE aweemlsrv_srvnam;
DEALLOCATE PREPARE aweemlsrv_srvnam;

SET @aweemlsrv_emlusr = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE AweEmlSrv MODIFY EmlUsr varchar(255) NULL', 'DO 0')
                         FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweemlsrv' AND LOWER(column_name) = 'emlusr'
                           AND character_maximum_length < 255);
PREPARE aweemlsrv_emlusr FROM @aweemlsrv_emlusr;
EXECUTE aweemlsrv_emlusr;
DEALLOCATE PREPARE aweemlsrv_emlusr;

SET @aweemlsrv_emlpwd = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE AweEmlSrv MODIFY EmlPwd varchar(512) NULL', 'DO 0')
                         FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweemlsrv' AND LOWER(column_name) = 'emlpwd'
                           AND character_maximum_length < 512);
PREPARE aweemlsrv_emlpwd FROM @aweemlsrv_emlpwd;
EXECUTE aweemlsrv_emlpwd;
DEALLOCATE PREPARE aweemlsrv_emlpwd;

SET @aweemlsrv_secmod = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweEmlSrv ADD COLUMN SecMod varchar(10) NULL', 'DO 0')
                         FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweemlsrv' AND LOWER(column_name) = 'secmod');
PREPARE aweemlsrv_secmod FROM @aweemlsrv_secmod;
EXECUTE aweemlsrv_secmod;
DEALLOCATE PREPARE aweemlsrv_secmod;

SET @aweemlsrv_oaureg = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweEmlSrv ADD COLUMN OauReg varchar(100) NULL', 'DO 0')
                         FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweemlsrv' AND LOWER(column_name) = 'oaureg');
PREPARE aweemlsrv_oaureg FROM @aweemlsrv_oaureg;
EXECUTE aweemlsrv_oaureg;
DEALLOCATE PREPARE aweemlsrv_oaureg;

SET @hisaweemlsrv_srvnam = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE HISAweEmlSrv MODIFY SrvNam varchar(100) NULL', 'DO 0')
                            FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisaweemlsrv' AND LOWER(column_name) = 'srvnam'
                              AND character_maximum_length < 100);
PREPARE hisaweemlsrv_srvnam FROM @hisaweemlsrv_srvnam;
EXECUTE hisaweemlsrv_srvnam;
DEALLOCATE PREPARE hisaweemlsrv_srvnam;

SET @hisaweemlsrv_emlusr = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE HISAweEmlSrv MODIFY EmlUsr varchar(255) NULL', 'DO 0')
                            FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisaweemlsrv' AND LOWER(column_name) = 'emlusr'
                              AND character_maximum_length < 255);
PREPARE hisaweemlsrv_emlusr FROM @hisaweemlsrv_emlusr;
EXECUTE hisaweemlsrv_emlusr;
DEALLOCATE PREPARE hisaweemlsrv_emlusr;

SET @hisaweemlsrv_emlpwd = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE HISAweEmlSrv MODIFY EmlPwd varchar(512) NULL', 'DO 0')
                            FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisaweemlsrv' AND LOWER(column_name) = 'emlpwd'
                              AND character_maximum_length < 512);
PREPARE hisaweemlsrv_emlpwd FROM @hisaweemlsrv_emlpwd;
EXECUTE hisaweemlsrv_emlpwd;
DEALLOCATE PREPARE hisaweemlsrv_emlpwd;

SET @hisaweemlsrv_secmod = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE HISAweEmlSrv ADD COLUMN SecMod varchar(10) NULL', 'DO 0')
                            FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisaweemlsrv' AND LOWER(column_name) = 'secmod');
PREPARE hisaweemlsrv_secmod FROM @hisaweemlsrv_secmod;
EXECUTE hisaweemlsrv_secmod;
DEALLOCATE PREPARE hisaweemlsrv_secmod;

SET @hisaweemlsrv_oaureg = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE HISAweEmlSrv ADD COLUMN OauReg varchar(100) NULL', 'DO 0')
                            FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisaweemlsrv' AND LOWER(column_name) = 'oaureg');
PREPARE hisaweemlsrv_oaureg FROM @hisaweemlsrv_oaureg;
EXECUTE hisaweemlsrv_oaureg;
DEALLOCATE PREPARE hisaweemlsrv_oaureg;

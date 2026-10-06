--------------------------------------------------------
--  Unify FTP credentials into the scheduler server row
--  Carries the per-task launcher credentials over to the ftp server they
--  point at. The launcher credential columns (SrvUsr, SrvPwd) are kept in
--  AweSchTskLch and HISAweSchTskLch as deprecated, nullable columns: the
--  scheduler no longer reads or writes them.
--  Winner on conflict: the lowest AWESCHTSKLCH.IDE with a non-empty user.
--  Servers that already carry credentials are left untouched.
--------------------------------------------------------
UPDATE "AWESCHSRV"
SET "SSHUSR" = (SELECT "LCH"."SRVUSR"
                FROM "AWESCHTSKLCH" "LCH"
                WHERE "LCH"."IDSRV" = "AWESCHSRV"."IDE"
                  AND "LCH"."SRVUSR" IS NOT NULL
                  AND "LCH"."IDE" = (SELECT MIN("WIN"."IDE")
                                     FROM "AWESCHTSKLCH" "WIN"
                                     WHERE "WIN"."IDSRV" = "AWESCHSRV"."IDE"
                                       AND "WIN"."SRVUSR" IS NOT NULL)),
    "SSHPWD" = (SELECT "LCH"."SRVPWD"
                FROM "AWESCHTSKLCH" "LCH"
                WHERE "LCH"."IDSRV" = "AWESCHSRV"."IDE"
                  AND "LCH"."SRVUSR" IS NOT NULL
                  AND "LCH"."IDE" = (SELECT MIN("WIN"."IDE")
                                     FROM "AWESCHTSKLCH" "WIN"
                                     WHERE "WIN"."IDSRV" = "AWESCHSRV"."IDE"
                                       AND "WIN"."SRVUSR" IS NOT NULL))
WHERE LOWER("AWESCHSRV"."PRO") = 'ftp'
  AND "AWESCHSRV"."SSHUSR" IS NULL
  AND "AWESCHSRV"."SSHPWD" IS NULL
  AND EXISTS (SELECT 1
              FROM "AWESCHTSKLCH" "CHK"
              WHERE "CHK"."IDSRV" = "AWESCHSRV"."IDE"
                AND "CHK"."SRVUSR" IS NOT NULL);

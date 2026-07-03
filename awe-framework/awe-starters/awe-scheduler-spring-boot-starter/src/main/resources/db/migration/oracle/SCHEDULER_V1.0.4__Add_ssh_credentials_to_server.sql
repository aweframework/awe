--------------------------------------------------------
--  Add SSH credential columns to scheduler server table
--  Additive, nullable, backward-compatible (existing rows unaffected)
--------------------------------------------------------
ALTER TABLE "AWESCHSRV"
    ADD (
    "SSHUSR" VARCHAR2(200 BYTE),
    "SSHPWD" VARCHAR2(200 BYTE),
    "SSHKEY" VARCHAR2(4000 BYTE),
    "SSHKEYPASS" VARCHAR2(200 BYTE)
);

ALTER TABLE "HISAWESCHSRV"
    ADD (
    "SSHUSR" VARCHAR2(200 BYTE),
    "SSHPWD" VARCHAR2(200 BYTE),
    "SSHKEY" VARCHAR2(4000 BYTE),
    "SSHKEYPASS" VARCHAR2(200 BYTE)
);

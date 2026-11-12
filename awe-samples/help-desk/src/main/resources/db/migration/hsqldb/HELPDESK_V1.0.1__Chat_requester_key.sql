-- ------------------------------------------------------
--  Support chat: a visitor who only gives an email becomes a requester, so the requesters need a key sequence like the tickets
--  (the maintain targets read it through AweKey). It starts after the last requester of the demo data
-- ------------------------------------------------------
INSERT INTO AweKey (KeyNam, KeyVal) SELECT 'ReqKey', COALESCE(MAX(IdeReq), 0) FROM HdkReq;

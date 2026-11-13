-- ------------------------------------------------------
--  Default saved search of a window (#915)
--  AweUsrWinCnf gains the default saved search of the user for the window (DefSavSch, an AweSavSch id); its AdvOpn
--  becomes nullable, so a row stored only for the default search leaves the state of the advanced block unknown.
--  AweSavSch gains Def: 1 marks a search published for a profile as the default search of that profile (only a
--  published search can be one).
--  The script can be applied again: a column or a constraint is only added when it is missing, and making AdvOpn
--  nullable twice changes nothing.
-- ------------------------------------------------------
ALTER TABLE AweUsrWinCnf ADD COLUMN IF NOT EXISTS DefSavSch int NULL;  --  Default saved search of the user
ALTER TABLE AweUsrWinCnf ALTER COLUMN AdvOpn DROP NOT NULL;            --  Advanced block left open (1), closed (0) or unknown (null)

ALTER TABLE AweSavSch ADD COLUMN IF NOT EXISTS Def int DEFAULT 0 NOT NULL;  --  Default search of its profile (1) or not (0)

--  Only a search published for a profile is the default search of the profile
DO $$
BEGIN
  IF NOT EXISTS(SELECT * FROM pg_constraint WHERE LOWER(conname) = 'ck_awesavsch_def') THEN
    ALTER TABLE AweSavSch ADD CONSTRAINT ck_AweSavSch_Def CHECK (Def = 0 OR (Def = 1 AND Pro IS NOT NULL));
  END IF;
END
$$;

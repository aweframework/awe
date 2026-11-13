-- ------------------------------------------------------
--  Default saved search of a window (#915)
--  AweUsrWinCnf gains the default saved search of the user for the window (DefSavSch, an AweSavSch id); its AdvOpn
--  becomes nullable, so a row stored only for the default search leaves the state of the advanced block unknown.
--  AweSavSch gains Def: 1 marks a search published for a profile as the default search of that profile (only a
--  published search can be one).
--  The script can be applied again: a column or a constraint is only added when it is missing, and making AdvOpn
--  nullable twice changes nothing.
-- ------------------------------------------------------
--  Default saved search of the user
SET @add_awewincnf_defsavsch = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweUsrWinCnf ADD COLUMN DefSavSch int NULL', 'DO 0')
                                FROM information_schema.columns
                                WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweusrwincnf'
                                  AND LOWER(column_name) = 'defsavsch');
PREPARE add_awewincnf_defsavsch FROM @add_awewincnf_defsavsch;
EXECUTE add_awewincnf_defsavsch;
DEALLOCATE PREPARE add_awewincnf_defsavsch;

--  Advanced block left open (1), closed (0) or unknown (null)
ALTER TABLE AweUsrWinCnf MODIFY AdvOpn int NULL;

--  Default search of its profile (1) or not (0)
SET @add_awesavsch_def = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweSavSch ADD COLUMN Def int DEFAULT 0 NOT NULL', 'DO 0')
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND LOWER(table_name) = 'awesavsch'
                            AND LOWER(column_name) = 'def');
PREPARE add_awesavsch_def FROM @add_awesavsch_def;
EXECUTE add_awesavsch_def;
DEALLOCATE PREPARE add_awesavsch_def;

--  Only a search published for a profile is the default search of the profile
SET @add_ck_awesavsch_def = (SELECT IF(COUNT(*) = 0,
                               'ALTER TABLE AweSavSch ADD CONSTRAINT ck_AweSavSch_Def CHECK (Def = 0 OR (Def = 1 AND Pro IS NOT NULL))',
                               'DO 0')
                             FROM information_schema.table_constraints
                             WHERE constraint_schema = DATABASE() AND LOWER(table_name) = 'awesavsch'
                               AND LOWER(constraint_name) = 'ck_awesavsch_def');
PREPARE add_ck_awesavsch_def FROM @add_ck_awesavsch_def;
EXECUTE add_ck_awesavsch_def;
DEALLOCATE PREPARE add_ck_awesavsch_def;

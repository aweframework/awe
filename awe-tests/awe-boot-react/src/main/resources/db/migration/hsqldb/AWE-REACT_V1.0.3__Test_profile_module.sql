-- The Test profile (TST, id 4) reaches the Test module (id 1) of V1.0.0, which holds the test menu: a user of that profile
-- opens the screens of the test menu without reaching the tools, as the user that applies the saved searches published
-- for its profile does (#790, AdvancedSearchTestsIT). It can be applied again: the row is only added when it is not there
INSERT INTO AweModPro (IdeModPro, IdeMod, IdePro, Ord)
SELECT KeyVal, 1, 4, NULL FROM AweKey
WHERE KeyNam = 'ModProKey'
  AND NOT EXISTS (SELECT 1 FROM AweModPro WHERE IdeMod = 1 AND IdePro = 4);

-- Update ModProKey
UPDATE AweKey SET KeyVal = KeyVal + 1 WHERE KeyNam = 'ModProKey';

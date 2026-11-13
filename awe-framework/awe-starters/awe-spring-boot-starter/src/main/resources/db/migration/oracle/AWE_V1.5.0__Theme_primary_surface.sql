--------------------------------------------------------
--  AWE 5 theme model (#885)
--  Themes gain their primary colour (palette name or hex), surface (slate|gray|zinc|neutral|stone|auto or hex) and
--  mode (light|dark|system); users gain their density (comfortable|compact, null for the application density).
--  AweThmCol is kept as it is (unused) and will be dropped in a later version.
--  The script can be applied again: a column is only added when it is missing and the data only fills the empty
--  values, so a value already set is never overwritten.
--------------------------------------------------------
DECLARE
  PROCEDURE add_column(table_name VARCHAR2, column_name VARCHAR2, column_type VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ' || table_name || ' ADD (' || column_name || ' ' || column_type || ' NULL)';
  EXCEPTION
    WHEN OTHERS THEN
      --  ORA-01430: the column already exists
      IF SQLCODE != -1430 THEN
        RAISE;
      END IF;
  END;
BEGIN
  add_column('AweThm', 'Pri', 'varchar2(20)');  --  Primary colour
  add_column('AweThm', 'Srf', 'varchar2(20)');  --  Surface
  add_column('AweThm', 'Mde', 'varchar2(10)');  --  Mode
  add_column('HISAweThm', 'Pri', 'varchar2(20)');
  add_column('HISAweThm', 'Srf', 'varchar2(20)');
  add_column('HISAweThm', 'Mde', 'varchar2(10)');
  add_column('AweUsrSet', 'density', 'varchar2(20)');  --  Density
END;
/

--  Themes with a light colour set keep its primary colour when it is a hex (#RGB or #RRGGBB), as #RRGGBB in upper
--  case: a short #RGB is expanded (#abc -> #AABBCC)
UPDATE AweThm
SET Pri = (SELECT CASE WHEN LENGTH(colors.primaryColor) = 4
                  THEN UPPER('#' || SUBSTR(colors.primaryColor, 2, 1) || SUBSTR(colors.primaryColor, 2, 1)
                                || SUBSTR(colors.primaryColor, 3, 1) || SUBSTR(colors.primaryColor, 3, 1)
                                || SUBSTR(colors.primaryColor, 4, 1) || SUBSTR(colors.primaryColor, 4, 1))
                  ELSE UPPER(colors.primaryColor) END
           FROM AweThmCol colors
           WHERE colors.theme = AweThm.Nam AND colors.dark = 0)
WHERE Pri IS NULL
  AND EXISTS(SELECT 1
             FROM AweThmCol colors
             WHERE colors.theme = AweThm.Nam AND colors.dark = 0
               AND REGEXP_LIKE(colors.primaryColor, '^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$'));

--  Classic themes take the preset of their name: the AWE 5 palette nearest to their brand colour
UPDATE AweThm SET Pri = COALESCE(Pri, 'sky'),     Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'default';
UPDATE AweThm SET Pri = COALESCE(Pri, 'teal'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'asphalt';
UPDATE AweThm SET Pri = COALESCE(Pri, 'violet'),  Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'purple-hills';
UPDATE AweThm SET Pri = COALESCE(Pri, '#948787'), Srf = COALESCE(Srf, 'stone')  WHERE Nam = 'dust';
UPDATE AweThm SET Pri = COALESCE(Pri, 'cyan'),    Srf = COALESCE(Srf, 'slate')  WHERE Nam = 'frost';
UPDATE AweThm SET Pri = COALESCE(Pri, 'lime'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'fresh';
UPDATE AweThm SET Pri = COALESCE(Pri, 'cyan'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'silver';
UPDATE AweThm SET Pri = COALESCE(Pri, 'emerald'), Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'clean';
UPDATE AweThm SET Pri = COALESCE(Pri, 'cyan'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'white';
UPDATE AweThm SET Pri = COALESCE(Pri, 'blue'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'adminflare';
UPDATE AweThm SET Pri = COALESCE(Pri, 'red'),     Srf = COALESCE(Srf, 'stone')  WHERE Nam = 'orange';
UPDATE AweThm SET Pri = COALESCE(Pri, 'red'),     Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'sunset';
UPDATE AweThm SET Pri = COALESCE(Pri, 'cyan'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'grass';
UPDATE AweThm SET Pri = COALESCE(Pri, 'blue'),    Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'sky';
UPDATE AweThm SET Pri = COALESCE(Pri, 'sky'),     Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'eclipse';
UPDATE AweThm SET Pri = COALESCE(Pri, 'yellow'),  Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'sunny';
UPDATE AweThm SET Pri = COALESCE(Pri, 'blue'),    Srf = COALESCE(Srf, 'slate')  WHERE Nam = 'sea';
UPDATE AweThm SET Pri = COALESCE(Pri, 'red'),     Srf = COALESCE(Srf, 'auto')   WHERE Nam = 'ferrari';
UPDATE AweThm SET Pri = COALESCE(Pri, '#948787'), Srf = COALESCE(Srf, 'stone')  WHERE Nam = 'amazonia';

--  The other themes take the defaults
UPDATE AweThm SET Pri = 'emerald' WHERE Pri IS NULL;
UPDATE AweThm SET Srf = 'auto' WHERE Srf IS NULL;
UPDATE AweThm SET Mde = 'system' WHERE Mde IS NULL;

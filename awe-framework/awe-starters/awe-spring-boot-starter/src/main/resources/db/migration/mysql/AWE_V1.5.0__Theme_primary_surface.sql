-- ------------------------------------------------------
--  AWE 5 theme model (#885)
--  Themes gain their primary colour (palette name or hex), surface (slate|gray|zinc|neutral|stone|auto or hex) and
--  mode (light|dark|system); users gain their density (comfortable|compact, null for the application density).
--  AweThmCol is kept as it is (unused) and will be dropped in a later version.
--  The script can be applied again: a column is only added when it is missing and the data only fills the empty
--  values, so a value already set is never overwritten.
-- ------------------------------------------------------
--  Primary colour
SET @add_awethm_pri = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweThm ADD COLUMN Pri varchar(20) NULL', 'DO 0')
                       FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND LOWER(table_name) = 'awethm'
                         AND LOWER(column_name) = 'pri');
PREPARE add_awethm_pri FROM @add_awethm_pri;
EXECUTE add_awethm_pri;
DEALLOCATE PREPARE add_awethm_pri;

--  Surface
SET @add_awethm_srf = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweThm ADD COLUMN Srf varchar(20) NULL', 'DO 0')
                       FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND LOWER(table_name) = 'awethm'
                         AND LOWER(column_name) = 'srf');
PREPARE add_awethm_srf FROM @add_awethm_srf;
EXECUTE add_awethm_srf;
DEALLOCATE PREPARE add_awethm_srf;

--  Mode
SET @add_awethm_mde = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweThm ADD COLUMN Mde varchar(10) NULL', 'DO 0')
                       FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND LOWER(table_name) = 'awethm'
                         AND LOWER(column_name) = 'mde');
PREPARE add_awethm_mde FROM @add_awethm_mde;
EXECUTE add_awethm_mde;
DEALLOCATE PREPARE add_awethm_mde;

SET @add_hisawethm_pri = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE HISAweThm ADD COLUMN Pri varchar(20) NULL', 'DO 0')
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisawethm'
                            AND LOWER(column_name) = 'pri');
PREPARE add_hisawethm_pri FROM @add_hisawethm_pri;
EXECUTE add_hisawethm_pri;
DEALLOCATE PREPARE add_hisawethm_pri;

SET @add_hisawethm_srf = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE HISAweThm ADD COLUMN Srf varchar(20) NULL', 'DO 0')
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisawethm'
                            AND LOWER(column_name) = 'srf');
PREPARE add_hisawethm_srf FROM @add_hisawethm_srf;
EXECUTE add_hisawethm_srf;
DEALLOCATE PREPARE add_hisawethm_srf;

SET @add_hisawethm_mde = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE HISAweThm ADD COLUMN Mde varchar(10) NULL', 'DO 0')
                          FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND LOWER(table_name) = 'hisawethm'
                            AND LOWER(column_name) = 'mde');
PREPARE add_hisawethm_mde FROM @add_hisawethm_mde;
EXECUTE add_hisawethm_mde;
DEALLOCATE PREPARE add_hisawethm_mde;

--  Density
SET @add_density = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE AweUsrSet ADD COLUMN density varchar(20) NULL', 'DO 0')
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND LOWER(table_name) = 'aweusrset'
                      AND LOWER(column_name) = 'density');
PREPARE add_density FROM @add_density;
EXECUTE add_density;
DEALLOCATE PREPARE add_density;

--  Themes with a light colour set keep its primary colour when it is a hex (#RGB or #RRGGBB), as #RRGGBB in upper
--  case: a short #RGB is expanded (#abc -> #AABBCC)
UPDATE AweThm
SET Pri = (SELECT CASE WHEN CHAR_LENGTH(colors.primaryColor) = 4
                  THEN UPPER(CONCAT('#', SUBSTRING(colors.primaryColor, 2, 1), SUBSTRING(colors.primaryColor, 2, 1),
                                   SUBSTRING(colors.primaryColor, 3, 1), SUBSTRING(colors.primaryColor, 3, 1),
                                   SUBSTRING(colors.primaryColor, 4, 1), SUBSTRING(colors.primaryColor, 4, 1)))
                  ELSE UPPER(colors.primaryColor) END
           FROM AweThmCol colors
           WHERE colors.theme = AweThm.Nam AND colors.dark = 0)
WHERE Pri IS NULL
  AND EXISTS(SELECT 1
             FROM AweThmCol colors
             WHERE colors.theme = AweThm.Nam AND colors.dark = 0
               AND colors.primaryColor REGEXP '^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$');

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

-- ------------------------------------------------------
--  DDL for Table AweThmCol
--  Themes table: List of available themes
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS AweThmCol (
    IdeThmCol INT NOT NULL PRIMARY KEY, -- Theme colorset key
    theme varchar(100) not NULL,                                -- Theme name
    dark   INT DEFAULT 0 NOT NULL,                              -- Is dark theme
    themeColor VARCHAR(20) NULL,
    textColor VARCHAR(20) NULL,
    primaryColor VARCHAR(20) NULL,
    secondaryColor VARCHAR(20) NULL,
    primaryTextColor VARCHAR(20) NULL,
    secondaryTextColor VARCHAR(20) NULL,
    primaryBackgroundColor VARCHAR(20) NULL,
    secondaryBackgroundColor VARCHAR(20) NULL,
    primaryMenuColor VARCHAR(20) NULL,
    secondaryMenuColor VARCHAR(20) NULL,
    menuTextColor VARCHAR(20) NULL,
    primaryNavbarColor VARCHAR(20) NULL,
    secondaryNavbarColor VARCHAR(20) NULL,
    navbarTextColor VARCHAR(20) NULL,
    navbarDropdownColor VARCHAR(20) NULL,
    primaryFontSize VARCHAR(20) NULL,
    secondaryFontSize VARCHAR(20) NULL,
    disabledColor VARCHAR(20) NULL,
    disabledBorderColor VARCHAR(20) NULL,
    disabledTextColor VARCHAR(20) NULL,
    headerHeight VARCHAR(20) NULL,
    panelBackgroundColor VARCHAR(20) NULL,
    panelBorderColor VARCHAR(20) NULL,
    panelTextColor VARCHAR(20) NULL,
    panelHeaderColor VARCHAR(20) NULL,
    gridBorderColor VARCHAR(20) NULL,
    gridHeaderColor VARCHAR(20) NULL,
    dangerColor VARCHAR(20) NULL,
    warningColor VARCHAR(20) NULL,
    successColor VARCHAR(20) NULL,
    infoColor VARCHAR(20) NULL,
    borderColor VARCHAR(20) NULL,
    borderRadius VARCHAR(20) NULL,
    loadingBarColor VARCHAR(20) NULL,
    requiredColor VARCHAR(20) NULL
    );
CREATE UNIQUE INDEX AweThmColI1 ON AweThmCol (theme, dark);

CREATE TABLE IF NOT EXISTS AweUsrSet (
    userName  varchar(20) PRIMARY KEY NOT NULL, -- User name
    themeMode varchar(20) NULL                  -- User mode
);
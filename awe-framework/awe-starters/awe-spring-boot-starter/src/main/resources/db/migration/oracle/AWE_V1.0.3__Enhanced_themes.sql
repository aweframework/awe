--------------------------------------------------------
--  DDL for Table AweThmCol
--  Themes table: List of available themes
--------------------------------------------------------
CREATE TABLE AweThmCol (
    IdeThmCol int CONSTRAINT pk_AweThmCol PRIMARY KEY NOT NULL, -- Theme colorset key
    theme VARCHAR2(100) not NULL,                               -- Theme name
    dark   INT DEFAULT 0 NOT NULL,                              -- Is dark theme
    themeColor VARCHAR2(20) NULL,
    textColor VARCHAR2(20) NULL,
    primaryColor VARCHAR2(20) NULL,
    secondaryColor VARCHAR2(20) NULL,
    primaryTextColor VARCHAR2(20) NULL,
    secondaryTextColor VARCHAR2(20) NULL,
    primaryBackgroundColor VARCHAR2(20) NULL,
    secondaryBackgroundColor VARCHAR2(20) NULL,
    primaryMenuColor VARCHAR2(20) NULL,
    secondaryMenuColor VARCHAR2(20) NULL,
    menuTextColor VARCHAR2(20) NULL,
    primaryNavbarColor VARCHAR2(20) NULL,
    secondaryNavbarColor VARCHAR2(20) NULL,
    navbarTextColor VARCHAR2(20) NULL,
    navbarDropdownColor VARCHAR2(20) NULL,
    primaryFontSize VARCHAR2(20) NULL,
    secondaryFontSize VARCHAR2(20) NULL,
    disabledColor VARCHAR2(20) NULL,
    disabledBorderColor VARCHAR2(20) NULL,
    disabledTextColor VARCHAR2(20) NULL,
    headerHeight VARCHAR2(20) NULL,
    panelBackgroundColor VARCHAR2(20) NULL,
    panelBorderColor VARCHAR2(20) NULL,
    panelTextColor VARCHAR2(20) NULL,
    panelHeaderColor VARCHAR2(20) NULL,
    gridBorderColor VARCHAR2(20) NULL,
    gridHeaderColor VARCHAR2(20) NULL,
    dangerColor VARCHAR2(20) NULL,
    warningColor VARCHAR2(20) NULL,
    successColor VARCHAR2(20) NULL,
    infoColor VARCHAR2(20) NULL,
    borderColor VARCHAR2(20) NULL,
    borderRadius VARCHAR2(20) NULL,
    loadingBarColor VARCHAR2(20) NULL,
    requiredColor VARCHAR2(20) NULL
    );
CREATE UNIQUE INDEX AweThmColI1 ON AweThmCol (theme, dark);

CREATE TABLE AweUsrSet (
    userName  varchar2(20) CONSTRAINT pk_AweUsrSet PRIMARY KEY NOT NULL, -- User name
    themeMode varchar2(20) NULL                                          -- User mode
);
---
id: developer-tools
title: DevTools Module
sidebar_label: DevTools Module
---

This module has some very useful management tools to manage server resources:
- [File manager](#file-manager)
- [SQL extractor](#sql-extractor)

To use this module, follow these steps:

- Add **awe tools dependencies** to pom.xml descriptor.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-tools-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Configure property value to add `awe-tools` to module list.

```properties
awe.application.module-list = APP, ..., awe-tools, ..., awe
```

## `SQL Extractor`
SQL Extractor is a tool to execute SQL queries directly over the application database

<img alt="SQL Extractor" src={require('@docusaurus/useBaseUrl').default('img/SQL_Extractor.png')} />


- Add the tools module screens into your `private.xml` file:

```xml
<option name="sql-extractor" label="MENU_SQL_EXTRACTOR" screen="sqlExtractor" icon="database"/>
```

### Run SQL queries with the database in use.

"SQL Extractor can only execute queries. If you want to insert, update or delete call AWE team"


### Save query strings in files.

Choose file name without extension. SQL Extractor will save it in .txt for you. We can only save a query per file. This file will be automatically created in your home directory. "c:/users/{user}/aweFiles/"

<img alt="save_query" src={require('@docusaurus/useBaseUrl').default('img/save_query.png')} />

### Load querie strings from file to relaunch it.

Load files from your home directory "c:/users/{user}/aweFiles/"

<img alt="load_query" src={require('@docusaurus/useBaseUrl').default('img/load_query.png')} />

### READ MODE AND WRITE MODE

By default sqlExtractor screen is in "Read" mode so you can only execute "select" statements. There is a hidden parameter in the screen called "sqlType" that has by default "R", for "Read Mode".

&lt;criteria id="sqlType" component="hidden" value="R" /&gt;


In order to allow some users or profiles to execute statements to modify the database (insert, delete, drop, update) you must change the "sqlType" hidden parameter value to "W" (Write mode) for the sqlExtractor screen in the Screen configuration option.

:::tip

If you want to change the functionality you must overwrite the service as follows:

```XML
  <service id="selectExtract">
    <java classname="com.almis.{project}.services.controller.SqlExtractorController" method="extractData" >
      <service-parameter name="select"   type="STRING"/>
    </java>
  </service>
```

And add your own SqlExtractorController.java and SqlExtractorManager.java thats overwrite the AWE methods.
:::

### Example:

They must start with select(1)

```sql
select(1) INSERT INTO x (columns,...) VALUES (y,...)
```

## `File Manager`

With the file manager you can move, copy, rename, remove, zip, unzip, download and upload files... 

<img alt="filemanager" src={require('@docusaurus/useBaseUrl').default('img/filemanager.gif')} />

To use this tool, follow these steps (after adding the dependency):


- Add the tools module screens into your `private.xml` file:

```xml
<option name="file-manager" label="MENU_TEST_FILEMANAGER" screen="filemanager-test" icon="folder"/>
```

- Create **new screen** with the widget awe-file-manager like `filemanager.xml`
```xml
<screen template="full" label="MENU_TEST_FILEMANAGER"
        xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
        xsi:noNamespaceSchemaLocation='../../sch/awe/screen.xsd'>
  <tag source="center">
      <widget type="awe-file-manager" id="file-manager" style="expand"/>
  </tag>
</screen>
```

- Add this screen as **new menu option** with your own locale and the option file-manager
```xml
    <option name="filemanager-test" label="MENU_TEST_FILEMANAGER" screen="filemanager-test" icon="folder" />
    <option name="file-manager" screen="file-manager" invisible="true"/>
```
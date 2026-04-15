---
name: awe-framework
description: |
  Guides AI agents through AWE (Almis Web Engine) framework development:
  XML descriptor authoring (screens, queries, maintain, services, actions),
  Java service patterns, project structure, and build workflows.
  Trigger: When working with AWE, Almis Web Engine, XML descriptors,
  awe-framework, screen XML, query XML, maintain XML, or ServiceConfig.
version: "1.0.0"
author: AWE Team
license: Apache-2.0
---

# AWE Framework — Agent Skill

AWE (Almis Web Engine) is a Java/Spring Boot web application framework where UI, data access,
and business logic are defined through XML descriptors rather than handwritten code. This skill
teaches you the descriptor system, project layout, Java service patterns, and development workflow.

## When to Use This Skill

| Trigger | Example |
|---------|---------|
| Creating or modifying a screen | "Add a new user management screen" |
| Writing or editing a query | "Create a query that joins users and profiles" |
| Defining maintain operations | "Add insert/update/delete for the orders table" |
| Wiring a Java service | "Expose a service method through XML" |
| Defining server actions | "Create an action that calls a maintain target" |
| Understanding project structure | "Where do I put screen XML files?" |
| Build or test commands | "How do I build and run the application?" |
| Adding UI dependencies | "Show/hide a field based on a dropdown value" |
| CRUD workflow (end-to-end) | "Create a full CRUD screen for customers" |

## AWE Project Structure

### AWE project archetype
AWE projects follow a standard multi-module Maven structure.

- AWE with ReactJS frontend: `awe-boot-react-archetype`
    ```bash
    mvn -B archetype:generate \
     -DarchetypeGroupId=com.almis.awe \
     -DarchetypeArtifactId=awe-boot-react-archetype \
     -DarchetypeVersion=[Archetype version] \
     -DgroupId=com.mycompany.app \
     -DartifactId=my-app \
     -Dversion=1.0-SNAPSHOT 
    ```
- AWE with AngularJS frontend: `awe-boot-angular-archetype`
    ```bash
    mvn -B archetype:generate \
     -DarchetypeGroupId=com.almis.awe \
     -DarchetypeArtifactId=awe-boot-angular-archetype \
     -DarchetypeVersion=[Archetype version] \
     -DgroupId=com.mycompany.app \
     -DartifactId=my-app \
     -Dversion=1.0-SNAPSHOT 
    ```

### Top-Level Modules

```
awe/
  pom.xml                    # Parent POM
  awe-framework/             # Core framework (10 sub-modules)
    awe-dependencies/        # BOM — centralizes dependency versions
    awe-model/               # Domain model, DTOs (ServiceData, DataList)
    awe-generic-screens/     # Built-in screens and global XML descriptors
    awe-client-angular/      # AngularJS 1.5.8 frontend
    awe-controller/          # Spring MVC controllers and services
    awe-testing/             # Test utilities and base test classes
    awe-modules/             # Optional feature modules
      awe-scheduler/         # Task scheduling module
      awe-notifier/          # Email/notification module
      awe-developer/         # Developer tools module
      awe-builder/           # Application builder module
    awe-starters/            # Spring Boot auto-configuration starters
    awe-boot-angular-archetype/  # Maven archetype for Angular apps
    awe-boot-react-archetype/    # Maven archetype for React apps
  awe-samples/               # Example applications
  awe-tests/                 # Integration and E2E tests
```

### XML Descriptor Folder Convention

Every AWE module places its descriptors under:

```
src/main/resources/application/{acronym}/
```

where `{acronym}` is the module identifier (e.g. `awe`, `awe-scheduler`). Inside that folder:

| Folder | Purpose | Key Files |
|--------|---------|-----------|
| `global/` | App-wide definitions | `Queries.xml`, `Maintain.xml`, `Services.xml`, `Actions.xml`, `Email.xml`, `Enumerated.xml`, `Queues.xml` |
| `screen/` | Screen definitions | One `.xml` file per screen (e.g. `scheduler-tasks.xml`) |
| `locale/` | Internationalization | `Locale-{lang}.xml` files with `<local>` entries |
| `menu/` | Navigation menus | `public.xml`, `private.xml` with `<option>` trees |
| `profile/` | Access profiles | `profile.xml` defining `<profile>` restrictions |

**Rule**: Global descriptors (queries, maintain, services, actions) go in `global/`.
Screen descriptors go in `screen/`. Never mix them.

**Path example** for the scheduler module:
```
awe-framework/awe-modules/awe-scheduler/
  src/main/resources/application/awe-scheduler/
    global/
      Queries.xml
      Maintain.xml
      Services.xml
    screen/
      scheduler-tasks.xml
      new-scheduler-task.xml
```

## XML Descriptor System

AWE uses five interconnected descriptor types. The wiring flow is:

```
Screen (UI)  --->  Query (read data)    --->  Service (Java)
             --->  Maintain (write data) --->  Service (Java)
             --->  Action (server call)  --->  Service (Java)
```

A screen's `<grid>` loads data via a query. Buttons trigger `<button-action>` chains that call
maintain targets or server actions. Services wire XML to Java methods.

### Screen Descriptors

Screen XML defines the visual layout: criteria (filters), grids (tables), buttons, and their
interactions. Each screen file lives in the `screen/` folder.

**Root element**: `<screen template="..." label="..." help="...">`

**Key elements**:

| Element | Purpose | Container |
|---------|---------|-----------|
| `<tag source="hidden">` | Hidden components: messages, hidden criteria | `<screen>` |
| `<tag source="buttons">` | Toolbar buttons | `<screen>` |
| `<tag source="center">` | Main content area | `<screen>` |
| `<tag source="modal">` | Modal dialogs (via `<include>`) | `<screen>` |
| `<window>` | Visual panel with title/icon | `<tag>` |
| `<criteria>` | Input filter (text, select, suggest, date, etc.) | `<window>` or `<tag>` |
| `<grid>` | Data table bound to a query | `<window>` |
| `<column>` | Column definition in a grid | `<grid>` |
| `<button>` | Action trigger with `<button-action>` chain | `<tag>` or `<window>` |
| `<message>` | Confirmation dialog definition | `<tag source="hidden">` |
| `<dependency>` | Dynamic UI behavior (show/hide/enable/disable/filter) | Any element |

**Example** — Task list screen with filters, grid, and toolbar buttons
(from `awe-scheduler/screen/scheduler-tasks.xml`):

```xml
<screen template="window" label="MENU_SCHEDULER_TASKS"
        help="HELP_SCREEN_SCHEDULER_TASKS">
  <!-- Hidden: confirmation messages -->
  <tag source="hidden">
    <message id="DelMsg" title="CONFIRM_TITLE_DELETE"
             message="CONFIRM_MESSAGE_DELETE_TASK" />
  </tag>

  <!-- Toolbar buttons -->
  <tag source="buttons">
    <button label="BUTTON_NEW" icon="plus-circle" id="ButNew">
      <button-action type="screen" target="new-scheduler-task" />
    </button>
    <button label="BUTTON_DELETE" icon="trash" id="ButDel">
      <button-action type="check-some-selected" target="GrdTskLst" />
      <button-action type="confirm" target="DelMsg" />
      <button-action type="server" server-action="maintain"
                     target-action="DeleteSchedulerTask" />
      <button-action type="filter" target="GrdTskLst" />
      <dependency target-type="disable" initial="true">
        <dependency-element id="GrdTskLst" condition="lt" value="1" />
      </dependency>
    </button>
  </tag>

  <!-- Main content -->
  <tag source="center">
    <!-- Filter criteria -->
    <window label="SCREEN_TEXT_FILTERS" icon="filter" style="static criteria">
      <tag id="FilterArea" type="div" style="panel-body static">
        <criteria label="PARAMETER_TASK" component="suggest" id="CrtTsk"
                  server-action="data" target-action="launchSchedulerTaskSuggest"
                  style="col-xs-12 col-sm-6 col-lg-4" />
        <criteria label="PARAMETER_ACTIVE" component="select" id="CrtAct"
                  initial-load="enum" target-action="Es1Es0"
                  style="col-xs-12 col-sm-2 col-lg-2" optional="true" />
      </tag>
      <tag type="div" style="panel-footer">
        <tag type="div" style="pull-right">
          <button button-type="submit" label="BUTTON_SEARCH" icon="search"
                  id="ButSch">
            <button-action type="filter" target="GrdTskLst" />
          </button>
        </tag>
      </tag>
    </window>

    <!-- Data grid -->
    <window label="SCREEN_TEXT_TASKS" icon="list" style="expand">
      <grid id="GrdTskLst" server-action="data"
            target-action="getSchedulerTaskList" initial-load="query"
            style="expand">
        <column label="#" name="IdeTsk" align="center" charlength="6" />
        <column label="PARAMETER_TASK" name="Nam" align="left"
                charlength="20" />
        <column label="PARAMETER_ACTIVE" name="ActIco" component="icon"
                align="center" charlength="8" />
      </grid>
    </window>
  </tag>
</screen>
```

**Button-action types reference**:

| Type | Purpose | Key Attributes |
|------|---------|----------------|
| `screen` | Navigate to another screen | `target` = screen name |
| `dialog` | Open a modal dialog | `target` = dialog id |
| `server` | Call a server action | `server-action` = `maintain` or `maintain-silent`, `target-action` = maintain target name |
| `filter` | Reload/filter a grid | `target` = grid id |
| `validate` | Run form validation | — |
| `confirm` | Show confirmation dialog | `target` = message id |
| `check-one-selected` | Require exactly one grid row selected | `target` = grid id |
| `check-some-selected` | Require at least one grid row selected | `target` = grid id |
| `reset` | Reset form fields | `target` = container id |

**Criteria component types**:

| Component | Description |
|-----------|-------------|
| `text` | Free text input (default) |
| `textarea` | Multi-line text |
| `select` | Dropdown (loads from enum or query) |
| `suggest` | Auto-complete (server-backed) |
| `date` | Date picker |
| `time` | Time picker |
| `numeric` | Number input |
| `checkbox` | Boolean toggle |
| `radio` | Radio button group |
| `hidden` | Hidden field |

### Query Descriptors

Query XML defines how data is fetched — either from a database table or from a Java service.
All queries live in `global/Queries.xml`.

**Root element**: `<queries>`

**Key elements**:

| Element | Purpose |
|---------|---------|
| `<query id="..." service="...">` | Service-backed query (calls a `<service>`) |
| `<query id="..." distinct="true">` | Database query with optional `distinct` |
| `<table id="..." alias="...">` | FROM clause table reference |
| `<field id="..." alias="...">` | SELECT column (optionally with `transform`, `translate`) |
| `<join type="LEFT\|INNER\|RIGHT">` | JOIN clause with nested `<table>` and `<filter>` |
| `<where>` / `<and>` / `<or>` | WHERE clause filters |
| `<filter>` | Condition: `left-field` + `condition` + `right-variable` or `right-field` |
| `<variable>` | Parameter binding (from criteria, session, or fixed value) |
| `<order-by>` | ORDER BY clause |
| `<computed>` | Calculated/formatted field |
| `<compound>` | Composite field combining multiple computed values |

**Example 1** — Service-backed query (delegates to Java):

```xml
<query id="SysDat" service="SysDat">
  <field transform="DATE" id="value" alias="value" />
</query>
```

**Example 2** — Database query with join and filtering:

```xml
<query id="getAppRoles" distinct="true" public="true">
  <table id="ope" alias="ope" />
  <field id="Acr" alias="role" table="AwePro" />
  <join type="LEFT">
    <table id="AwePro" alias="AwePro" />
    <and>
      <filter condition="eq" left-field="IdePro" left-table="ope"
              right-field="IdePro" right-table="AwePro" />
    </and>
  </join>
</query>
```

**Example 3** — Database query with WHERE clause and variables:

```xml
<query id="getApplicationParameters">
  <table id="AweAppPar" />
  <field id="IdeAweAppPar" alias="identifier" />
  <field id="ParNam" alias="name" />
  <field id="ParVal" alias="value" />
  <field id="Cat" alias="category" translate="CatParameterType" />
  <field id="Des" alias="description" />
  <where>
    <and>
      <filter left-field="Act" condition="eq" right-variable="ActFlag" />
    </and>
  </where>
  <variable id="ActFlag" type="INTEGER" value="1" />
</query>
```

**Example 4** — Query with compound and computed fields:

```xml
<query id="getLogList" service="getLogList">
  <field id="name" alias="Nam" />
  <field id="date" alias="dateMs" transform="DATE_MS" />
  <field id="date" alias="dateTimestamp" transform="TIMESTAMP" />
  <compound alias="Dat">
    <computed alias="value" format="[dateMs]" />
    <computed alias="label" format="[dateTimestamp]" />
  </compound>
  <order-by field="dateMs" type="DESC" />
  <variable id="file" type="STRINGN" name="CrtFil" />
</query>
```

**Filter condition reference**:

| Condition | SQL Equivalent |
|-----------|---------------|
| `eq` | `=` |
| `ne` | `!=` |
| `gt` | `>` |
| `ge` | `>=` |
| `lt` | `<` |
| `le` | `<=` |
| `like` | `LIKE` (add `ignorecase="true"` for case-insensitive) |
| `in` | `IN (...)` |
| `is null` | `IS NULL` |
| `is not null` | `IS NOT NULL` |

**Variable type reference**:

| Type | Description |
|------|-------------|
| `STRING` | Text value |
| `STRINGN` | Nullable text |
| `STRINGB` | Text with `%` wildcards (for LIKE) |
| `INTEGER` | Integer number |
| `FLOAT` | Decimal number |
| `DATE` | Date value |
| `OBJECT` | Generic object |
| `STRING_HASH_RIPEMD160` | Hashed text (RIPEMD-160) |
| `STRING_HASH_SHA` | Hashed text (SHA) |

### Maintain Descriptors

Maintain XML defines write operations: insert, update, delete, or delegate to a Java service.
All maintain targets live in `global/Maintain.xml`.

**Root element**: `<maintain>`

**Key elements**:

| Element | Purpose |
|---------|---------|
| `<target name="...">` | Named maintain operation (referenced by actions/buttons) |
| `<serve service="...">` | Delegate to a Java service instead of direct SQL |
| `<insert>` | INSERT operation with `<table>`, `<field>`, `<variable>` |
| `<update>` | UPDATE operation with `<table>`, `<field>`, `<where>`, `<variable>` |
| `<delete>` | DELETE operation with `<table>`, `<where>`, `<variable>` |
| `<multiple>` | Batch operation: processes multiple rows from a grid |
| `<field>` | Column to set — `id` = column, `variable` = value source |
| `<variable>` | Parameter binding — `name` = criteria id or literal `value` |

**Example 1** — Service-delegated maintain (login):

```xml
<target name="login" public="true">
  <serve service="loginMaintain" />
</target>
```

**Example 2** — Database insert with sequence and variables:

```xml
<target name="UsrNew">
  <insert>
    <table id="ope" />
    <field id="IdeOpe" sequence="OpeKey" variable="IdeOpe" />
    <field id="l1_nom" variable="Usr" />
    <field id="l1_pas" variable="Pas" />
    <field id="l1_act" variable="Sta" />
    <field id="EmlAdr" variable="Eml" />
    <field id="l1_lan" variable="Lan" />
    <field id="OpeNam" variable="Nam" />
    <field id="IdePro" variable="Pro" />
    <variable id="IdeOpe" type="INTEGER" name="IdeOpe" />
    <variable id="Usr" type="STRING" name="Usr" />
    <variable id="Pas" type="STRING_HASH_RIPEMD160" name="Pas" />
    <variable id="Sta" type="INTEGER" name="Sta" />
    <variable id="Eml" type="STRING" name="Eml" />
    <variable id="Lan" type="STRING" name="Lan" />
    <variable id="Nam" type="STRING" name="Nam" />
    <variable id="Pro" type="INTEGER" name="Pro" />
  </insert>
</target>
```

**Example 3** — Database update with WHERE clause:

```xml
<target name="UpdateOauthUserProfile">
  <update>
    <table id="ope" />
    <field id="IdePro" query="GetProfileId" />
    <where>
      <and>
        <filter left-field="l1_nom" condition="eq"
                right-variable="userName" />
      </and>
    </where>
    <variable id="userName" type="STRING" name="userName" />
    <variable id="profile" type="STRING" name="profile" />
  </update>
</target>
```

**Field value sources**:

| Attribute | Source | Example |
|-----------|--------|---------|
| `variable="X"` | From a `<variable>` definition | `<field id="Name" variable="Name" />` |
| `sequence="X"` | Auto-generated from DB sequence | `<field id="Id" sequence="MySeq" />` |
| `query="X"` | Value from a sub-query | `<field id="IdePro" query="GetProfileId" />` |
| `value="X"` | Literal value | `<field id="Act" value="1" />` |

### Service Descriptors

Service XML wires named services to Java class methods. All service definitions
live in `global/Services.xml`.

**Root element**: `<services>`

**Key elements**:

| Element | Purpose |
|---------|---------|
| `<service id="...">` | Named service (referenced by queries and maintain `<serve>`) |
| `<java classname="..." method="...">` | Java method binding |
| `<service-parameter type="..." name="...">` | Method parameter mapping |

**Example 1** — Simple service (no parameters):

```xml
<service id="SysDat">
  <java classname="com.almis.awe.service.SystemService" method="getDate" />
</service>
```

**Example 2** — Service with parameters:

```xml
<service id="storeSessionString">
  <java classname="com.almis.awe.service.SessionService"
        method="setSessionParameter">
    <service-parameter type="STRING" name="name" />
    <service-parameter type="STRING" name="value" />
  </java>
</service>
```

**Example 3** — Service with launch phase (runs at app startup):

```xml
<service id="loadDatabaseProperties" launch-phase="APPLICATION_START">
  <java classname="com.almis.awe.service.PropertyService"
        method="refreshDatabaseProperties" />
</service>
```

**Wiring pattern** — How service connects query to Java:

```
Queries.xml:   <query id="SysDat" service="SysDat"> ...
                                        |
Services.xml:  <service id="SysDat">    <--- matches service attribute
                 <java classname="...SystemService" method="getDate" />
                                                          |
Java:          SystemService.getDate()   <--- method called
```

### Action Descriptors

Action XML defines server-side action handlers that process requests and return structured
responses. All action definitions live in `global/Actions.xml`.

**Root element**: `<actions>`

**Key elements**:

| Element | Purpose |
|---------|---------|
| `<action id="..." format="JSON\|HTML">` | Named action |
| `<call service="...">` | Service to invoke |
| `<answer type="ok\|warning\|error">` | Response handler per result type |
| `<response>` | Individual response directive |
| `<type>` | Response action: `end-load`, `message`, `cancel`, `redirect`, `screen` |
| `<parameter>` | Message parameter binding |

**Example** — Action with ok/error response handling:

```xml
<action id="login-azure" format="JSON">
  <call service="login-azure" />
  <answer type="ok">
    <response>
      <type>end-load</type>
    </response>
  </answer>
  <answer type="error">
    <response>
      <type>end-load</type>
    </response>
    <response>
      <type>message</type>
      <parameters>
        <parameter name="type" variable="MESSAGE_TYPE" />
        <parameter name="title" variable="MESSAGE_TITLE" />
        <parameter name="message" variable="MESSAGE_DESCRIPTION" />
      </parameters>
    </response>
    <response>
      <type>cancel</type>
    </response>
  </answer>
</action>
```

**Response type reference**:

| Type | Effect |
|------|--------|
| `end-load` | Stop the loading indicator |
| `message` | Display a message to the user |
| `cancel` | Cancel the current action chain |
| `redirect` | Redirect to a URL |
| `screen` | Navigate to a screen |

**Button-action server-action values**:

| server-action | Behavior |
|---------------|----------|
| `maintain` | Call a maintain target, show result message |
| `maintain-silent` | Call a maintain target, suppress result message |
| `data` | Call a query (for suggest/select loading) |

## Java Service Pattern

All AWE Java services follow the `ServiceConfig` -> `ServiceData` pattern:

1. **Extend `ServiceConfig`** — provides access to AWE context (session, locale, beans)
2. **Return `ServiceData`** — wraps response data in a `DataList` with typed columns
3. **Wire via XML** — register in `Services.xml`, reference from queries or maintain targets

### ServiceConfig Base Class

`ServiceConfig` (from `com.almis.awe.config`) gives your service:

| Method / Field | Purpose |
|----------------|---------|
| `getBean(Class)` | Get a Spring bean from the AWE context |
| `getRequest()` | Access the current request parameters |
| `getSession()` | Access session attributes |
| `getLocale(key)` | Resolve an i18n locale string |

### ServiceData Response

`ServiceData` (from `com.almis.awe.model.dto`) is the standard return type:

| Method | Purpose |
|--------|---------|
| `setDataList(DataList)` | Set the response data rows |
| `setType(AnswerType)` | Set result type: `OK`, `WARNING`, `ERROR` |
| `setMessage(String)` | Set a user-facing message |

### DataList & DataListUtil

`DataList` holds columnar data. Use `DataListUtil` helper methods to build it:

| Method | Purpose |
|--------|---------|
| `DataListUtil.addColumn(dataList, name)` | Add an empty column |
| `DataListUtil.addColumnWithOneRow(dataList, name, value)` | Add a single-value column |
| `DataListUtil.addRow(dataList, row)` | Add a `HashMap<String, CellData>` row |

### Complete Example

**Java service** (`SystemService.java`):

```java
package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import java.util.Date;

public class SystemService extends ServiceConfig {

  /**
   * Returns the current system date.
   * @return ServiceData with a "value" column containing the date
   */
  public ServiceData getDate() {
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(new DataList());
    DataListUtil.addColumnWithOneRow(
      serviceData.getDataList(), "value", new Date()
    );
    return serviceData;
  }
}
```

**XML wiring** — connect the service to a query:

```xml
<!-- Services.xml -->
<service id="SysDat">
  <java classname="com.almis.awe.service.SystemService" method="getDate" />
</service>

<!-- Queries.xml -->
<query id="SysDat" service="SysDat">
  <field transform="DATE" id="value" alias="value" />
</query>
```

**Key rules**:
- Every public service method must return `ServiceData`
- Use `DataListUtil` to build the `DataList` — never construct `CellData` manually
- Column names in `DataList` must match `<field id="...">` in the query XML
- All services in AWE extend `ServiceConfig` (confirmed across 25+ services in the codebase)

## Build, Test & Run

### Prerequisites

- Java 17 (JDK)
- Maven 3.8+
- Node.js (for frontend builds)

### Maven Commands

| Command | Purpose |
|---------|---------|
| `mvn clean install` | Full build (all modules) |
| `mvn clean install -pl awe-framework/awe-controller -am` | Build a specific module with dependencies |
| `mvn clean install -DskipTests` | Build without running tests |
| `mvn test` | Run unit tests only |
| `mvn verify` | Run unit + integration tests |
| `mvn clean install -pl awe-tests/awe-boot -am` | Build and run integration tests |
| `mvn site` | Generate project reports |
| `mvn versions:set -DnewVersion=X.Y.Z` | Update project version |

### Frontend Commands

| Command | Purpose | Working Directory |
|---------|---------|-------------------|
| `npm install` | Install frontend dependencies | `awe-framework/awe-client-angular/` |
| `npm run build` | Build frontend assets | `awe-framework/awe-client-angular/` |
| `npm test` | Run JavaScript unit tests | `awe-framework/awe-client-angular/` |

### Running a Sample Application

```bash
# Build the framework first
mvn clean install -DskipTests

# Then run a sample app
cd awe-samples/
mvn spring-boot:run
```

### Maven Profiles & Properties

| Property | Default | Purpose |
|----------|---------|---------|
| `application.acronym` | `awe` | Module acronym (used in XML paths) |
| `java.version` | `17` | Java source/target version |
| `check.xml.directory` | `src/main/resources/application/` | XML validation path |

## Development Workflow

### Branching Model (Gitflow)

AWE uses Gitflow. All branch operations follow this model:

| Branch Pattern | Base Branch | Purpose |
|----------------|-------------|---------|
| `master` | — | Stable releases only |
| `develop` | `master` | Integration branch for features |
| `feature/{name}` | `develop` | New feature development |
| `hotfix/{name}` | `master` | Critical bug fixes |
| `release/{version}` | `develop` | Release preparation |

### Merge Request Rules

1. **One MR = one feature or one bug fix** — MRs with unrelated changes will be rejected
2. **Separate logical commits** — unrelated changes in different commits
3. **Pipeline must pass** — do not attempt to merge with failing CI
4. **Remove WIP/Draft status** when development is complete
5. **Fill the template** — use `features` or `bug` templates in the MR description

### Testing Requirements

| Requirement | Details |
|-------------|---------|
| New features | Must include unit tests covering the new functionality |
| Bug fixes | Must include a test that previously failed and now passes |
| Regressions | All existing tests must continue to pass |
| Integration tests | Run via `awe-tests/awe-boot` module |

### Creating a New Feature Branch

```bash
# Start from develop
git checkout develop
git pull origin develop
git checkout -b feature/my-feature

# Work, commit, push
git add .
git commit -m "feat: add my feature"
git push origin feature/my-feature

# Create MR targeting develop
```

### XML Validation

AWE validates XML descriptors against XSD schemas. Schema locations are declared in each XML file:

```xml
<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation=
          "https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd"
        template="window" label="...">
</screen>
```

Available schemas:
- `screen.xsd` — Screen descriptors
- `queries.xsd` — Query descriptors
- `maintain.xsd` — Maintain descriptors
- `services.xsd` — Service descriptors
- `actions.xsd` — Action descriptors

Always include the `xsi:noNamespaceSchemaLocation` attribute in your root elements.

## Common Patterns & Recipes

### CRUD Screen (End-to-End)

This recipe creates a complete CRUD interface for a `Customer` entity with fields:
`IdeCustomer` (PK), `Name`, `Email`, `Active`.

**Step 1 — Screen XML** (`screen/customer-list.xml`):

```xml
<screen template="window" label="MENU_CUSTOMERS">
  <tag source="hidden">
    <message id="DelMsg" title="CONFIRM_TITLE_DELETE"
             message="CONFIRM_MESSAGE_DELETE" />
  </tag>
  <tag source="buttons">
    <button label="BUTTON_NEW" icon="plus-circle" id="ButNew">
      <button-action type="screen" target="customer-new" />
    </button>
    <button label="BUTTON_UPDATE" icon="edit" id="ButUpd">
      <button-action type="check-one-selected" target="GrdCst" />
      <button-action type="screen" target="customer-update" />
      <dependency target-type="disable" initial="true">
        <dependency-element id="GrdCst" condition="ne" value="1" />
      </dependency>
    </button>
    <button label="BUTTON_DELETE" icon="trash" id="ButDel">
      <button-action type="check-some-selected" target="GrdCst" />
      <button-action type="confirm" target="DelMsg" />
      <button-action type="server" server-action="maintain"
                     target-action="CstDel" />
      <button-action type="filter" target="GrdCst" />
      <dependency target-type="disable" initial="true">
        <dependency-element id="GrdCst" condition="lt" value="1" />
      </dependency>
    </button>
  </tag>
  <tag source="center">
    <window label="SCREEN_TEXT_DATA" icon="list" style="expand">
      <grid id="GrdCst" server-action="data" target-action="CstLst"
            initial-load="query" style="expand">
        <column label="COLUMN_NAME" name="Name" charlength="20" />
        <column label="COLUMN_EMAIL" name="Email" charlength="25" />
        <column label="COLUMN_ACTIVE" name="Active" component="icon"
                align="center" charlength="8" />
      </grid>
    </window>
  </tag>
</screen>
```

**Step 2 — Query XML** (in `global/Queries.xml`):

```xml
<query id="CstLst">
  <table id="Customer" />
  <field id="IdeCustomer" alias="IdeCustomer" />
  <field id="Name" alias="Name" />
  <field id="Email" alias="Email" />
  <field id="Active" alias="Active" />
  <where>
    <and>
      <filter left-field="Name" condition="like" right-variable="CrtName"
              ignorecase="true" optional="true" />
    </and>
  </where>
  <variable id="CrtName" type="STRINGB" name="CrtName" />
</query>
```

**Step 3 — Maintain XML** (in `global/Maintain.xml`):

```xml
<!-- Insert -->
<target name="CstNew">
  <insert>
    <table id="Customer" />
    <field id="IdeCustomer" sequence="CustomerSeq" />
    <field id="Name" variable="Name" />
    <field id="Email" variable="Email" />
    <field id="Active" variable="Active" />
    <variable id="Name" type="STRING" name="Name" />
    <variable id="Email" type="STRING" name="Email" />
    <variable id="Active" type="INTEGER" name="Active" />
  </insert>
</target>

<!-- Update -->
<target name="CstUpd">
  <update>
    <table id="Customer" />
    <field id="Name" variable="Name" />
    <field id="Email" variable="Email" />
    <field id="Active" variable="Active" />
    <where>
      <and>
        <filter left-field="IdeCustomer" condition="eq"
                right-variable="IdeCustomer" />
      </and>
    </where>
    <variable id="IdeCustomer" type="INTEGER" name="IdeCustomer" />
    <variable id="Name" type="STRING" name="Name" />
    <variable id="Email" type="STRING" name="Email" />
    <variable id="Active" type="INTEGER" name="Active" />
  </update>
</target>

<!-- Delete -->
<target name="CstDel">
  <delete multiple="true">
    <table id="Customer" />
    <where>
      <and>
        <filter left-field="IdeCustomer" condition="eq"
                right-variable="IdeCustomer" />
      </and>
    </where>
    <variable id="IdeCustomer" type="INTEGER" name="IdeCustomer" />
  </delete>
</target>
```

**Step 4 — Wiring checklist**:

| What | Where | Notes |
|------|-------|-------|
| Grid `target-action="CstLst"` | Screen XML | Must match query `id="CstLst"` |
| Button `target-action="CstDel"` | Screen XML | Must match maintain target `name="CstDel"` |
| Locale keys (`MENU_CUSTOMERS`, etc.) | `locale/Locale-EN.xml` | Add `<local name="..." value="..." />` |
| Menu entry | `menu/private.xml` | Add `<option screen="customer-list" ... />` |

### Dependency Patterns (Dynamic UI)

Dependencies let UI elements react to other elements' state changes.

**Pattern 1 — Disable button when no row selected**:

```xml
<button label="BUTTON_EDIT" id="ButEdit">
  <button-action type="check-one-selected" target="MyGrid" />
  <button-action type="screen" target="edit-screen" />
  <dependency target-type="disable" initial="true">
    <dependency-element id="MyGrid" condition="ne" value="1" />
  </dependency>
</button>
```

**Pattern 2 — Show/hide element based on dropdown value**:

```xml
<criteria label="TYPE" component="select" id="CrtType"
          initial-load="enum" target-action="TypeEnum" />
<criteria label="DETAILS" component="textarea" id="CrtDetails">
  <dependency target-type="show" initial="true">
    <dependency-element id="CrtType" condition="eq" value="ADVANCED" />
  </dependency>
</criteria>
```

**Pattern 3 — Filter a grid when a criteria changes** (master-detail):

```xml
<criteria label="CATEGORY" component="select" id="CrtCat">
  <dependency>
    <dependency-element id="CrtCat" />
    <dependency-action type="filter" target="GrdItems" />
  </dependency>
</criteria>
```

**Pattern 4 — Show button only when specific row value matches**:

```xml
<button label="BUTTON_STOP" icon="stop" id="ButStp">
  <button-action type="server" server-action="maintain"
                 target-action="StopTask" />
  <dependency target-type="show" initial="true">
    <dependency-element id="GrdTasks" condition="eq" value="1" />
    <dependency-element id="GrdTasks" column="Status"
                        attribute="selectedRowValue" condition="eq"
                        value="RUNNING" />
  </dependency>
</button>
```

**Dependency reference**:

| `target-type` | Effect |
|---------------|--------|
| `disable` | Enable/disable the element |
| `show` | Show/hide the element |
| `hide` | Hide/show the element (inverse of `show`) |
| `set-value` | Set element value |
| `reset` | Reset element to initial state |

| `dependency-element` attribute | Purpose |
|-------------------------------|---------|
| `id` | Source element to watch |
| `condition` | Comparison: `eq`, `ne`, `gt`, `lt`, `ge`, `le` |
| `value` | Static value to compare against |
| `column` | Grid column name (for row-based conditions) |
| `attribute` | What to read: `selectedRowValue`, `value`, `label` |
| `id2` | Second source element (for element-to-element comparison) |

**Dependency action reference** (for `<dependency-action>` inside `<dependency>`):

| `type` | Purpose | Key Attributes |
|--------|---------|----------------|
| `filter` | Reload/filter a grid or component | `target` = grid id |
| `set-value` | Set a field's value | `target` = criteria id, `value` = new value |
| `add-class` | Add a CSS class | `target` = CSS selector, `target-action` = class name |
| `remove-class` | Remove a CSS class | `target` = CSS selector, `target-action` = class name |
| `reset` | Reset to initial state | `target` = element id |

### Naming Conventions

| Entity | Convention | Examples |
|--------|-----------|----------|
| Screen file | lowercase-kebab | `customer-list.xml`, `scheduler-tasks.xml` |
| Query id | PascalCase or camelCase | `CstLst`, `getApplicationParameters` |
| Maintain target | PascalCase | `CstNew`, `CstUpd`, `CstDel`, `UsrNew` |
| Service id | PascalCase or camelCase | `SysDat`, `storeSessionString` |
| Action id | lowercase-kebab or PascalCase | `login-azure`, `DEFAULT_ERROR` |
| Grid id | `Grd` + abbreviated name | `GrdTskLst`, `GrdCst` |
| Criteria id | `Crt` + abbreviated name | `CrtTsk`, `CrtAct` |
| Button id | `But` + abbreviated action | `ButNew`, `ButUpd`, `ButDel` |
| Variable id | Matches field or criteria | `IdeOpe`, `Usr`, `Sta` |
| Locale key | `UPPER_SNAKE_CASE` | `MENU_CUSTOMERS`, `BUTTON_NEW` |

## Resources & References

### Documentation

- **AWE Website Docs**: `website/docs/` — Guides, API reference, tutorials
- **Project Structure Guide**: `website/docs/guides/project-structure.md`
- **Contributing Guide**: `CONTRIBUTING.md` — Branching rules, MR requirements

### Key Codebase Locations

| What | Path |
|------|------|
| Global XML descriptors | `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/` |
| Screen descriptors | `awe-framework/awe-modules/{module}/src/main/resources/application/{acronym}/screen/` |
| Java services | `awe-framework/awe-controller/src/main/java/com/almis/awe/service/` |
| DTOs (ServiceData, DataList) | `awe-framework/awe-model/src/main/java/com/almis/awe/model/dto/` |
| XSD schemas | Published at `https://aweframework.gitlab.io/awe/docs/schemas/` |
| Spring Boot starters | `awe-framework/awe-starters/` |
| Test utilities | `awe-framework/awe-testing/` |
| Integration tests | `awe-tests/` |

### XSD Schema URLs

| Schema | URL |
|--------|-----|
| Screen | `https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd` |
| Queries | `https://aweframework.gitlab.io/awe/docs/schemas/queries.xsd` |
| Maintain | `https://aweframework.gitlab.io/awe/docs/schemas/maintain.xsd` |
| Services | `https://aweframework.gitlab.io/awe/docs/schemas/services.xsd` |
| Actions | `https://aweframework.gitlab.io/awe/docs/schemas/actions.xsd` |

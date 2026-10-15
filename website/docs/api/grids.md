---
id: grids
title: Grids
---

Grids are the main component in AWE to show data lists. It is defined by *grid* and *columns*:

* The **grid** is the container of the data list. It has the query to load and a set of attributes to define the behaviour of the grid.
* Each **column** contains the source of the data from the data list, the label of the column and other attributes to manage each column cell.

There is also a **group-header** tag which is used to show a second level header over a group of column headers.

<img alt="Grid" src={require('@docusaurus/useBaseUrl').default('img/Grid.png')} />

## XML skeleton

To define a **grid** or a **treegrid** in AWE you must follow the next structure:

```xml
<grid id="[grid-identifier]" ...>
  <column ... ></column>
  ...
  <group-header ...>
    <column ...></column>
    ...
  </group-header>
  ...
  <button ... > ... </button>
  ...
  <context-button ...> ... </context button>
  ...
</grid>
```
## Grid structure

```xml
<grid id="[grid-id]" style="[grid-style]" 
      multiselect="[grid-is-multiselect]" checkbox-multiselect="[checkbox-multiselectable]" editable="[grid-is-editable]" send-operations="[grid-is-multioperation]" initial-load="[initial-load]" server-action="[server-action]" target-action="[target-action]" max="[elements-per-page] pagination-disabled="[pagination-disabled]"">
...
</grid>
```

### General attributes

| Attribute           | Use          | Type    | Description                                                                                                               | Values                                                                                                                             |
|---------------------|--------------|---------|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| id                  | **Required** | String  | Grid identifier. For reference purposes                                                                                   |                                                                                                                                    |
| label               | Optional     | String  | Grid title (For printing only)                                                                                            | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                         |
| style               | Optional     | String  | Grid CSS classes                                                                                                          |                                                                                                                                    |
| initial-load        | Optional     | String  | Server action call to load the grid data. It only supports `query` value                                                  |                                                                                                                                    |
| server-action       | Optional     | String  | Server action call                                                                                                        | See [server action list](actions.md#server-actions)                                                                                |
| target-action       | Optional     | String  | Target to call on the server                                                                                              |                                                                                                                                    |
| max                 | Optional     | Integer | Maximum number of elements to retrieve **per page**                                                                       |                                                                                                                                    |
| pagination-disabled | Optional     | Boolean | Disabled the pagination element of grid                                                                                   | **Note:** Default value `false`                                                                                                    |
| pager-values        | Optional     | String  | Enable grid pager. Set the value list with the number of rows per page                                                    | **Example:** `pager-value="5,25,50,100"`                                                                                           |
| load-all            | Optional     | Boolean | Load all values of the grid and sort and paginate locally                                                                 | **Note:** Default value is `false`                                                                                                 |
| send-all            | Optional     | Boolean | Send all values of the grid to the server instead of selected ones. (Selected ones will be sent on `.selected` parameter) | **Note:** Default value is `false`                                                                                                 |
| help                | Optional     | String  | Help text for the grid                                                                                                    | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                         |
| help-image          | Optional     | String  | Help image for the grid                                                                                                   | This **must** be a image path                                                                                                      |
| icon-loading        | Optional     | String  | Set the loading icon                                                                                                      | `skeleton`, `spinner`, `square`, `circles`, `carpet`, `dots`, `folding`, `squarebar`, `circlebar`, `cubes`, `icon`, `custom`, `none`. Default: the [`awe.application.component.grid-icon-loading`](../properties#awe.application.component.grid-icon-loading) property (`skeleton`). `skeleton` shows placeholder rows with the grid columns; set another value to disable it on this grid |
| row-numbers         | Optional     | Boolean | Whether to show row numbers or not                                                                                        | Default value is `true`                                                                                                            |                                                                                                          
| row-height          | Optional     | Integer | Set the row height in pixels                                                                                              |                                                                                                                                    |
| validate-on-save    | Optional     | Boolean | Set `false` to avoid validate the selected row when clicking the `save` button                                            | Default value is `true`                                                                                                            |
| empty-icon          | Optional     | String  | Icon of the [empty state](#empty-state) of the grid                                                                       | Default value is `inbox`. Check [icons](icons.md)                                                                                  |
| empty-title         | Optional     | String  | Title of the [empty state](#empty-state) of the grid                                                                      | **Note:** You can use [i18n](i18n-internationalization.md) files (locales). Default: a translated "No records found" title         |
| empty-text          | Optional     | String  | Text of the [empty state](#empty-state) of the grid                                                                       | **Note:** You can use [i18n](i18n-internationalization.md) files (locales). Default: a translated explanation text                 |
| empty-action        | Optional     | String  | Identifier of a button of the grid that is offered in the [empty state](#empty-state)                                     | The empty state shows a button with the label and the icon of that button, which runs its actions                                  |


### Empty state

A grid without rows shows an empty state instead of an empty table, in both the AngularJS and the React engines. It is
made of an icon, a title and a text. The default texts are translated; use `empty-icon`, `empty-title` and `empty-text`
to change them. While the grid is loading, the empty state is not shown.

The optional `empty-action` points to a button of the grid (a `button` declared inside the grid). The empty state offers
a button with the label and the icon of the referenced one, and clicking it runs the actions of that button, so it
can be used, for example, to clear the search criteria.

```xml
<grid id="UserGrid" server-action="data" target-action="UserList"
      empty-icon="search" empty-title="GRID_NO_USERS" empty-text="GRID_NO_USERS_HELP" empty-action="ClearSearch">
  <column label="USER" name="user" />
  <button id="ClearSearch" label="BUTTON_CLEAR" icon="eraser" button-type="reset" />
</grid>
```

## Column structure

```xml
<column label="[column-label]" name="[column-name]" sort-field="[sort-field]" align="[text-align]" 
        charlength="[column-width-chars]" component="[column-component]" >
  <dependency ...> ... </dependency>
  ...
</column>
```

### Column attributes

| Attribute  | Use          | Type    | Description                                                                                                                       | Values                                                                           |
|------------|--------------|---------|-----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| name       | **Required** | String  | Column identifier. For reference purposes                                                                                         |                                                                                  |
| sort-field | Optional     | String  | Sort field to sort by (if not treegrid)                                                                                           |                                                                                  |
| type       | Optional     | String  | Field type (for printing purposes)                                                                                                | `string`, `integer`, `float` or `date`                                           |
| hidden     | Optional     | Boolean | Column is **not** visible on screen. It can still be printed, see [printing columns](#printing-columns)                           | Default value is `false`                                                         |
| printable  | Optional     | String  | Whether the column is included in the printed report. See [printing columns](#printing-columns)                                   | `true` (default) or `false`                                                      |
| align      | Optional     | String  | Column is **not** visible                                                                                                         | `left`, `center` or `right`                                                      |
| width      | Optional     | Integer | Column width in pixels or percent.                                                                                                | Ex.: `width = "10"` or `width = "20%"` **Note:** You can use '*' value for auto. |
| sortable   | Optional     | Boolean | Field is sortable (if not treegrid)                                                                                               | Default value is `true`                                                          |
| movable    | Optional     | Boolean | Allows to move column position in the grid                                                                                        | Default value is `true`                                                          |
| sendable   | Optional     | Boolean | Column data must be sent to the server                                                                                            | Default value is `true`                                                          |
| charlength | Optional     | Integer | Column width in chars                                                                                                             |                                                                                  |
| label      | Optional     | String  | Column label                                                                                                                      | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)       |
| style      | Optional     | String  | Column css class                                                                                                                  | Css class to apply to the column                                                 |
| component  | Optional     | String  | Column type (if editable)                                                                                                         | See [components](criteria.md#components)                                         |
| max        | Optional     | Integer | Max number of records to retrieve when the column is initialized with target-action                                               | Default value is `30`                                                            |
| visibility | Optional     | Boolean | Initial visibility of the column component                                                                                        | Default value is `true`                                                          |
| frozen     | Optional     | Boolean | Keep the column fixed out of the horizontal scroll. **WARNING**: Don't use this attribute if header has more than one line height | Default value is `false`                                                         |

> **Note:** When a column is editable (it has a *component*) all attributes of criteria can be used in the column. See **[criteria attributes](criteria.md#criteria-structure)** for more references.

### Printing columns

When a screen is printed (see the [print engine guide](../guides/print-guide.md)), each grid decides which columns go to
the report using the `printable` attribute of the column:

| `printable` | Printed in PDF, DOCX and TEXT      | Printed in XLSX and CSV            |
|-------------|------------------------------------|------------------------------------|
| not set     | Only when the column is on screen  | Only when the column is on screen  |
| `true`      | Always, even if `hidden="true"`    | Always, even if `hidden="true"`    |
| `excel`     | Never                              | Always, even if `hidden="true"`    |
| `false`     | Never                              | Never                              |

"On screen" means the column is not `hidden="true"` and it has not been hidden at runtime by a `hide-column`
dependency action. Declaring `printable="true"`, `printable="excel"` or `printable="false"` takes precedence
over the screen state.

This makes it possible to show a styled value on screen while exporting the raw value:

```xml
<grid id="GrdAmounts" server-action="data" target-action="QryAmounts" load-all="true">
  <!-- Shown on screen with HTML styling, excluded from the report -->
  <column label="AMOUNT" name="Amount" align="right" charlength="20" printable="false"/>
  <!-- Hidden on screen, printed as a numeric value in every format -->
  <column label="AMOUNT" name="AmountRaw" align="right" charlength="20" hidden="true" type="float" printable="true"/>
  <!-- Hidden on screen, exported to the spreadsheet only -->
  <column label="INTERNAL_CODE" name="Code" charlength="10" hidden="true" printable="excel"/>
</grid>
```

Keep in mind:

- A printed column needs its data in the client, so it must be `sendable` (the default). A `sendable="false"` column
  appears in the report header with empty cells.
- Use `type` (`string`, `integer`, `float`, `date`) on printed columns so spreadsheet cells get the right format.
- Columns inside a `group-header` keep their grouping in the report. Columns of the group that are not printed in a
  format are left out, and the group header disappears when none of its columns is printed.
- When a print request mixes spreadsheet and document formats and some column is `printable="excel"`, AWE designs the
  report twice, once per kind of output. Grids that print from a query run that query once per design.
- The values `all` and `tab`, accepted by the schema in earlier versions, have been removed. Replace `all` with `true`.

### Column components

| Component        | Description                                                                                                 |
|------------------|-------------------------------------------------------------------------------------------------------------|
| text             | Text column component. See [text criterion](criteria.md#text-criterion)                                     |
| password         | Password column component. See [password criterion](criteria.md#password-criterion)                         |
| textarea         | Textarea column component. See [textarea criterion](criteria.md#textarea-criterion)                         |
| numeric          | Numeric column component. See [numeric criterion](criteria.md#numeric-criterion)                            |
| date             | Date column component. See [date criterion](criteria.md#date-criterion)                                     |
| time             | Time column component. See [time criterion](criteria.md#time-criterion)                                     |
| filtered-date    | Filtered date column component. See [filtered date criterion](criteria.md#filtered-date-criterion)          |
| select           | Select column component. See [select criterion](criteria.md#select-criterion)                               |
| suggest          | Suggest column component. See [suggest criterion](criteria.md#suggest-criterion)                            |
| select-multiple  | Select multiple column component. See [select multiple criterion](criteria.md#multiple-select-criterion)    |
| suggest-multiple | Suggest multiple column component. See [suggest multiple criterion](criteria.md#multiple-suggest-criterion) |
| checkbox         | Checkbox column component. See [checkbox criterion](criteria.md#checkbox-criterion)                         |
| radio            | Radio column component: the options of a list as a row of radio buttons. See [radio column component](#radio-column-component) |
| button-radio     | Radio column component: the options of a list as one joined bar of buttons. See [radio column component](#radio-column-component) |
| color            | Color column component. See [color criterion](criteria.md#color-criterion)                                  |
| uploader         | Uploader column component. See [uploader criterion](criteria.md#uploader-criterion)                         |
| text-view        | Text view column component. See [text view criterion](criteria.md#text-view-criterion)                      |
| formatted-text   | HTML Text column component. See [formatted-text component](#formatted-text-column-component)                |
| icon             | Icon column component. Use it for showing font awesome icons in a column.                                   |
| image            | Image column component. Use it for showing images in a column.                                              |
| button           | Component which allows the user to click a button which launches a set of `button-action`                   |
| progress         | Progress column component. Useful for showing a progress bar in a column. See [progress](#progress-column-component) |
| sparkline        | Sparkline column component: a small line, bar or area chart of a series of numbers. See [sparkline column component](#sparkline-column-component) |
| link             | HTML Link component. Opens an URL in a new page                                                             |


### Specific row style

When you need set specific style to all cells of one row (ex.: change background color), you can add a new field called `_style_` with CSS style name.

```xml
<query id="MyQuery" >
    <field id="_style_" alias="_style_" variable="styleClass"/>
    <variable id="styleClass" type="STRING" value="red"/>
</query>
```

### Theme classes

AWE provides ready-made CSS classes for two common grid looks. They are available in **both client engines and in every
theme**, light or dark, so an application does not need to write its own CSS.

#### Grid without header row

Add the `grid-no-header` class to the `style` of the grid. The header row is hidden but it keeps taking part in the
layout of the columns, so the columns keep their widths (`charlength` and `width`). It is useful for compact lists.

```xml
<grid id="GrdCompactList" style="expand grid-no-header" initial-load="query" server-action="data" target-action="CompactList">
  <column label="Name" name="name" align="left" charlength="30"/>
  <column label="Description" name="description" align="left" charlength="40"/>
</grid>
```

:::note
The header is not available to screen readers while it is hidden, and neither are the sort, filter and column menu
controls of the header. Use the class on grids that do not need them.
:::

#### Colour-scaled cells (heat maps)

The query assigns one of the following classes to each cell with the `style` attribute of a
[compound cell](#standard-cell-without-components). Every class paints the whole cell and sets its own text colour, so the text keeps
a contrast of at least 4.5:1 (WCAG AA) in every theme.

| Scale                         | Classes                                             | Use it for                                  |
|-------------------------------|-----------------------------------------------------|---------------------------------------------|
| Sequential (blue)             | `scale-seq-1` (lowest) to `scale-seq-5` (highest)   | Values that go from low to high             |
| Diverging, positive (green)   | `scale-pos-1` (lowest) to `scale-pos-5` (highest)   | Values above the reference (e.g. gains)     |
| Diverging, negative (red)     | `scale-neg-1` (lowest) to `scale-neg-5` (highest)   | Values below the reference (e.g. losses)    |

For example, a query that colours the amount in the positive or negative scale depending on its value:

```xml
<query id="Results">
  <table id="Results"/>
  <field id="Name" alias="name"/>
  <field id="Amount" alias="amount" noprint="true"/>
  <compound alias="result">
    <computed alias="value" format="[amount]"/>
    <computed alias="label" format="[amount]"/>
    <computed alias="style" format="[amount] &lt; -50 ? 'scale-neg-5' : [amount] &lt; 0 ? 'scale-neg-2' : [amount] &lt; 50 ? 'scale-pos-2' : 'scale-pos-5'" eval="true"/>
  </compound>
</query>
```

The test applications include the screens *Grid without header* and *Colour scales* in the matrix test menu, with an
example of each class.

### Specific column components

There are several components developed specifically to be used in grid cells. 
These components need a special structure on the query which fills them:

```xml
<compound alias="specialCell">
  <computed alias="value" format="xxx"/>  
  <computed alias="label" format="xxx" translate="EnumTranslate"/>
</compound>
```

These are the cells which use the special structure:

#### Standard cell without components

When you send a **compound structure** to a standard cell, you can use the following attributes:

| Attribute | Description                                                                                                          |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| value     | Value which will be sent to the server                                                                               |
| label     | Text which will be shown. You can use [i18n](i18n-internationalization.md) files (locales)                           |
| title     | Text to show when you move the mouse over the icon. You can use [i18n](i18n-internationalization.md) files (locales) |
| style     | CSS class to format the contents                                                                                     |

#### Text view column component

This component is very useful to show an styled text inside a grid with an icon. The **compound attributes** to fill this component are the following:

| Attribute | Description                                                                                                          |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| value     | Value which will be sent to the server                                                                               |
| label     | Text which will be shown. You can use [i18n](i18n-internationalization.md) files (locales)                           |
| title     | Text to show when you move the mouse over the icon. You can use [i18n](i18n-internationalization.md) files (locales) |
| icon      | Icon class. You can check all iconset at [icons](icons.md) screen                                                    |
| unit      | Unit label (shown on the right of the cell). You can use [i18n](i18n-internationalization.md) files (locales)        |
| style     | CSS class to format the contents                                                                                     |

#### Icon column component

<img alt="IconColumn" src={require('@docusaurus/useBaseUrl').default('img/IconColumn.png')} />

This component is very useful to show an icon inside a grid. The **compound attributes** to fill this component are the following:

| Attribute | Description                                                                                                          |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| value     | Value which will be sent to the server                                                                               |
| label     | Text to show when you move the mouse over the icon. You can use [i18n](i18n-internationalization.md) files (locales) |
| title     | Text to show when you move the mouse over the icon. You can use [i18n](i18n-internationalization.md) files (locales) |
| icon      | Icon class. You can check all iconset at [icons](icons.md) screen                                                    |
| style     | CSS class to format the contents                                                                                     |

#### Formatted text column component

This component is used to show HTML code inside a grid. There are two type of HTML tags allowed:

* `<format style="[text class]">`: Formatted text. 
* `<br>`: New line

#### Image column component

<img alt="ImageColumn" src={require('@docusaurus/useBaseUrl').default('img/ImageColumn.png')} />

This component is used to show an image inside a grid. The **compound attributes** to fill this component are the following:

| Attribute | Description                                                                                                          |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| value     | Value which will be sent to the server                                                                               |
| label     | Alternate text. You can use [i18n](i18n-internationalization.md) files (locales)                                     |
| title     | Text to show when you move the mouse over the icon. You can use [i18n](i18n-internationalization.md) files (locales) |
| image     | Image path                                                                                                           |
| style     | CSS class to format the contents                                                                                     |

#### Button column component

<img alt="Button Column" src={require('@docusaurus/useBaseUrl').default('img/DialogColumn.png')} />

A button column is a component which allows to launch a set of `button-action` defined on column.

| Attribute | Description                                                                                                            |
|-----------|------------------------------------------------------------------------------------------------------------------------|
| value     | Value which will be sent to the server                                                                                 |
| label     | Text to show in the button. You can use [i18n](i18n-internationalization.md) files (locales)                           |
| title     | Text to show when you move the mouse over the button. You can use [i18n](i18n-internationalization.md) files (locales) |
| icon      | Icon class to show in the button. You can check all iconsets at [icons](icons.md) screen                               |
| style     | CSS class to format the contents                                                                                       |

#### Radio column component

The `radio` and `button-radio` columns show **all the options of a list in every cell** and the user chooses one of them.
The options come from the column in the same way as in a `select` column: with `initial-load` and `target-action`
(for instance an enumerated, `enum`, or a query) and every option has a `value` and a `label`. The value of the chosen
option is the value of the cell.

```xml
<enumerated>
  <group id="LevelOptions">
    <option label="Low" value="L"/>
    <option label="Medium" value="M"/>
    <option label="High" value="H"/>
  </group>
</enumerated>
```

```xml
<grid id="GrdTheme" style="expand" initial-load="query" server-action="data" target-action="ThemeList" send-all="true">
  <column label="Theme" name="name" align="left" charlength="30"/>
  <column label="Level" name="level" component="radio" initial-load="enum" target-action="LevelOptions" align="center" charlength="30"/>
  <column label="Mode" name="mode" component="button-radio" initial-load="enum" target-action="ModeOptions" align="center" charlength="30"/>
</grid>
```

| Attribute      | Description                                                                                                                   |
|----------------|-------------------------------------------------------------------------------------------------------------------------------|
| initial-load   | How the options are loaded: `enum` for an enumerated, `query` for a query                                                      |
| target-action  | Enumerated group or query that provides the options (`value` and `label` of each one)                                          |
| label          | Header of the column. It is also the accessible name of the group of options of every cell                                      |
| validation     | `required` requires a choice in every row                                                                                      |
| readonly       | A read-only column, or a read-only cell, cannot be changed                                                                     |

To start with a choice, the query that fills the grid returns the value of the option in a field named like the column
(here `level`, with `L`, `M` or `H`). A row where that field is empty or null starts without choice.

The column behaves like any other column of the grid: it can be sorted, validated, edited and printed.

##### `radio` look

Every option is a radio button followed by its label, and the options of a cell are in one line. When the column is
narrow, the React engine wraps them onto more lines, while the AngularJS engine keeps one line and hides what does not fit,
so give the column enough width (for example with `charlength`). The checked radio has the primary colour of the theme.

##### `button-radio` look

The options of a cell are **one joined bar of buttons** (a segmented control), like the
[button radio criterion](criteria.md#radio-button-criterion-line): adjacent buttons share their borders, only the two
ends of the bar are rounded and the chosen option is highlighted.

##### Keyboard, validation and read-only

| Behaviour     | Description                                                                                                                                       |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Keyboard      | The left and right arrow keys move the choice between the options of the cell (the up and down ones too) and go round at both ends; `Tab` leaves the cell |
| Accessibility | The options of a cell are a `radiogroup` labelled with the label of the column, and the radios of a row share one name                              |
| Validation    | With `validation="required"` a row without choice is not valid                                                                                    |
| Read-only     | A read-only column, or a read-only cell, cannot be changed                                                                                        |

:::note
The chosen option cannot be unchecked by itself, only by choosing another one.
:::

##### Data sent to the server

The grid sends the column in the same way as a `select` column: a list with one value per row (in the same order as the
other columns), the chosen option of each row and `null` for a row without choice:

```json
{
  "id": [12, 3, 7],
  "level": ["M", null, "H"],
  "level.selected": "H"
}
```

* `<column>` has the chosen value of each row sent (all the rows when the grid has `send-all`, the selected ones otherwise).
* `<column>.selected` has the value of the selected rows.
* `<column>.editing` has the value of the rows being edited, in editable and multioperation grids.

The maintain target reads the list as a variable named like the column (`<variable id="level" type="STRING" name="level"/>`).

#### Progress column component

<img alt="Progress" src={require('@docusaurus/useBaseUrl').default('img/Progress.png')} />

The progress component is very useful to show the status of a job, or a task.

| Attribute | Description                                                                                                            |
|-----------|------------------------------------------------------------------------------------------------------------------------|
| value     | Value (in percentage (0-100) of the progress bar                                                                       |
| label     | Text to show in the progress bar (ie percentage)                                                                       |
| title     | Text to show when you move the mouse over the button. You can use [i18n](i18n-internationalization.md) files (locales) |
| style     | CSS class to format the contents                                                                                       |

#### Sparkline column component

A sparkline draws a small chart of a series of numbers inside the cell, so a trend per row (sales per month, load per
day) can be seen without opening a chart. The value of the cell is a **JSON array of numbers**, for example
`[3, 5, -1, 8]`. The query can return it as text or as an already parsed list.

```xml
<column label="LOAD" name="load" component="sparkline" sparkline-type="bar" sparkline-negative-color="#d64550" charlength="14"/>
```

| Attribute               | Description                                                                                                                  | Values                                    |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| sparkline-type          | Drawing of the series: a connected line, one bar per value, or a line with the area under it filled                           | `line` (default), `bar` or `area`         |
| sparkline-color         | Colour of the series. A value that is not a plain colour is ignored                                                           | Any CSS colour, ex.: `#3b82f6`            |
| sparkline-negative-color | Colour of the negative values: the bars below zero, or the parts of the line and the area that go below zero. Without it the negative values use the colour of the series | Any CSS colour                            |
| sparkline-last-point    | Highlights the last point of the series: a dot on lines and areas, a dark outline on bars                                     | `true` or `false` (default)               |
| sparkline-min-max       | Marks the lowest point (red) and the highest point (green) of the series with a dot                                           | `true` or `false` (default)               |

How the series is drawn:

* The chart is an SVG of 100 by 24 pixels, drawn in the same way by **both client engines**. Give the column enough
  width (`charlength="14"` or more); the cell alignment (`align`) places the chart inside the cell.
* **Bars** and **negative values**: bars start at the zero line, so the negative ones hang below it. Lines and areas
  are scaled to the lowest and highest values; a thin grey zero line is drawn whenever the series has a negative value.
* **Tooltip**: hover a point to see its value. Each point has a hit area as wide as its share of the chart.
* **Single value**: lines and areas draw one dot, bars draw one bar.
* **Empty or invalid series**: the cell is left empty, with no error. That is the case of an empty value, `[]`, a text
  that is not JSON, a value that is not a list, and a list with any item that is not a number.
* The cell is a display component: it is not editable and not a tab stop (the chart is not focusable), so it does not
  get in the way of the keyboard navigation of the grid.
* The legacy jQuery sparkline plugin of the AngularJS client has been removed; applications that used its CSS classes
  (`jqstooltip`, `stats-sparklines`) must provide their own.

## Group header structure

```xml
<group-header label="[group-header-label]" name="[group-header-name]">
  <column...> ... </column>
  ...
</group-header>
```

<img alt="GroupHeader" src={require('@docusaurus/useBaseUrl').default('img/GroupHeader.png')} />

### Group header attributes

| Attribute | Use          | Type   | Description                                      | Values                                                                     |
|-----------|--------------|--------|--------------------------------------------------|----------------------------------------------------------------------------|
| name      | **Required** | String | Group header identifier. For reference purposes  |                                                                            |
| label     | Optional     | String | Group header label                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |

## Basic grid

The basic grid is the standard grid without a tree structure. It can be defined as single-select grid, multiselect grid, editable grid or multi-option grid, and combinations between them.

<img alt="BasicGrid" src={require('@docusaurus/useBaseUrl').default('img/BasicGrid.png')} />

### Basic grid specific attributes

| Attribute   | Type    | Description                 | Values                   |
|-------------|---------|-----------------------------|--------------------------|
| show-totals | Boolean | Show a line with totalizers | Default value is `false` |
| row-numbers | Boolean | Show the row numbers        | Default value is `false` |


## Multiselect grid

<img alt="MultiselectGrid" src={require('@docusaurus/useBaseUrl').default('img/MultiselectGrid.png')} />

### Multiselect grid specific attributes

| Attribute            | Type     | Description                                                                      | Values                   |
|----------------------|----------|----------------------------------------------------------------------------------|--------------------------|
| multiselect          | Boolean  | Allow selecting more than one line                                               | Default value is `false` |
| checkbox-multiselect | Boolean  | Allow selecting more than one line, but clicking **only** on the side checkboxes | Default value is `false` |

## Tree grid

The tree grid is a grid that can be expanded as a tree. It can be defined as *standard tree*, *loading tree*, *editable tree* or *multi-option tree*, and combinations between them.

The data structure **needs** an **identifier per row**, and also a **parent identifier**, with no value if the row is a root row (with no parent). The identifier field must be defined on the grid tree-id attribute, and the parent must be defined on the tree-parent attribute.

<img alt="TreeGrid" src={require('@docusaurus/useBaseUrl').default('img/TreeGrid.png')} />

### Tree grid specific attributes

| Attribute     | Type    | Description                       | Values                                                           |
|---------------|---------|-----------------------------------|------------------------------------------------------------------|
| treegrid      | Boolean | Set grid as treegrid              | Default value is `false`                                         |
| tree-id       | String  | Define identifier field           | Default value is `id`                                            |
| tree-parent   | String  | Define parent field               | Default value is `parent`                                        |
| tree-leaf     | String  | Define the 'is leaf' field        | Default value is `isLeaf`                                        |
| expand-column | String  | Name of the column to be expanded | Must be a column identifier                                      |
| initial-level | Integer | Initial level to expand           | Default value is `1`                                             |
| icon-expand   | String  | Icon of a branch without expand   | **Note:** You can check all iconsets at [icons](icons.md) screen |
| icon-collapse | String  | Icon of a expanded branch         | **Note:** You can check all iconsets at [icons](icons.md) screen |
| icon-leaf     | String  | Icon of a leaf branch             | **Note:** You can check all iconsets at [icons](icons.md) screen |

## Editable grid

<img alt="EditableGrid" src={require('@docusaurus/useBaseUrl').default('img/EditableGrid.png')} />

### Editable grid specific attributes

| Attribute | Type    | Description                 | Values                   |
|-----------|---------|-----------------------------|--------------------------|
| editable  | Boolean | Allow editing the grid rows | Default value is `false` |

### Moving rows

An editable grid can reorder its rows with four client actions that move the selected row: `move-row-top`, `move-row-up`,
`move-row-down` and `move-row-bottom`. See the [actions](actions.md) page. The rows keep the new order in the grid model, so
that order is the one the grid sends to the server when it is saved (set `send-all="true"` to send every row, not only the
selected ones). The order of the rows is stored in the position of the values of each column list, and an application
that wants to keep it must save that position (for instance in an `order` column).

```xml
<grid id="GrdSteps" editable="true" send-all="true" server-action="data" target-action="StepList">
  <column name="name" label="COLUMN_NAME" component="text"/>
  <button label="BUTTON_MOVE_ROW_UP" icon="angle-up" id="ButStepUp">
    <button-action type="move-row-up" target="GrdSteps" silent="true"/>
    <dependency target-type="enable" initial="true">
      <dependency-element id="GrdSteps" attribute="selectedRows" condition="eq" value="1"/>
    </dependency>
  </button>
</grid>
```

With a row selected, both web engines also move it with the keyboard while the grid has the focus:

| Keys             | Action          |
|------------------|-----------------|
| `Alt+ArrowUp`    | `move-row-up`   |
| `Alt+ArrowDown`  | `move-row-down` |
| `Alt+Home`       | `move-row-top`  |
| `Alt+End`        | `move-row-bottom` |

The actions are ignored when the row is already in that position, and tree grids do not support them.

## Multioption grid

<img alt="MultioptionGrid" src={require('@docusaurus/useBaseUrl').default('img/MultioptionGrid.png')} />

> **Note:** The multioption grid will send a variable called `[GridId]-RowTyp` (where `[GridId]` is the identifier of the grid) containing the action done in each row (`INSERT`, `UPDATE` or `DELETE` actions)

### Multioption grid specific attributes

| Attribute       | Type    | Description                                                      | Values                   |
|-----------------|---------|------------------------------------------------------------------|--------------------------|
| send-operations | Boolean | Allow editing the grid rows, but sending only the modified lines | Default value is `false` |

## Grid buttons


You can add button elements to perform actions over the grids. For instance, a pair of buttons to insert and delete rows.

<img alt="GridButtons" src={require('@docusaurus/useBaseUrl').default('img/GridButtons.png')} />

```xml
<grid id="GrdKeyLst" style="expand" initial-load="query" server-action="data" target-action="AweKeyLst" send-operations="true" editable="true" max="40">
  <column label="COLUMN_NAM" name="KeyNam" sort-field="KeyNam" align="left" charlength="20" component="text" validation="required">
    <dependency source-type="query" server-action="unique" target-action="AweKeyUni">
      <dependency-element id="GrdKeyLst" column="KeyNam" attribute="currentRowValue" />
    </dependency>
  </column>
  ...
  <button label="BUTTON_NEW" icon="plus-circle" id="ButGrdKeyLstAdd">
    <button-action type="add-row" target="GrdKeyLst" silent="true" />
  </button>
  <button label="BUTTON_DELETE" icon="trash" id="ButGrdKeyLstDel">
    <button-action type="delete-row" target="GrdKeyLst" silent="true" />
    <dependency target-type="enable" initial="true">
      <dependency-element id="GrdKeyLst" attribute="selectedRows" condition="==" value="1" />
    </dependency>
  </button>
</grid>
```

## Context menu

You can define a context-menu inside the grid to help the user do actions with the selected rows. See [context menu](context-menu.md).

<img alt="GridContextMenu" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

## Variables

The grid component sends some specific variables to the server, depending on its status and attributes. 
Here is the list of variables that can be sent by the grid component:

| Variable                        | Type        | Condition                            | Description                                                                                                                                |
|---------------------------------|-------------|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| _[GridName]_                    | Array       | Always                               | List of selected lines identifiers                                                                                                         |
| _[GridName]_-id                 | Array       | Always                               | List of row identifiers for rows with operations                                                                                           |
| _[ColumnName]_                  | Array       | `send-all="false"`                   | List of selected lines column values (for each column)                                                                                     |
| _[ColumnName]_                  | Array       | `send-all="true"`                    | List of all column values (for each column)                                                                                                |
| _[ColumnName]_.selected         | Value/Array | Always                               | List of selected lines column values (for each column). If there is only one row selected, it will send only the value instead of an array |
| _[GridName]_.selectedRowAddress | JsonNode    | When there is only one line selected | Json node with the `address` of the selected row: view, component and row id                                                               |
| sort                            | Array       | When sorting                         | List of JsonNodes with the grid sort information (`id`: column id, `direction`: sort direction)                                            |
| _[GridName]_.data               | JsonNode    | Always                               | List of grid extra information (page, max records, sort information, etc)                                                                  |

The multioperation grid sends the variables in a different way than the other grids:

| Variable                        | Type        | Condition                            | Description                                                                                                                                |
|---------------------------------|-------------|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| _[GridName]_                    | Integer     | `multioperation="true"`              | Number of sent operations                                                                                                                  |
| _[GridName]_.RowTyp             | Array       | `multioperation="true"`              | List of operations required for each column (only for the rows with operations)                                                            |
| _[GridName]_-id                 | Array       | Always                               | List of row identifiers for rows with operations                                                                                           |
| _[ColumnName]_                  | Array       | `multioperation="true"`              | List of column values for rows with operations (for each column)                                                                           |
| _[ColumnName]_.selected         | Value/Array | Always                               | List of selected lines column values (for each column). If there is only one row selected, it will send only the value instead of an array |
| _[GridName]_.selectedRowAddress | JsonNode    | When there is only one line selected | Json node with the `address` of the selected row: view, component and row id                                                               |
| sort                            | Array       | When sorting                         | List of JsonNodes with the grid sort information (`id`: column id, `direction`: sort direction)                                            |
| _[GridName]_.data               | JsonNode    | Always                               | List of grid extra information (page, max records, sort information, etc)                                                                  |

> **Note:** See samples of usages on [Service Definition](service-definition.md#load-beans-from-parameters)
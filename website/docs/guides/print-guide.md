---
id: print-engine
title: Print engine
sidebar_label: Print engine
---

AWE includes a generic printing engine that allows generating the content of a screen in PDF, Excel and DOC formats.

## Printing a screen

To enable printing in a screen, include the generic AWE printing dialog:

```xml
<include target-screen="PrnOpt" target-source="center" />
```

Then add a button that opens the included dialog:

<img alt="Print button" src={require('@docusaurus/useBaseUrl').default('img/Boton.png')} />

Once these two steps are completed, the user can open the printing dialog and choose the output format and options:

<img alt="Print dialog" src={require('@docusaurus/useBaseUrl').default('img/DialogImagePrint.png')} />

## Configure title

By default, the title of a printed report is generated with the pattern:

```text
report title : report subtitle
```

The values used for the title and subtitle are taken from the labels defined in the screen, such as the screen label, tabcontainer label, window label or grid label.

The following example shows a screen before printing:

<img alt="Example screen before printing" src={require('@docusaurus/useBaseUrl').default('img/print-01-screen-default.png')} />

If no additional configuration is applied, AWE uses the existing screen labels to build the printed title.

The printed result for the previous screen is the following:

<img alt="Printed output with default title" src={require('@docusaurus/useBaseUrl').default('img/print-02-output-default-title.png')} />

You can customize this behavior in the following ways:

- **Default behavior**: if you do not add any additional configuration, AWE uses the labels already defined in the screen.
- **Custom subtitle**: define the `label` attribute in the corresponding `tabcontainer` and use a locale entry if needed.
- **Remove the main title**: set the screen `label` to an empty value if you want the printed report to show only the subtitle.

For example, after defining a custom subtitle in the tabcontainer label, the screen configuration looks like this:

<img alt="Example screen with custom subtitle" src={require('@docusaurus/useBaseUrl').default('img/print-03-screen-with-subtitle.png')} />

In short, the printed title is controlled by the same labels used in the screen structure, so title customization is done through normal screen configuration.

## Configure printing data

Since AWE 3.2, the printing engine uses the data that is currently available in the client and sends it to the printing engine.

This is important when a grid uses server-side pagination (`load-all="false"`). In that case, the client only has the rows currently loaded in the page, so the printed document will only include that data.

If you need to print the complete result set, the simplest option is to use local pagination:

```xml
load-all="true"
```

This makes all rows available in the client, so the printing engine can include the full dataset.

:::warning
This approach can cause serious performance problems if the query returns a large number of rows.
:::

If you need to keep server-side pagination and still print the full dataset, you should implement a project-specific printing flow that reloads the required data before generating the document. That scenario depends on the application and is outside the scope of this generic guide.


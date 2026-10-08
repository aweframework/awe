---
id: xsd-tooling
title: XSD tooling and IDE autocompletion
sidebar_label: XSD tooling
---

Every AWE XML definition (screens, menus, queries, maintains, locales...) is described by an XSD.
Pointing your XML to its schema gives you autocompletion, inline documentation and validation in the
IDE, and the same schemas are used by the build (`xml-maven-plugin`) and by the
[XML hot reload](../dev-tools.md#xml-hot-reload-backend-definitions) to validate your files.

## Published schemas

The schemas are published with the documentation site, next to an OASIS XML catalog that maps each URL
to its file:

| URL | Use it for |
| --- | --- |
| `https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd` | Screens |
| `https://aweframework.gitlab.io/awe/docs/schemas/menu.xsd` | Menus (`menu/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd` | Locales (`locale/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/profile.xsd` | Profiles (`profile/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/queries.xsd` | `global/Queries.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/maintain.xsd` | `global/Maintain.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/services.xsd` | `global/Services.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/enumerated.xsd` | `global/Enumerated.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/queues.xsd` | `global/Queues.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/email.xsd` | `global/Email.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/actions.xsd` | `global/Actions.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/access.xsd` | Access definitions (users and groups) |
| `https://aweframework.gitlab.io/awe/docs/schemas/catalog.xml` | OASIS catalog for all of the above |

`general.xsd`, `query.xsd` and `variable.xsd` are shared building blocks included by the other schemas: you do not
reference them from your XML files.

## Reference the schema in your XML

AWE schemas have no target namespace, so the file declares its schema with `xsi:noNamespaceSchemaLocation`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd"
        label="SCREEN_TITLE_HELLO" template="window">
  ...
</screen>
```

Use the schema that matches the root element of the file (see the table above).

## IntelliJ IDEA

IntelliJ reads `xsi:noNamespaceSchemaLocation` by itself. With network access it can download the schema from
the URL (place the caret on the URL and press `Alt+Enter` to see the available actions).

To work offline, or to use the exact schemas of the AWE version you depend on, map the URL to a local file:

1. Get the schemas, see [Offline use](#offline-use).
2. Open **Settings | Languages & Frameworks | Schemas and DTDs** and add an entry under
   **External Schemas and DTDs** (`Add` or `Alt+Insert`): the URI is the schema URL, the location is the
   local `.xsd` file. You can also press `Alt+Enter` on the URL and choose **Manually setup external resource**.
3. Repeat for each schema you use. Entries can be shared by all projects or kept in the current project
   (stored in `.idea/misc.xml`).

## Visual Studio Code

Install the [XML extension by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-xml),
which validates and completes XML using the schema declared in each file. With network access it works
out of the box.

To resolve the schemas locally, register the catalog in your workspace settings
(`.vscode/settings.json`). `xml.catalogs` takes a list of catalog files:

```json
{
  "xml.catalogs": [
    ".awe-schemas/schemas/awe/catalog.xml"
  ]
}
```

If a file does not declare its schema, bind it with `xml.fileAssociations` (`pattern` is a glob, `systemId` the
schema location, a local path or a URL):

```json
{
  "xml.fileAssociations": [
    {
      "pattern": "**/application/**/screen/**/*.xml",
      "systemId": "https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd"
    }
  ]
}
```

## Offline use

The `awe-generic-screens` jar ships the same XSDs and `catalog.xml` under `schemas/awe/`. The `uri` entries of the
catalog are relative (`./screen.xsd`), so the catalog and the XSDs must stay in the same folder. Extract them
in your project folder, and again when you change the AWE version:

```bash
unzip awe-generic-screens-<version>.jar 'schemas/awe/*' -d .awe-schemas
# catalog: .awe-schemas/schemas/awe/catalog.xml (add .awe-schemas/ to .gitignore)
```

The build does the same: it unpacks the schemas into `target/schemas/awe/` of each module and validates the
application XML with that catalog, so the build never needs the network for validation. In a Maven project you can
point your IDE to `target/schemas/awe/catalog.xml` after the first build.

## Troubleshooting

- **No completion**: the file has no `xsi:noNamespaceSchemaLocation`, or the URL does not match the root element
  (a `<menu>` file needs `menu.xsd`). Compare the root element with the table above.
- **Unknown schema or "failed to read schema document"**: no network or a proxy blocks `aweframework.gitlab.io`.
  Use the offline mapping or catalog above.
- **Catalog ignored**: check that the `xml.catalogs` path is correct for your workspace and points to a
  `catalog.xml` that sits next to the `.xsd` files.
- **Errors that the build does not report** (or the reverse): the schema in your IDE is from a different AWE
  version than the one in your project. Extract the schemas of the version you depend on.
- **Schema looks out of date**: the published URL always holds the latest AWE schemas; use the jar of your version
  to match your project exactly.

## JSON Schema: evaluated, not adopted

We evaluated publishing JSON Schema versions of the definitions. We did not adopt it: AWE definitions are XML
and the XSDs are the single source of truth used by the build, the hot-reload validator and the IDEs, so a
generated JSON Schema would be a second artifact to keep in sync without bringing validation the XSDs do not already
provide.

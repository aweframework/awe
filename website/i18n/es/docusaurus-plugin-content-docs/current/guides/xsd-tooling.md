---
id: xsd-tooling
title: Herramientas XSD y autocompletado en el IDE
sidebar_label: Herramientas XSD
---

Cada definición XML de AWE (pantallas, menús, consultas, mantenimientos, locales...) está descrita por un XSD. Apuntar
su XML a su esquema le proporciona autocompletado, documentación en línea y validación en el IDE, y los mismos
esquemas los usa la compilación (`xml-maven-plugin`) y la
[recarga en caliente de XML](../dev-tools.md#xml-hot-reload-backend-definitions) para validar sus archivos.

## Esquemas publicados {#published-schemas}

Los esquemas se publican con el sitio de documentación, junto a un catálogo XML de OASIS que asocia cada URL con su
archivo:

| URL | Úselo para |
| --- | --- |
| `https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd` | Pantallas |
| `https://aweframework.gitlab.io/awe/docs/schemas/menu.xsd` | Menús (`menu/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd` | Locales (`locale/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/profile.xsd` | Perfiles (`profile/*.xml`) |
| `https://aweframework.gitlab.io/awe/docs/schemas/queries.xsd` | `global/Queries.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/maintain.xsd` | `global/Maintain.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/services.xsd` | `global/Services.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/enumerated.xsd` | `global/Enumerated.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/queues.xsd` | `global/Queues.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/email.xsd` | `global/Email.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/actions.xsd` | `global/Actions.xml` |
| `https://aweframework.gitlab.io/awe/docs/schemas/access.xsd` | Definiciones de acceso (usuarios y grupos) |
| `https://aweframework.gitlab.io/awe/docs/schemas/catalog.xml` | Catálogo OASIS de todos los anteriores |

`general.xsd`, `query.xsd` y `variable.xsd` son bloques compartidos incluidos por los demás esquemas: no se referencian
desde sus archivos XML.

Cada elemento y atributo de estos esquemas, con su tipo, valores permitidos y dónde se puede usar, está listado en la
[referencia XSD](/reference/) generada.

## Referenciar el esquema en su XML {#reference-the-schema-in-your-xml}

Los esquemas de AWE no tienen espacio de nombres de destino, por lo que el archivo declara su esquema con
`xsi:noNamespaceSchemaLocation`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd"
        label="SCREEN_TITLE_HELLO" template="window">
  ...
</screen>
```

Use el esquema que corresponda al elemento raíz del archivo (consulte la tabla anterior).

## IntelliJ IDEA {#intellij-idea}

IntelliJ lee `xsi:noNamespaceSchemaLocation` por sí mismo. Con acceso a la red puede descargar el esquema desde la URL
(coloque el cursor sobre la URL y pulse `Alt+Enter` para ver las acciones disponibles).

Para trabajar sin conexión, o para usar los esquemas exactos de la versión de AWE de la que depende, asocie la URL con
un archivo local:

1. Obtenga los esquemas, consulte [Uso sin conexión](#offline-use).
2. Abra **Settings | Languages & Frameworks | Schemas and DTDs** y añada una entrada en
   **External Schemas and DTDs** (`Add` o `Alt+Insert`): el URI es la URL del esquema, la ubicación es el archivo
   `.xsd` local. También puede pulsar `Alt+Enter` sobre la URL y elegir **Manually setup external resource**.
3. Repita para cada esquema que use. Las entradas pueden compartirse entre todos los proyectos o mantenerse en el
   proyecto actual (se guardan en `.idea/misc.xml`).

## Visual Studio Code {#visual-studio-code}

Instale la [extensión XML de Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-xml), que
valida y completa XML usando el esquema declarado en cada archivo. Con acceso a la red funciona directamente.

Para resolver los esquemas en local, registre el catálogo en los ajustes de su espacio de trabajo
(`.vscode/settings.json`). `xml.catalogs` recibe una lista de archivos de catálogo:

```json
{
  "xml.catalogs": [
    ".awe-schemas/schemas/awe/catalog.xml"
  ]
}
```

Si un archivo no declara su esquema, asócielo con `xml.fileAssociations` (`pattern` es un glob, `systemId` la ubicación
del esquema, una ruta local o una URL):

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

## Uso sin conexión {#offline-use}

El jar `awe-generic-screens` incluye los mismos XSD y `catalog.xml` bajo `schemas/awe/`. Las entradas `uri` del catálogo
son relativas (`./screen.xsd`), por lo que el catálogo y los XSD deben permanecer en la misma carpeta. Extráigalos en la
carpeta de su proyecto, y de nuevo cuando cambie la versión de AWE:

```bash
unzip awe-generic-screens-<version>.jar 'schemas/awe/*' -d .awe-schemas
# catalog: .awe-schemas/schemas/awe/catalog.xml (add .awe-schemas/ to .gitignore)
```

La compilación hace lo mismo: desempaqueta los esquemas en `target/schemas/awe/` de cada módulo y valida el XML de la
aplicación con ese catálogo, por lo que la compilación nunca necesita la red para validar. En un proyecto Maven puede
apuntar su IDE a `target/schemas/awe/catalog.xml` después de la primera compilación.

## Resolución de problemas {#troubleshooting}

- **Sin autocompletado**: el archivo no tiene `xsi:noNamespaceSchemaLocation`, o la URL no coincide con el elemento
  raíz (un archivo `<menu>` necesita `menu.xsd`). Compare el elemento raíz con la tabla anterior.
- **Esquema desconocido o «failed to read schema document»**: no hay red o un proxy bloquea `aweframework.gitlab.io`.
  Use la asociación sin conexión o el catálogo descritos antes.
- **Catálogo ignorado**: compruebe que la ruta de `xml.catalogs` es correcta para su espacio de trabajo y apunta a un
  `catalog.xml` que esté junto a los archivos `.xsd`.
- **Errores que la compilación no informa** (o al revés): el esquema de su IDE es de una versión de AWE distinta a la
  de su proyecto. Extraiga los esquemas de la versión de la que depende.
- **El esquema parece desactualizado**: la URL publicada siempre contiene los últimos esquemas de AWE; use el jar de su
  versión para que coincida exactamente con su proyecto.

## Comprobaciones al cambiar un esquema {#checks-when-you-change-a-schema}

Los XSD están en `awe-framework/awe-generic-screens/src/main/resources/schemas/awe`. Cuatro comprobaciones los protegen; ninguna es un job
propio, se ejecutan dentro de los jobs normales de pruebas y compilación:

| Comprobación | Qué verifica | Cómo ejecutarla en local |
|---|---|---|
| `XsdCatalogConsistencyTest` (prueba unitaria de `awe-generic-screens`) | Los system id del catálogo son únicos y son las URL publicadas, cada XSD tiene una entrada en el catálogo, cada entrada apunta a un archivo existente y cada `schemaLocation` se resuelve a través del catálogo | `mvn test -pl awe-framework/awe-generic-screens -Dtest=XsdCatalogConsistencyTest` |
| `validate` de `xml-maven-plugin` (fase `compile`) | El XML de aplicación de los módulos que lo configuran (`awe-boot`, `awe-boot-react`, `awe-scheduler-boot`, los módulos de AWE...) es válido frente a los esquemas, resueltos con el catálogo desempaquetado en `target/schemas/awe/` | `mvn compile -pl awe-tests/awe-scheduler-boot -Dskip.frontend=true` |
| Pruebas unitarias del generador de la referencia XSD (`website/scripts/xsd-reference`) | El generador que produce la [referencia XSD](/reference/) a partir de los esquemas | `npm --prefix website run test:scripts` (también forma parte del job `Build · Website`) |
| Umbral de documentación del generador de la referencia XSD (`website/scripts/xsd-reference/thresholds.json`) | Ningún esquema tiene más atributos sin documentar que su umbral (`maxUndocumented`, 0 para un esquema que no aparece); si no, la compilación falla y los enumera | `npm --prefix website run generate:reference` (se ejecuta antes de cada compilación del sitio, también en `Build · Website`) |

Documenta cada atributo nuevo con un `xs:annotation/xs:documentation`. Los umbrales solo bajan: cuando documentas
atributos, el generador muestra el valor más bajo y lo pones en `thresholds.json` en el mismo cambio.

Cuando añadas un XSD, añade su entrada a `catalog.xml` (la prueba falla en caso contrario) y su fila a la tabla de
[esquemas publicados](#published-schemas). El pipeline publica los XSD y el catálogo en el sitio desde `develop` y
`master` (job `pages`).

## JSON Schema: evaluado, no adoptado {#json-schema-evaluated-not-adopted}

Evaluamos publicar versiones en JSON Schema de las definiciones. No lo adoptamos: las definiciones de AWE son XML y los
XSD son la única fuente de verdad que usan la compilación, el validador de recarga en caliente y los IDE, por lo que un
JSON Schema generado sería un segundo artefacto que mantener sincronizado sin aportar una validación que los XSD no
proporcionen ya.

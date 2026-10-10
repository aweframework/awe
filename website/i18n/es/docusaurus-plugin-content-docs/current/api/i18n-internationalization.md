---
id: i18n-internationalization
Title: I18N (internacionalización)
sidebar_label: 'i18n: Internacionalización'
---

AWE implementa un sistema i18n para la internacionalización de aplicaciones web. Por esta razón, AWE utiliza archivos locales que contienen literales en diferentes idiomas.

:::tip
Todos los elementos y atributos de los locales se enumeran en la [referencia XSD](/reference/locale) generada.
:::

También puedes añadir la etiqueta `CDATA` dentro del idioma local en **markdown** para mostrar texto enriquecido. Muy útil cuando quiera mostrar mucha información de formato. Por ejemplo, en la aplicación de pantalla de ayuda. Puedes ver toda la sintaxis de markdown en [esta página](https://wiki.almis.com/help/markdown/markdown).

:::info
**Nota:** Todos los locales están definidos en los archivos `Locale-[Locale code].xml` en la **carpeta de locales**. Los códigos de país deben seguir los códigos ISO 3166 e ISO 639 (es-ES, en-GB, fr-FR...). Ver la [estructura de proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## Estructura XML de locales {#locales-xml-structure}

La estructura completa de locales es la siguiente:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<locales xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd">
  <locale name="[locale-name]" value=[locale-value]" />
    <![CDATA[ Markdown content ]]>
  <locale name="[locale-name]" value=[locale-value]">
  </local>
  ... (More <locale>)
</locales>
```

### Estructura de locales {#locales-structure}


| Elemento                 | Uso             | Varias instancias | Descripción                                    |
| ------------------------ | --------------- | ----------------- | ---------------------------------------------- |
| locales                  | **Obligatorio** | No                | Nodo raíz de la estructura de locales          |
| [locale](#locale-element) | **Obligatorio** | Si                | Utilizado para definir una traducción regional |


### Elemento local {#locale-element}

El elemento local tiene los siguientes atributos:

| Atributo | Uso             | Tipo   | Descripción                                                  | Valores                                                                      |
| -------- | --------------- | ------ | ------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| name     | **Obligatorio** | String | El nombre del local                                          |                                                                              |
| value    | Opcional        | String | El valor de la localidad, la traducción del idioma del texto | **Nota:** Puede establecer valores como variables con la sintaxis {0} {1}... |
| markdown | Opcional        | String | El valor de la localidad. Se traducirá como markdown         |                                                                              |

## Ejemplos {#examples}

Algunos ejemplos de locales en diferentes idiomas:



**Archivo Locale-en-GB.xml**
```xml
<locales xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd">
  <locale name="BUTTON_ACCEPT" value="Accept" />
  <locale name="CONFIRM_MESSAGE_DELETE" value="You will delete the selected records. Do you agree?" />
  <locale name="ERROR_MESSAGE_BAD_QUEUE_REQUEST_DEFINITION_FORMAT" value="Bad request definition format for queue {0}" />
  ...
</locales>
```



**Archivo Locale-es-ES.xml**
```xml
<locales xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd">
  <locale name="BUTTON_ACCEPT" value="Aceptar" />
  <locale name="CONFIRM_MESSAGE_DELETE" value="Vas a borrar el registro seleccionado. ¿Estás de acuerdo?" />
  <locale name="ERROR_MESSAGE_BAD_QUEUE_REQUEST_DEFINITION_FORMAT" value="El formato de la petición a la cola {0} es erróneo" />
  ...
</locales>
```



**Archivo Locale-fr-FR.xml**
```xml
<locales xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/locale.xsd">
  <locale name="BUTTON_ACCEPT" value="Accepter" />
  <locale name="CONFIRM_MESSAGE_DELETE" value="Donnees selectionees vont etre effacees. Etes vous d&apos;accord?" />
  <locale name="ERROR_MESSAGE_BAD_QUEUE_REQUEST_DEFINITION_FORMAT" value="Le format du message pour l&apos;envoy à la queue {0} n&apos;a pas été définie" />
  ...
</locales>
```



**Ejemplo regional con markdown CDATA usado en los criterios de texto de ayuda**
```xml
<locale name="HELP_SCREEN_TITLE_SIT">
<![CDATA[
Manage application sites. Here you can view, add, delete and modify all application sites.

A site is defined as a *separated application node*, with it's own databases and modules.
]]>
</locale>
```
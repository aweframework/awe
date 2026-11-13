---
id: email
title: Definición de email
sidebar_label: Definición de email
---

El motor de email es la herramienta que proporciona AWE para definir estructuras de email con los parámetros y variables de la aplicación.

:::tip
Cada elemento y atributo de los emails está listado en la [referencia XSD](/reference/email) generada.
:::

Los emails se envían mediante la operación `send-email` dentro de un `target` de los [maintains](maintain-definition.md#email-maintain).

:::info
**Nota:** Todos los emails se definen en el archivo `Email.xml` de la **carpeta global**. Consulta la [estructura de proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## Estructura XML del email {#xml-email-structure}

La estructura del email es la siguiente:

```xml
  <email id="[email_name]">
    <from query="[query_to_retrieve_data]" label="[label_column_on_query]" value="[value_column_on_query]" />
    <to query="[query_to_retrieve_data]" label="[label_column_on_query]" value="[value_column_on_query]" />
    <cc query="[query_to_retrieve_data]" label="[label_column_on_query]" value="[value_column_on_query]" />
    <cco query="[query_to_retrieve_data]" label="[label_column_on_query]" value="[value_column_on_query]" />
    <subject label="[locale_with_subject]" />
    <body label="EMAIL_MESSAGE_HTML_HEADER" type="html" />
    <body label="[locale_with_body_in_html]" type="html" />
    <body label="EMAIL_MESSAGE_HTML_BOTTOM" type="html" />
    <body label="[locale_with_body_in_plain_text]" type="text" />
    <attachment value="PdfNam" label="EMAIL_FILE_PDF" />
    <attachment value="DocNam" label="EMAIL_FILE_DOC" />
    <attachment value="XlsNam" label="EMAIL_FILE_XLS" />
    <attachment value="CsvNam" label="EMAIL_FILE_CSV" />
    <attachment value="TxtNam" label="EMAIL_FILE_TXT" />
    <variable id="ScrTit" type="STRING" name="ScrTit"/>
    <variable id="ScrTitFil" type="STRING" name="ScrTitFil"/>
    <variable id="PdfNam" type="STRING" name="PdfNam" optional="true"/>
    <variable id="DocNam" type="STRING" name="DocNam" optional="true"/>
    <variable id="XlsNam" type="STRING" name="XlsNam" optional="true"/>
    <variable id="CsvNam" type="STRING" name="CsvNam" optional="true"/>
    <variable id="TxtNam" type="STRING" name="TxtNam" optional="true"/>
  </email>
```

| Elemento                                            | Uso           | Varias instancias   | Descripción                                                |
|-----------------------------------------------------|---------------|---------------------|------------------------------------------------------------|
| email                                               | **Obligatorio** | No                | Describe el nombre del email                               |
| from                                                | **Obligatorio** | No                | Origen del email                                           |
| to                                                  | **Obligatorio** | No                | Destino del email                                          |
| cc                                                  | Opcional      | No                  | Destino de copia                                           |
| cco                                                 | Opcional      | No                  | Destino de copia oculta                                    |
| subject                                             | **Obligatorio** | No                | Título del email                                           |
| body                                                | **Obligatorio** | Si                | Cuerpo del email                                           |
| attachment                                          | Opcional      | Si                  | Adjuntos del email                                         |
| [variable](maintain-definition.md#variable-element) | Opcional      | Si                  | Son los parámetros pasados al email y los [comodines](#wildcards) |

## Comodines {#wildcards}

Para permitir múltiples posibilidades al generar emails, existe un formato de comodín que permite insertar valores de variables en los locales.

El formato del comodín es el siguiente:

```
[#VariableId#]
```

Por ejemplo, en el siguiente locale:

```xml
<locale name="EMAIL_FILE_PDF" value="[#ScrTitFil#].pdf"/>
```

`ScrTitFil` se sustituirá por la variable con id `ScrTitFil`.

## Variables especiales {#special-variables}

Existe una variable llamada `user` que, cuando se envía al motor de email, se utiliza para buscar el servidor de email
de ese usuario y enviar el email a través de él. Si no está definida, se utilizará el usuario de la sesión para buscar el servidor
de email.

:::info
**Nota:** Si no hay sesión ni variable `user`, se utilizará el servidor de email por defecto para enviar el email.
:::

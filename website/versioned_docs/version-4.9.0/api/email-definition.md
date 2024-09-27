---
id: email
title: Email definition
sidebar_label: Email definition
---

The email engine is the tool AWE provides to define email structures with the application parameters and variables.

Emails are sent using the `send-email` operation inside a `target` from [maintains](maintain-definition.md#email-maintain).

:::info
**Note:** All emails are defined in the `Email.xml` file at **global folder**. View [project structure](../guides/project-structure.md#global-folder) for more info.
:::

## XML email structure

The email structure is the following:

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

| Element                                             | Use           | Multiples instances | Description                                                |
|-----------------------------------------------------|---------------|---------------------|------------------------------------------------------------|
| email                                               | **Required**  | No                  | It describe the name of email                              |
| from                                                | **Required*   | No                  | Source of the email                                        |
| to                                                  | **Required**  | No                  | Target destination of the email                            |
| cc                                                  | Optional      | No                  | Target copy                                                |
| cco                                                 | Optional      | No                  | Target hidden copy                                         |
| subject                                             | **Required**  | No                  | Email title                                                |
| body                                                | **Required**  | Yes                 | Email body                                                 |
| attachment                                          | Optional      | Yes                 | Email attachments                                          |
| [variable](maintain-definition.md#variable-element) | Optional      | Yes                 | Are parameters passed to email and [wildcards](#wildcards) |

## Wildcards

To allow multiple possibilities when generating emails, there is a wildcard format to allow inserting variable values into locales.

The wildcard format is the following:

```
[#VariableId#]
```

For example, in the following locale:

```xml
<locale name="EMAIL_FILE_PDF" value="[#ScrTitFil#].pdf"/>
```

`ScrTitFil` will be replaced with the variable with id `ScrTitFil`.

## Special variables

There is a variable called `user` that, when it's sent to the email engine, it will use that variable to find the email 
server for that user and use it to send the email. If it's not defined, the session user will be used to find the email 
server.

:::info
**Note:** If there is no session and no `user` variable, then the default email server will be used to send the email.
:::
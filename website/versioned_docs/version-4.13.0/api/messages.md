---
id: messages
title: Messages
---

Message elements define the messages that are shown after executing an action. They are usually declared in the hidden section of the window.

This element is usually referenced from the `target` attribute of a button action.

<img alt="Dialog displayed when a message is triggered (confirm dialog)" src={require('@docusaurus/useBaseUrl').default('img/Messages.png')} />

## XML structure

The XML structure of a message element is the following:

```xml
  <tag source="hidden">
    <message id="[id]" title="[message-title]" message="[message-text]" />
    ... more messages ...
  </tag>
```

## Message attributes

| Name |  Type | Use | Description     | Values |
| ------ | -------| ---------------------- | ----------------------------------|---------------------------------------- |
|`id`| String | **Required**| Message identifier | |
|`title`| String | **Required**| Message title | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
|`message`| String | **Required**| Content of message  | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)  |

## Examples

- Show a confirmation message before inserting new data

```xml
...
<tag source="hidden">
  <message id="NewMsg" title="CONFIRM_TITLE_NEW" message="CONFIRM_MESSAGE_NEW" />
</tag>
...
<button button-type="button" label="BUTTON_CONFIRM" icon="save" id="ButCnf" help="HELP_CONFIRM_BUTTON">
  <button-action type="confirm" target="NewMsg" />
  <button-action type="server" server-action="maintain" target-action="UsrNew" />
</button>
```

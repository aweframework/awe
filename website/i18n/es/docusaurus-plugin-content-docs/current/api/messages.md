---
id: messages
title: Mensajes
---

Los elementos de mensaje definen los mensajes que se muestran tras ejecutar una acción. Normalmente se declaran en la sección oculta de la ventana.

Este elemento suele referenciarse desde el atributo `target` de una acción de botón.

<img alt="Diálogo mostrado al lanzar un mensaje (diálogo de confirmación)" src={require('@docusaurus/useBaseUrl').default('img/Messages.png')} />

## Estructura XML {#xml-structure}

La estructura XML de un elemento de mensaje es la siguiente:

```xml
  <tag source="hidden">
    <message id="[id]" title="[message-title]" message="[message-text]" />
    ... more messages ...
  </tag>
```

## Atributos del mensaje {#message-attributes}

| Nombre | Tipo   | Uso             | Descripción               | Valores |
| ------ | ------ | --------------- | ------------------------- | ------- |
|`id`| String | **Obligatorio**| Identificador del mensaje | |
|`title`| String | **Obligatorio**| Título del mensaje | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
|`message`| String | **Obligatorio**| Contenido del mensaje  | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)  |

## Ejemplos {#examples}

- Mostrar un mensaje de confirmación antes de insertar nuevos datos

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

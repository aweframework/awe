---
id: enumerate
title: Definición de enumerados
sidebar_label: Definición de enumerados
---

Enumerated components are structures to define `label` - `value` lists. Son útiles, por ejemplo, para traducciones.

:::info
**Note:** All enumerateds are defined in the `Enumerated.xml` file at **global folder**. View [project structure](../guides/project-structure.md#global-folder)  for more info.
:::

## Estructura XML del enumerado {#enumerated-xml-structure}

Su estructura es la siguiente:

```xml
<enumerated
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:noNamespaceSchemaLocation = "https://aweframework.gitlab.io/awe/docs/schemas/enumerated.xsd">

  <group id="[Group Id]">
    <option label="[Option label]"  value="[Option value]" />
    <option label="[Option label]"  value="[Option value]" />
    ... more <option>
  </group>

  ... more <group>
</enumerated>
```

### Estructura del enumerado {#enumerated-structure}

| Elemento                  | Uso          | Varias instancias | Descripción                                               |
| ------------------------- | ------------ | ----------------- | --------------------------------------------------------- |
| enumerated                | **Required** | No                | Nodo raíz de la estructura del enumerado                  |
| [group](#group-element)   | **Required** | Si                | Utilizado para agrupar las opciones del enumerado         |
| [option](#option-element) | **Required** | Si                | Define each of the `key` - `values` of a group of options |

### Elemento de grupo {#group-element}

El elemento de grupo tiene los siguientes atributos:

| Atributo | Uso          | Tipo   | Descripción             | Valores                                               |
| -------- | ------------ | ------ | ----------------------- | ----------------------------------------------------- |
| id       | **Required** | String | Identificador del grupo | **Note:**  The id name must be unique |

### Elemento de opción {#option-element}

El elemento de opción tiene los siguientes atributos:

| Atributo | Uso          | Tipo   | Descripción              | Valores                                                                                                       |
| -------- | ------------ | ------ | ------------------------ | ------------------------------------------------------------------------------------------------------------- |
| label    | **Required** | String | La etiqueta de la opción | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| value    | **Required** | String | El valor de la opción    | **Note:**  The id name must be unique                                                         |

## Ejemplos {#examples}

Varios ejemplos de grupos de enumerados:

```xml
<!-- Enumerated YES (0) | NO (1) -->
<group id="Es1Es0">
  <option label="ENUM_NO"  value="0" />
  <option label="ENUM_YES" value="1" />
</group>
```

```xml
<!-- LANGUAGES -->
<group id="Lan">
  <option label="ENUM_LAN_ES" value="es-ES"/>
  <option label="ENUM_LAN_EN" value="en-GB"/>
  <option label="ENUM_LAN_FR" value="fr-FR"/>
</group>
```


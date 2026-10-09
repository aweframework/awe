---
id: enumerate
title: Definición de enumerados
sidebar_label: Definición de enumerados
---

Los componentes enumerados son estructuras para definir listas de `label` - `value`. Son útiles, por ejemplo, para traducciones.

:::tip Every element and attribute of enumerated lists is listed in the generated [XSD reference](/reference/enumerated). :::

:::info **Nota:** Todos los enumerados están definidas en el archivo `Enumerated.xml` en la **carpeta global**. Ver la [estructura de proyecto](../guides/project-structure.md#global-folder)  para más información. :::

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


| Elemento                      | Uso             | Varias instancias | Descripción                                                      |
| ----------------------------- | --------------- | ----------------- | ---------------------------------------------------------------- |
| enumerated                    | **Obligatorio** | No                | Nodo raíz de la estructura del enumerado                         |
| [group](#group-element)   | **Obligatorio** | Si                | Utilizado para agrupar las opciones del enumerado                |
| [option](#option-element) | **Obligatorio** | Si                | Defina cada una de las `clave` - `valor` de un grupo de opciones |

### Elemento de grupo {#group-element}

El elemento de grupo tiene los siguientes atributos:

| Atributo | Uso             | Tipo   | Descripción             | Valores                                    |
| -------- | --------------- | ------ | ----------------------- | ------------------------------------------ |
| id       | **Obligatorio** | String | Identificador del grupo | **Nota:**  El nombre del id debe ser único |

### Elemento de opción {#option-element}

El elemento de opción tiene los siguientes atributos:

| Atributo | Uso             | Tipo   | Descripción              | Valores                                                                       |
| -------- | --------------- | ------ | ------------------------ | ----------------------------------------------------------------------------- |
| label    | **Obligatorio** | String | La etiqueta de la opción | **Nota:** Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| value    | **Obligatorio** | String | El valor de la opción    | **Nota:**  El nombre del id debe ser único                                    |


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


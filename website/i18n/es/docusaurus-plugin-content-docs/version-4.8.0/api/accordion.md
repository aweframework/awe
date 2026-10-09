---
id: accordion
title: Accordion
---

Un acordeón es un componente de diseño que permite organizar el contenido en diferentes secciones que pueden ser colapsadas o ampliadas.  Dentro de sus paneles, permite tener el mismo contenido que una ventana o un elemento de etiqueta, incluso otros acordeones anidados.

<img alt="Accordion" src={require('@docusaurus/useBaseUrl').default('img/accordion4.png')} />

## Esqueleto de XML {#xml-skeleton}

```xml
<accordion id="[accordion-identifier]" ...>
  <accordion-item... />
  <dependency... />
</accordion>
```

## Estructura de acordeón {#accordion-structure}

```xml
<accordion id="[accordion-identifier]" selected="[id-selected]" autocollapse="[accordion-autocollapse]" style="[accordion-style]">
...
</accordion>
```

## Atributos de acordeón {#accordion-attributes}

| Atributo     | Uso          | Tipo   | Descripción                                                                               | Valores                                                                                                                                                             |
| ------------ | ------------ | ------ | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id           | **Required** | String | Identificador del acordeón. Con fines de referencia                       |                                                                                                                                                                     |
| autocollapse | Opcional     | String | Si se establece en "true" solo un panel del acordeón puede permanecer abierto a la vez    | `true` or `false` (default is `true`)                                                                                                            |
| selected     | Opcional     | String | Si queremos que un elemento del acordeón esté abierto al principio, establezca su ID aquí |                                                                                                                                                                     |
| style        | Opcional     | String | Clases CSS de Grid                                                                        | We can use `panel-group-[type]` and `panel-group-dark` classes to style (type can be the usual `danger`, `success`, etc. values) |

## Estructura de paneles de acordeón {#accordion-item-structure}

```xml
<accordion-item id="[accordion-item-identifier]" label="[accordion-item-label]" 
style="[accordion-item-style]">
...
</accordion-item>
```

## Atributos de paneles de acordeón {#accordion-item-attributes}

| Atributo | Uso          | Tipo   | Descripción                                                                   | Valores                                                                                                       |
| -------- | ------------ | ------ | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| id       | **Required** | String | Identificador del panel del acordeón. Con fines de referencia |                                                                                                               |
| label    | **Required** | String | Texto a colocar dentro de la cabecera del panel                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| style    | Opcional     | String | Clases CSS de Grid                                                            |                                                                                                               |

## Dependencies

If we want to collapse or expand an accordion item using a dependency, we must use one that modifies the attribute `selected` setting the ID of the item (if it's expanded it will collapse and viceversa)

```xml
<dependency target-type="attribute" target-action="selected" value="[child-id]">
...
</dependency>
```

## Ejemplos {#examples}

Below we can see the difference between having the _autocollapse_ attribute as `true` (left) or `false` (right)

<img alt="Accordion example 1" src={require('@docusaurus/useBaseUrl').default('img/accordion.png')} />

Lo siguiente es un panel de acordeón que contiene una ventana con un <code>include</code>

<img alt="Accordion example 2" src={require('@docusaurus/useBaseUrl').default('img/accordion3.png')} />

Y esto sería un ejemplo completo de estructura de acordeón

```xml
<accordion id="acc2" autocollapse="false" style="panel-group-info">
    <accordion-item id="acc2-1" label="Item 1">
        <tag>
            <text>
                "Lorem ipsum ...  laborum."
            </text>
        </tag>
    </accordion-item>
    <accordion-item id="acc2-2" label="Item 2">
        <tag>
            <text>
                "Sed ut ... pariatur?"
            </text>
        </tag>
    </accordion-item>
    <accordion-item id="acc2-3" label="Item 3">
        <tag>
            <text>
                "At vero ... repellat."
            </text>
        </tag>
    </accordion-item>
</accordion>
```


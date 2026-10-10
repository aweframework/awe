---
id: accordion
title: Acordeón
---

Un acordeón es un componente de diseño que permite organizar el contenido en diferentes secciones que pueden ser colapsadas o ampliadas.  Dentro de sus paneles, permite tener el mismo contenido que una ventana o un elemento de etiqueta, incluso otros acordeones anidados.

<img alt="Acordeón" src={require('@docusaurus/useBaseUrl').default('img/accordion4.png')} />

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

| Atributo     | Uso             | Tipo   | Descripción                                                                               | Valores                                                                                                                                                |
| ------------ | --------------- | ------ | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| id           | **Obligatorio** | String | Identificador del acordeón. Con fines de referencia                                       |                                                                                                                                                        |
| autocollapse | Opcional        | String | Si se establece en "true" solo un panel del acordeón puede permanecer abierto a la vez    | `true` o `false` (por defecto es `true`)                                                                                                               |
| selected     | Opcional        | String | Si queremos que un elemento del acordeón esté abierto al principio, establezca su ID aquí |                                                                                                                                                        |
| style        | Opcional        | String | Clases CSS                                                                                | Podemos usar clases de `panel-group-[type]` y `panel-group-dark` para definir el estilo (type puede ser uno de los valores habituales `danger`, `success`, etc.) |

## Estructura de paneles de acordeón {#accordion-item-structure}

```xml
<accordion-item id="[accordion-item-identifier]" label="[accordion-item-label]" 
style="[accordion-item-style]">
...
</accordion-item>
```

## Atributos de paneles de acordeón {#accordion-item-attributes}

| Atributo | Uso             | Tipo   | Descripción                                                   | Valores                                                              |
| -------- | --------------- | ------ | ------------------------------------------------------------- | -------------------------------------------------------------------- |
| id       | **Obligatorio** | String | Identificador del panel del acordeón. Con fines de referencia |                                                                      |
| label    | **Obligatorio** | String | Texto a colocar dentro de la cabecera del panel               | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md) |
| style    | Opcional        | String | Clases CSS                                                    |                                                                      |

## Dependencias {#dependencies}

Si queremos colapsar o expandir un elemento de acordeón usando una dependencia, debemos usar una que modifique el atributo `selected` estableciendo el ID del elemento (si está expandido se cerrará y viceversa)

```xml
<dependency target-type="attribute" target-action="selected" value="[child-id]">
...
</dependency>
```

## Ejemplos {#examples}

A continuación podemos ver la diferencia entre tener el atributo *autocollapse* como `true` (izquierda) o `false` (derecha)

<img alt="Ejemplo de acordeón 1" src={require('@docusaurus/useBaseUrl').default('img/accordion.png')} />

A continuación, un panel de acordeón que contiene una ventana con un include

<img alt="Ejemplo de acordeón 2" src={require('@docusaurus/useBaseUrl').default('img/accordion3.png')} />

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


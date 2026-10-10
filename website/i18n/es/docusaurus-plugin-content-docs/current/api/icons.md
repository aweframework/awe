---
id: icons
Title: Iconos
sidebar_label: Iconos
---

AWE incluye varias familias de iconos para usar en componentes y grids.

Basta con buscar un icono que se ajuste al concepto que se quiere mostrar e indicar el nombre del
icono en el atributo `icon` (o el valor, en el caso de las columnas).

Para admitir más familias de iconos, la nueva estructura del nombre de icono es la siguiente:

```
family:icon_name
```

Donde family es el nombre de la familia de iconos descrita a continuación

## Iconos Font Awesome 4.7.0 (fa) {#font-awesome-470-icons-fa}

La familia de iconos por defecto (heredada de la versión 3 de AWE) es Font Awesome:

[Iconos Font Awesome 4.7.0](https://fontawesome.com/v4/icons/)

Aquí tienes un conjunto de 675 iconos con marcas, aplicaciones web, flechas y muchos
conceptos muy útiles para poner en opciones o botones

Para usar estos iconos, basta con indicar el nombre del icono en el atributo `icon` sin
ninguna descripción de familia:

```
icon="check"
```

o, si lo prefieres, puedes indicar el nombre de la familia:

```
icon="fa:check"
```

## Material Design Icons (mdi) {#material-design-icons-mdi}

Los iconos de Google Material Design son un conjunto de iconos muy famoso y usado en todo el mundo, basado
en el tema Google Material Design.

[Iconos Material Design](https://fonts.google.com/icons?icon.style=Filled&icon.set=Material+Icons)

Para usar esta familia, indica el nombre de la familia `mdi` antes del nombre del icono:

```
icon="mdi:home"
```

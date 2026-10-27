---
id: overview
title: Sistema de diseño de AWE 5
sidebar_label: Visión general
---

:::caution Diseño propuesto
Esta sección describe el sistema de diseño **propuesto** para AWE 5 (issue #883). La implementación en los motores
AngularJS y React está pendiente. Hasta entonces, la referencia es el
[prototipo en vivo](pathname:///design-system/prototype/), y los tokens actuales están documentados en
[Design tokens](../guides/design-tokens-guide.md).
:::

AWE 5 estrena lenguaje visual: tokens, tipografía, densidad, iconos, estructura de la aplicación y componentes nuevos.
Un único sistema de diseño y un único contrato de tokens se aplican a **los dos** motores, de modo que una pantalla
definida en XML se ve igual en AngularJS y en React.

## Camino rápido {#quick-path}

1. Abra el [prototipo](pathname:///design-system/prototype/) y cambie el tema con el botón **Theme**.
2. Lea [Color y temas](color-and-themes.md) para entender los niveles de tokens.
3. Antes de escribir CSS, lea [Para desarrolladores](for-developers.md).

## Objetivos {#goals}

| Objetivo | Qué significa |
|----------|---------------|
| Un producto nuevo | AWE 5 debe parecer un producto nuevo, no un AWE 4 con otros colores: cambian tipografía, espaciado, formas, iconos, estructura y componentes. |
| Los dos motores a la par | Un contrato de tokens (`--awe-*`) y las mismas reglas de componentes para AngularJS y React. |
| Claro y oscuro | Cada token de color tiene un valor claro y uno oscuro. Elige el usuario o, si no elige, el sistema operativo. |
| Temas sencillos | Un tema es una paleta **primaria** más una paleta de **superficie**. No hay nada más que configurar. |
| Accesible | El texto cumple WCAG 2.2 AA (4,5:1) en los dos modos y con cualquier primario. El foco siempre es visible. |

## Principios {#principles}

- **Solo tokens.** Los componentes leen `var(--awe-*)`. Sin colores literales ni tamaños fuera de las escalas.
- **El contenido primero.** Superficies neutras, un único color de acento y el color reservado para el significado
  (estado, selección, acción).
- **Densidad tranquila.** Cómoda por defecto, compacta a demanda y la misma disposición en las dos.
- **El mismo XML, otra presentación.** Pantallas, menús y acciones conservan su XML. Solo cambia cómo se pintan.
- **Estados previsibles.** Cada componente define hover, foco, activo, deshabilitado, inválido, cargando y vacío.

## Secciones {#sections}

| Página | Léala cuando |
|--------|--------------|
| [Color y temas](color-and-themes.md) | Necesite un color, un tema o el modelo claro/oscuro. |
| [Fundamentos](foundations.md) | Necesite tipografía, espaciado, densidad, radios, elevación, movimiento o iconos. |
| [Estructura de la aplicación](layout-and-shell.md) | Trabaje en el marco de la aplicación, el menú, la cabecera o la página de login. |
| [Componentes](components.md) | Construya o estilice un componente, o necesite las reglas de un elemento de AWE. |
| [Referencia de tokens](tokens-reference.md) | Necesite el nombre o el valor exacto de un token, o la correspondencia con los tokens actuales. |
| [Para desarrolladores](for-developers.md) | Escriba CSS, añada un componente o configure el tema de un producto. |

## Prototipo en vivo {#live-prototype}

El prototipo es HTML y CSS estático construido sobre los tokens propuestos. Usa un cliente ficticio, *Veloria
Logistics*.

| Página | Muestra |
|--------|---------|
| [Pantalla de ejemplo](pathname:///design-system/prototype/) | Estructura, criterios, grid, pestañas, acordeón, diálogos, toasts. |
| [Componentes de AWE 5](pathname:///design-system/prototype/components.html) | Los elementos nuevos de AWE 5, agrupados y con su número de issue. |
| [Login](pathname:///design-system/prototype/login.html) | Página de acceso y sus estados. |
| [Design tokens](pathname:///design-system/prototype/tokens.html) | Paletas, colores semánticos en los dos modos, tipografía, espaciado, radios, elevación. |

El botón **Theme** de cada página solo cambia atributos de `<html>`: modo, primario, superficie, densidad y dos
comparaciones exclusivas del prototipo (logo del cliente e imagen del login).

## Decisiones de esta sección {#decisions-behind-this-section}

| Decisión | Elección |
|----------|----------|
| Arquitectura de tokens | Tres niveles: primitivo, semántico y de componente. Ver [Color y temas](color-and-themes.md). |
| Tipografía | Inter. |
| Densidad | Dos densidades: cómoda (14px, por defecto) y compacta (13px). |
| Iconos | Iconos de línea Lucide en los dos motores; los nombres `fa-*` del XML siguen funcionando mediante una tabla de equivalencias. |
| Estructura | Nueva: menú lateral plegable, cabecera clara con búsqueda y usuario, breadcrumb y contenido en tarjetas. |
| Licencias | PrimeReact se mantiene en 10.9.9 (MIT). No se usa ningún paquete con licencia PrimeUI. |

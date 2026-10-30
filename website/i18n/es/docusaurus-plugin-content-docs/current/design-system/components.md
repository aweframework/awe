---
id: components
title: Componentes
sidebar_label: Componentes
---

Reglas por elemento de AWE. Cada componente lee solo tokens semánticos o de componente, define todos sus estados (hover,
foco, activo, deshabilitado, inválido, cargando, vacío) y funciona en las dos densidades y en los dos modos. La
[pantalla de ejemplo](pathname:///design-system/prototype/) y la
[página de componentes de AWE 5](pathname:///design-system/prototype/components.html) los muestran en vivo.

## Botones {#buttons}

| Variante | Uso |
|----------|-----|
| Primario | La acción principal de una pantalla, tarjeta o diálogo. **Una** por zona. |
| Secundario | Acciones frecuentes junto a la primaria (Importar, Restablecer). |
| Con borde | Acciones neutras, barras de herramientas, grupos de botones. |
| Texto | Acciones de poco énfasis (Historial, Limpiar todo, enlaces dentro de tarjetas). |
| Icono sin fondo | Acciones de barras de herramientas y de filas. Siempre `aria-label` + tooltip. |
| Peligro / peligro con borde | Acciones destructivas. Relleno solo en el paso de confirmación. |

- Tamaños: pequeño (`--awe-size-control-sm`), normal (`--awe-size-control`) y grande (`--awe-size-control-lg`, login).
- Etiqueta con peso 500, icono a la izquierda, 8px de separación.
- Ocupado (#771): si la acción que lanzó un botón sigue en curso pasados unos 400 ms, ese botón muestra un spinner
  en lugar de su icono (antes de su texto si no tiene), sigue deshabilitado y no se atenúa; las acciones rápidas no
  muestran nada. El resto de botones se deshabilitan mientras corren acciones. `show-loading="false"` lo desactiva en un
  botón (ver [Botón](../api/button.md)).
- Los grupos de botones comparten bordes; un botón dividido es un primario más un botón con chevron.

| Hacer | No hacer |
|-------|----------|
| "Guardar cambios", "Borrar clientes" | "Aceptar", "Sí" |
| Un primario por zona | Dos botones rellenos uno junto a otro |
| Deshabilitar y explicar (tooltip) | Ocultar una acción que el usuario espera |

## Criterios (campos de formulario) {#criteria-form-fields}

| Elemento | Regla |
|----------|-------|
| Etiqueta | Encima del campo, `--awe-font-size-label`, peso 500. Obligatorio: `*` en rojo. Indicaciones opcionales alineadas a la derecha en texto atenuado. |
| Campo | Altura `--awe-size-control`, `--awe-radius-field`, borde `--awe-color-field-border`. Icono inicial opcional. |
| Foco | Borde en el primario más un halo suave. |
| Inválido | Borde rojo, mensaje bajo el campo con icono (`--awe-color-danger-text`), `aria-invalid` y `aria-describedby`. |
| Deshabilitado / solo lectura | Deshabilitado: fondo atenuado, sin sombra. Solo lectura: fondo relleno, valor seleccionable. Bloqueado: icono de candado y el motivo (ver #791). |
| Ayuda | Una línea bajo el campo en texto atenuado. |
| Disposición | Rejilla de columnas de 232px como mínimo; los campos anchos ocupan 2 columnas. |

**Números.** Alineados a la derecha, cifras tabulares, unidad como prefijo o sufijo (`€`, `days`), botones de
incremento opcionales.

### Select, selección múltiple y autocompletar {#select-multiselect-and-autocomplete}

La lista nativa del navegador **no** se usa: ignora el tema y el modo. Cada select es un combobox con una lista
desplegable al estilo de las listas de AWE.

| Parte | Regla |
|-------|-------|
| Desplegable | Superficie de capa, borde, `--awe-shadow-overlay`, 4px de relleno, 2px de separación, top layer (nunca recortado por grids ni diálogos). Se abre debajo, o encima si no hay sitio. |
| Opción | Relleno de 8px × 12px, `--awe-radius-list-option`. Hover/foco: `--awe-color-list-option-focus`. Seleccionada: fondo de resaltado más una marca. |
| Grupos | Cabecera de grupo opcional (pequeña, atenuada) y un separador. |
| Filtro | Un campo de filtro arriba cuando hay más de 10 opciones. |
| Teclado | ↑/↓, Inicio/Fin, Enter/Espacio, Esc, Tab cierra; búsqueda al teclear también con la lista cerrada. |
| ARIA | `role="combobox"` con `aria-expanded`, `aria-controls` y `aria-activedescendant`; `role="listbox"` y `role="option"` con `aria-selected`. |
| Selección múltiple | Valores seleccionados como chips eliminables en el campo; la lista muestra todas las opciones con casilla y "n de m" en el campo. |
| Autocompletar | El mismo estilo de lista; el texto coincidente se resalta; las opciones pueden tener una segunda línea (meta). |

### Fechas y horas {#dates-and-times}

- Fecha: campo de texto con botón de calendario. Calendario: hoy con contorno, el día seleccionado relleno con el
  primario.
- Selector de mes (#820): rejilla de 3×4 meses.
- Formatos de fecha (#820): el formato se muestra como indicación junto a la etiqueta.
- Rango (#839): un campo con dos entradas y una flecha; atajos (7 días, 1 mes, 3 meses, año en curso, 12 meses, todo)
  como botones píldora bajo el campo; el final no puede ser anterior al inicio.
- Hora: campo de texto con icono de reloj y cifras tabulares.

### Control segmentado (botones de opción) {#segmented-control-button-radio}

- Una pista con borde; el segmento seleccionado tiene superficie, borde, sombra y texto seminegrita propios
  (`--awe-segmented-*`). Debe leerse en los dos modos.
- **Nunca se recorta.** Los segmentos pasan a otra línea; en una celda de menos de 300px forman una rejilla de dos
  columnas a todo el ancho.
- Dele ancho suficiente en ejemplos y formularios: un control de cuatro opciones ocupa dos columnas de criterios.

### Casilla, opción e interruptor {#checkbox-radio-and-switch}

- Casilla `--awe-size-checkbox`, `--awe-radius-sm`; admite estado indeterminado.
- Grupos de opciones en fila, que pasan a otra línea; las opciones deshabilitadas conservan su etiqueta visible.
- Interruptor para ajustes que se aplican al momento; casilla para valores que se envían con un formulario.

### Subida de ficheros (#841) {#uploader-841}

Zona de arrastre (borde discontinuo, resaltado al arrastrar), enlace "browse", tipos y tamaño admitidos como ayuda.
Ficheros listados con icono, nombre, tamaño, progreso o estado. Una imagen muestra vista previa con Limpiar. Un botón
**Hacer foto** abre la cámara. En solo lectura se muestra el fichero y se deshabilitan las acciones.

## Grid {#grid}

| Parte | Regla |
|-------|-------|
| Tarjeta | El grid vive en una tarjeta; título y recuento en la cabecera de la tarjeta. |
| Barra de herramientas | Buscar en resultados, Filtros (con recuento), espacio, Columnas, Exportar (botón dividido). |
| Filtros activos | Chips discontinuos eliminables bajo la barra, más "Limpiar todo". |
| Cabecera | `--awe-grid-header-*`; las columnas ordenables muestran una flecha y la ordenada va en el primario. Fija al desplazar. |
| Filas | Relleno de celda `--awe-grid-cell-padding-*`, fondo de hover, sin cebreado. |
| Selección | Columna de casillas; las filas seleccionadas usan el fondo de resaltado; una barra de selección muestra el recuento y las acciones en bloque. |
| Números | Alineados a la derecha, tabulares, negativos en color de peligro con signo menos. |
| Estado | Etiquetas con punto (`tag--success`, `--warning`, `--danger`, `--info`). |
| Edición en línea | La fila editada lleva un contorno primario, campos dentro de las celdas (controles pequeños) y Guardar y Cancelar en la columna de acciones. |
| Acciones de fila | Botones de icono sin fondo al final de la fila, o un botón de menú (#801). |
| Paginador | "Showing 1–10 of 1,284", tamaño de página (select), páginas con primera/anterior/siguiente/última, "Go to page" (#826, #874). |
| Cargando | Filas esqueleto bajo la cabecera real (#856). Un refresco atenúa las filas actuales y muestra una barra de carga arriba. |
| Vacío | Estado vacío dentro del cuerpo del grid (#837). |

| Hacer | No hacer |
|-------|----------|
| Mantener la cabecera real mientras carga | Mostrar un spinner sobre una caja vacía |
| Poner la columna clave primero, con segunda línea | Repetir el mismo dato en dos columnas |
| Truncar con puntos suspensivos y tooltip | Dejar que un texto largo rompa la altura de la fila sin límite |

## Pestañas y acordeón {#tabs-and-accordion}

- Pestañas: etiquetas atenuadas; la activa, con texto y subrayado de 2px en `--awe-color-primary-text`; badge de
  recuento opcional; ←/→ para moverse. Una pestaña con error muestra el número de errores (#827).
- Acordeón: una cabecera por sección con icono y chevron; las secciones abiertas se animan; puede haber varias
  abiertas.

## Capas superpuestas {#overlays}

| Elemento | Regla |
|----------|-------|
| Diálogo | `<dialog>`, `--awe-radius-modal`, cabecera (título, subtítulo, cerrar), cuerpo con desplazamiento y pie con las acciones a la derecha. Máscara `--awe-color-mask` con un ligero desenfoque. Tamaños en #844. |
| Confirmación | Diálogo pequeño con icono de peligro, una pregunta precisa ("¿Borrar 2 clientes?"), la consecuencia y Cancelar enfocado por defecto. |
| Panel lateral (#843) | Entra desde la derecha, a toda la altura, con su propio pie de acciones. Para editar un registro sin salir de la lista. |
| Popover (#843) | Anclado a su botón, con flecha; puede contener criterios y un grid pequeño; Cancelar / Aceptar. |
| Menú desplegable | Lista de opciones con iconos; las destructivas al final, en texto de peligro, tras un separador. |
| Toast | Arriba a la derecha, bajo la cabecera, en la top layer (por encima de los diálogos). Éxito, información, aviso, error; se cierra a los 6 s y tiene botón de cerrar. Los errores usan `role="alert"`. |
| Tooltip | Superficie inversa (`--awe-color-tooltip`), texto pequeño; aparece con hover y con foco. Nunca es el único sitio de una información esencial. |

## Respuesta al usuario {#feedback}

- Mensajes: fondo sutil, borde e icono por estado; las primeras palabras en negrita enuncian el hecho.
- Etiquetas para estados, badges para recuentos.
- Estado vacío: icono en un círculo, título, una o dos líneas de explicación y como mucho dos acciones.
- Barra de progreso para un progreso conocido; barra de carga para uno desconocido.

## Elementos nuevos de AWE 5 {#new-awe-5-elements}

Agrupados como en la [página de componentes](pathname:///design-system/prototype/components.html). Los elementos
**Done** ya están en AWE 5 y se rediseñan; los **Planned** son propuestas.

### Funciones del grid {#grid-features}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Celdas de varias líneas con tooltip | #825 | Done | Hasta N líneas por columna y después puntos suspensivos; el texto completo en un tooltip. |
| Columnas de opciones y de barra de botones | #838 | Done | Una columna contiene un conjunto de opciones; las flechas cambian el valor. Usa el estilo del control segmentado. |
| Columna sparkline | #855 | Done | Línea, barras o área por fila; negativos en peligro; marcas de mínimo, máximo y último. |
| Detalle desplegable de fila | #835 | Done | Un chevron abre una zona de detalle bajo la fila (campos o un grid anidado), marcada con un borde izquierdo primario. |
| Mover filas | #842 | Done | Acciones de la barra: arriba del todo, subir, bajar, abajo del todo sobre las filas seleccionadas. |
| Barras de datos en celdas | #831 | Done | Una barra clara detrás del número; los negativos crecen hacia la izquierda en tono de peligro. El número sigue siendo legible. Un segundo valor dibuja dos barras finas redondeadas, el valor encima; los segmentos apilados usan la escala de datos. |
| Segunda línea, estado y etiquetas en celdas | #832 | Done | Segunda línea atenuada, estado con punto, etiquetas que se agrupan en "+n". |
| Agrupación de filas con subtotales | #833 | Done | Cabecera de grupo plegable con recuento y totales, una fila de subtotal por grupo y el total general en el pie. |
| Búsqueda rápida y filtros por valor | #834 | Planned | Búsqueda en todas las columnas; filtro de columna con valores y recuentos; los totales del pie indican que están filtrados. |
| Paginador con rango, total e ir a página | #826, #874 | Done | Ver Grid. |
| Diálogo de columnas por usuario | #813 | Done | Popover anclado al botón de columnas por defecto (`columns-panel`: `popover`, `dialog` o `side-panel`), con los cambios aplicados en vivo. Mostrar, ocultar, reordenar (arrastrando o con flechas), Restaurar predeterminado, Cancelar y Guardar. |

### Criterios y formularios {#criteria-and-forms}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Selector de mes y formatos de fecha | #820 | Done | Ver Fechas y horas. |
| Rango de fechas con atajos | #839 | Done | Ver Fechas y horas. |
| Subida con zona de arrastre, vista previa y cámara | #841 | Done | Ver Subida de ficheros. |
| Título de sección | #910 | Done | Una etiqueta pequeña en mayúsculas y atenuada, seguida de una línea que ocupa el resto de la fila, divide un formulario largo en secciones: el estilo `awe-section-title` de un tag, mejor en un encabezado (`<tag type="h3" style="awe-section-title" label="..."/>`). |
| Ir al primer campo con error entre pestañas | #827 | Done | Al validar, abrir la pestaña con el primer error, marcar la pestaña, enfocar el campo y mostrar un resumen con "Go to field". |
| Obligatorio para guardar frente a obligatorio para terminar | #840 | Planned | `*` impide guardar; una segunda marca (rombo, tono de aviso, borde discontinuo) es necesaria para terminar; una lista de pendientes con enlaces; Guardar borrador sigue habilitado. |
| Búsqueda avanzada | #790 | Planned | Condiciones (campo, operador, valor), grupos anidados Todas/Alguna, búsquedas guardadas por usuario. |
| Restricciones de acceso visibles | #791 | Planned | Los campos y acciones bloqueados siguen visibles con un candado y el motivo (ayuda o tooltip). |

### Capas y paneles {#overlays-and-panels}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Menú contextual de campo y botón de menú de fila | #801 | Done | Clic derecho o Mayús+F10 sobre un campo, botón o pestaña; un botón de menú en una fila abre el menú de esa fila. |
| Maximizar y restaurar | #799, #813 | Done | Ocupa el área de contenido; Restaurar o Esc lo devuelve. |
| Panel lateral | #843 | Done | Ver Capas superpuestas. |
| Popover anclado a un botón | #843 | Done | Ver Capas superpuestas. |
| Tamaños de diálogo | #844 | Done | sm 360px, md 560px, lg 800px, xl 1140px, pantalla completa. |

### Navegación y lanzaderas {#navigation-and-launchers}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Páginas lanzadera con descripciones | #828 | Done | Tarjetas con icono, título, una línea de descripción y recuento opcional; una lanzadera puede abrir otra; el breadcrumb muestra todos los niveles. |

### Visualización de datos {#data-visualisation}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Tabla dinámica | #821, #878 | Done | Zonas para filas, columnas y valores; select de agregación; intercambiar; totales por fila y por columna. |
| Mapa | #798 | Done | Controles y paneles con las superficies y botones de AWE; marcadores y rutas en el primario. |
| Celdas con escala de color y grid sin cabecera | #830 | Done | Escala de 5 pasos desde el primario con leyenda; un grid sin cabecera empieza por su primera fila. |
| Card | #899 | Done | Tarjeta de métrica: título, valor (tabular) con unidad, variación como etiqueta con flecha y texto (tono según `good-trend`), progreso hacia un objetivo opcional, un chip de estado opcional arriba a la derecha, delante del icono (#836) y un cuerpo con cualquier componente; se coloca con `awe-card-grid`. |
| KPI | #836 | Planned | Añade el modo filtro sobre la card (#899): un estado seleccionado, y un clic filtra una rejilla mediante dependencias. |

### Respuesta al usuario y carga {#feedback-and-loading}

| Elemento | Issue | Estado | Regla |
|----------|-------|--------|-------|
| Esqueletos: grid, pantalla y atenuado | #856, #869 | Done | Mostrar la estructura que va a llegar; un refresco atenúa en lugar de ocultar. |
| Estado vacío configurable del grid | #837 | Done | Icono, título, texto y acción propios por grid. |
| Avatar sin imagen y pantalla de carga inicial | #864 | Done | Foto, iniciales o un icono genérico de usuario; nunca un círculo vacío. Pantalla de carga con el logo y después el esqueleto de la pantalla. |

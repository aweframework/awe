---
id: keyboard-shortcuts
title: Atajos de teclado
sidebar_label: Atajos de teclado
---

Esta página lista los atajos de teclado del cliente React. El cliente AngularJS tiene los mismos atajos para la pila de
acciones, las filas del grid editable, los criterios que envían la pantalla con Enter, el buscador del menú y las
columnas radio.

## Atajos disponibles {#available-shortcuts}

### Globales {#global}

| Teclas | Acción |
|--------|--------|
| `Alt` + `Shift` + `0`..`9` | Muestra la pila de acciones de cliente y servidor (depuración). `0` la oculta. |
| `Ctrl` + `K` (`⌘` + `K` en macOS) | Abre el buscador del menú y le da el foco. El campo de búsqueda muestra el atajo. Con el menú plegado en el rail el buscador está oculto y el navegador conserva la tecla. |

El dígito se lee de la tecla física, por lo que también funciona en macOS, donde `Option` cambia el carácter escrito.

### Grid

| Teclas | Acción |
|--------|--------|
| `Alt` + `ArrowUp` / `ArrowDown` | Mueve la fila seleccionada hacia arriba o hacia abajo (grid editable, o grid con [`move-rows`](../api/grids.md#drag-and-drop)). |
| `Alt` + `Home` / `End` | Mueve la fila seleccionada a la primera o a la última posición (grid editable, o grid con [`move-rows`](../api/grids.md#drag-and-drop)). |
| `Enter` | Guarda la edición de la fila. |
| `Escape` | Cancela la edición de la fila. |
| `Escape` (en una celda cortada) | Oculta el tooltip con el texto completo de la celda. |
| `Enter` (en una columna de texto) | Guarda el valor escrito en la celda. |
| `ArrowRight` / `ArrowDown` | Columna radio: selecciona la opción siguiente, de la última a la primera. |
| `ArrowLeft` / `ArrowUp` | Columna radio: selecciona la opción anterior, de la primera a la última. |

### Criterios {#criteria}

| Teclas | Acción |
|--------|--------|
| `Enter` (texto, contraseña, color, fecha, fecha filtrada, hora y rango de fechas) | Envía la pantalla. |
| `Enter` (numérico) | Confirma el valor escrito en el criterio. |
| `Enter` (en un día, mes o año de un calendario) | Lo elige, sin enviar la pantalla ni guardar la fila de un grid. |
| `ArrowDown` (campo de fecha) | Entra en el calendario. Al abrirse el calendario el foco sigue en el campo, para poder escribir la fecha. |
| `Space` (suggest vacío) | Abre las opciones del suggest. |

### Buscador del menú {#menu-search}

| Teclas | Acción |
|--------|--------|
| `ArrowDown` / `ArrowUp` | Va al resultado siguiente o anterior. |
| `Enter` | Abre el resultado activo. |
| `Escape` | Cierra el buscador. |

### Diálogos {#dialogs}

| Teclas | Acción |
|--------|--------|
| `Escape` | Cierra el diálogo. Los diálogos de mensaje y de confirmación no se cierran con ella. |

## Registrar un atajo {#register-a-shortcut}

El cliente React registra sus atajos con el hook `useKeyboardShortcut` y las utilidades de
`src/utilities/keyboard.js`. El hook añade un listener mientras el componente está montado y lo elimina al
desmontarlo, por lo que nunca reemplaza los manejadores de otros componentes (no asignes `window.onkeydown`).

```jsx
import useKeyboardShortcut from "../hooks/useKeyboardShortcut";

function MyComponent({onSave}) {
  // Escucha en window; pasa {target: ref} para escuchar en un elemento
  useKeyboardShortcut([
    {shortcut: {key: "s", ctrl: true}, handler: (event) => { event.preventDefault(); onSave(); }}
  ]);
  return null;
}
```

* Un atajo tiene `key` (depende del idioma, para teclas con nombre como `Enter`) o `code` (tecla física, para dígitos y
  letras), ambos como cadena o como lista de cadenas.
* Los modificadores `alt`, `shift`, `ctrl` y `meta` son exactos: el que no se indica no debe estar pulsado. Indícalo
  como `ANY` (exportado por la utilidad) para aceptarlo en ambos casos, o expande `ANY_MODIFIERS` para aceptar todas las
  combinaciones.
* Para manejadores escritos como props `onKeyDown`, `createKeyHandler(bindings)` devuelve una función que ejecuta el
  primer binding que coincide y devuelve si alguno lo hizo. Un binding puede tener un predicado `when(event)` para una
  condición que no es una tecla (por ejemplo, un input vacío): el binding se omite cuando devuelve false.
* Con `target: ref` el elemento se lee cuando se ejecuta el efecto, por lo que debe estar montado junto con el
  componente. Para un elemento que se renderiza de forma condicional usa un `onKeyDown` con `createKeyHandler`.
* El hook conserva los últimos bindings, por lo que pueden cambiar en cada render sin volver a suscribirse. Las
  opciones son `target`, `enabled`, `eventName` y `capture`.

Añade cada atajo nuevo al catálogo `SHORTCUTS` de `src/utilities/keyboard.js` y a las tablas anteriores.

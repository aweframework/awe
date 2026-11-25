---
id: dependency-task
title: Dependencias entre tareas
sidebar_label: Dependencias entre tareas
---

Una tarea puede ejecutarse después de otras tareas: sus **padres**. Cuando un padre termina, el planificador lanza las
tareas que se ejecutan después de él (sus **dependencias**, o hijas), y esas tareas pueden tener sus propias
dependencias, creando un flujo de trabajo.

## Configuración de dependencias {#dependencies-configuration}

Las dependencias de una tarea se eligen **en la hija**, en la sección **Cuándo** del
[editor de tareas](../scheduler-module.md#task-editor) (`scheduler-task-editor`):

1. Abre la tarea en el editor (**Editar** en el [catálogo de tareas](../scheduler-module.md#tasks-catalog) o en la
   [ficha de tarea](../scheduler-module.md#task-screen), o **Cambiar de qué depende** en su pestaña
   **Dependencias**).
2. En **Cuándo**, elige **Después de otras tareas** y selecciona sus padres. La lista agrupa las tareas en *Elegidas*,
   *Disponibles* y *No se pueden elegir* (las tareas que ya se ejecutan después de ella, que cerrarían un ciclo).
3. Guarda. La tarea se guarda con lanzamiento `Manual` y sus padres, y se ejecuta cuando estos terminan.

| Ajuste | Dónde se configura | Significado |
|--------|--------------------|-------------|
| Padres | **Cuándo** › **Después de otras tareas** de la hija | Las tareas tras las que se ejecuta. Con varios padres espera a todos (ver [fan-in](#flow-runs-and-fan-in)) |
| Condición | **Comportamiento** del **padre** | Cuándo lanza el padre a sus hijas: siempre si termina OK, también con aviso, también con error. Es la misma para todas las hijas de ese padre, así que la hija la muestra en solo lectura, con un enlace al padre |
| Marcar el padre como aviso | **Comportamiento** del **padre** | El padre termina con aviso cuando una de sus dependencias termina con error |

La sección **Dependencias** del editor y la pestaña **Dependencias** de la ficha de tarea muestran, en solo
lectura, las tareas que se ejecutan después de ella. Salir de **Después de otras tareas** con padres elegidos pide antes
una confirmación, porque los borra.

## Flujos de trabajo {#workflows}

<img alt="Ejemplo de flujo de trabajo" src={require('@docusaurus/useBaseUrl').default('img/Dependency-tasks.png')} />

Esta imagen muestra un ejemplo de cómo crear un flujo de trabajo mediante la concatenación de dependencias del planificador.

## Ejecuciones de flujo y fan-in {#flow-runs-and-fan-in}

Una tarea lanzada por su planificación, de forma manual, por un fichero o por un webhook inicia una **ejecución de flujo**. La ejecución de
flujo agrupa esa ejecución y todas las dependencias lanzadas a partir de ella, directamente o a través de otras
dependencias: estas tareas forman el **ámbito** de la ejecución de flujo.

Una tarea con varios padres (**fan-in**) se lanza una sola vez, no una vez por padre. La tarea hija espera a que hayan
terminado todos los padres que están en el ámbito de la ejecución de flujo, y solo se lanza si cada uno cumplió su
condición: terminó correctamente, o con advertencia o error cuando el padre lanza sus dependencias en advertencia o en
error. Si algún padre no cumplió su condición, la hija se **omite**, y la omisión se propaga: las hijas de una tarea
omitida también se omiten.

La ejecución de flujo termina cuando no queda ninguna tarea en espera ni en ejecución. Su estado es el peor de sus
ejecuciones: **error** (error, detenida o interrumpida), después **advertencia** (advertencia o alguna tarea omitida) y,
por último, **correcto**.

### Parámetros de una dependencia {#parameters-of-a-dependency}

Cada parámetro de una dependencia toma el primer valor que se encuentre en este orden:

1. el valor por defecto de la tarea;
2. el valor propagado por el padre que completa la unión (el que termina el último);
3. el parámetro de la ejecución de flujo con el mismo nombre. Los parámetros de la ejecución de flujo solo se aplican a
   los parámetros `VARIABLE`: la tarea hija define qué parámetros acepta. Los parámetros de la ejecución de flujo de un
   [webhook](scheduler-guide.md#4-webhook) son los valores mapeados desde sus datos, de modo que llegan a todos los pasos
   del flujo.

### Dependencias circulares {#circular-dependencies}

Se rechaza guardar una tarea cuyas dependencias harían que se lance a sí misma, directamente o a través de otras tareas, con
el mensaje **Dependencia circular**. Elimina la dependencia que cierra el ciclo.

### Página del flujo {#flow-run-page}

Todo enlace a una ejecución de flujo abre la [página del flujo](../scheduler-module.md#flow-run-page)
(`scheduler-flow-run`): la columna de flujo y el panel lateral de la [pantalla de actividad](../scheduler-module.md#activity-screen)
(también su vista **Agrupar por flujo**, que lista las ejecuciones de flujo de la más reciente a la más antigua), y el
historial y la última ejecución de la [ficha de tarea](../scheduler-module.md#task-screen). La página muestra el estado final del
flujo y por qué, quién lo lanzó, su inicio, su duración y sus parámetros, y sus tareas como grafo, cronograma o lista,
con las omitidas; una tarea abre el detalle de su ejecución.

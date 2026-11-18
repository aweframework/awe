---
id: dependency-task
title: Dependencias entre tareas
sidebar_label: Dependencias entre tareas
---

Otra potente característica del módulo Planificador es la posibilidad de añadir dependencias a una tarea. Las dependencias son tareas que se lanzarán justo después de que termine la tarea padre. Estas dependencias también pueden contener otras dependencias, creando un flujo de trabajo.

## Tipos {#types}
Hay dos tipos principales de dependencias.

### Dependencias síncronas {#synchronous-dependencies}

Las dependencias síncronas se ejecutan, como su nombre indica, de forma síncrona, en un orden configurable.

Las dependencias síncronas se crean estableciendo la opción Blocking=Yes.

Establecer bloqueante a `yes` significa que, en caso de que alguna de las dependencias termine con un error, cancelará toda la pila de ejecución síncrona.

Para gestionar la pila existe el criterio Order, que se utiliza para establecer el orden en el que se van a ejecutar las dependencias síncronas.

### Dependencias asíncronas {#asynchronous-dependencies}

Las dependencias asíncronas se ejecutan en bloque y su orden de ejecución no se puede configurar.

Como las dependencias se ejecutan en bloque, no se bloquean entre sí.

## Configuración de dependencias {#dependencies-configuration}

La pantalla de configuración de dependencias permite crear dos tipos de dependencias, síncronas y asíncronas.

La diferencia entre ellas es simplemente que una es bloqueante y la otra no.

**Para añadir una tarea como dependencia, su tipo de lanzamiento debe estar establecido en `Manual`.**

| Elemento      | Definición    | Uso   |
| ------------- |:-------------:| -----:|
| Task          | La tarea que va a ejecutar la dependencia    | **Obligatorio** |
| Blocking      | Se utiliza para definir si la dependencia va a ser síncrona o asíncrona, y si puede cancelar la pila de ejecución de dependencias síncronas | **Obligatorio** |
| Order         | Orden de ejecución de la dependencia síncrona, solo necesario si la opción `Blocking` está establecida en `Yes`; en caso contrario estará deshabilitado |  **Obligatorio** |

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

### Pantalla de ejecuciones de flujo {#flow-runs-screen}

La opción **Ejecuciones de flujo** del menú del planificador lista las ejecuciones de flujo, de la más reciente a la más
antigua, con su estado, tarea raíz, ejecución raíz, quién la lanzó y las horas de inicio y fin. Al seleccionar una se
muestran sus ejecuciones y las tareas omitidas. En el gestor de tareas, selecciona una ejecución y pulsa **Abrir flujo** para
ir directamente a su ejecución de flujo.

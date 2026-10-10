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

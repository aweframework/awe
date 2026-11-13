---
id: parameter-handling
title: Gestión de parámetros
sidebar_label: Gestión de parámetros
---

Los servicios de AWE pueden leer parámetros desde dos lugares:

1. la **petición en vivo**, mientras la petición HTTP sigue activa;
2. una **instantánea de parámetros propagada**, cuando el código se ejecuta más tarde en un hilo asíncrono.

Utiliza la API que se ajuste a lo que necesitas hacer. No uses `getRequest()` como API de parámetros por defecto.

## Referencia rápida de la API {#api-quick-reference}

| Necesidad | Usar | Significado |
| --- | --- | --- |
| Acceder al objeto de la petición en vivo | `getRequest()` | "Necesito el bean de la petición actual." |
| Leer parámetros de forma segura desde el contexto actual | `getRequestParameters()` / `getRequestParameter(...)` | "Quiero leer parámetros de forma segura." |
| Construir un objeto de parámetros que voy a modificar | `getMutableRequestParameters()` | "Quiero una instantánea que voy a modificar." |
| Añadir o reemplazar valores en un `ObjectNode` | `putRequestParameter(parameters, name, value)` | "Estoy preparando parámetros para otra llamada." |
| Hacer visibles los nuevos valores a las tareas asíncronas descendientes | `mergePropagatedRequestParameters(...)` o `putPropagatedRequestParameter(...)` | "Los futuros saltos asíncronos deben heredar estos valores." |
| Trabajar fuera de `ServiceConfig` | `QueryUtil` | API de más bajo nivel usada por los ayudantes de `ServiceConfig`. |

## La regla simple {#the-simple-rule}

:::tip Valor recomendado por defecto
En los servicios, prefiere los ayudantes de parámetros expuestos por `ServiceConfig`:

- `getRequestParameters()` para leer;
- `getMutableRequestParameters()` cuando vayas a modificar una instantánea;
- `putRequestParameter(...)` para añadir valores a esa instantánea.
:::

Usa `getRequest()` solo cuando realmente necesites el propio bean de la petición en vivo.

## `getRequest()` {#getrequest}

Usa `getRequest()` cuando el código esté ligado a una petición HTTP activa y necesite un comportamiento específico de la petición.

Casos de uso típicos:

- leer metadatos exclusivos de la petición;
- acceder al token de la petición, a la acción de destino o a datos relacionados con el servlet;
- actualizar la petición en vivo en un flujo de petición síncrono.

```java
String screenName = getRequest().getParameterAsString("screen");
```

:::warning
No uses `getRequest()` solo porque necesites el valor de un parámetro. En código asíncrono la petición en vivo puede haber dejado de existir.
:::

## `getRequestParameters()` {#getrequestparameters}

`getRequestParameters()` significa:

> "Quiero leer parámetros de forma segura."

Uso esperado:

- leer valores;
- inspeccionar los parámetros del contexto de ejecución actual;
- evitar depender de una petición en vivo en código que puede ejecutarse de forma asíncrona;
- no hacer hincapié en la modificación.

```java
ObjectNode parameters = getRequestParameters();
String userName = getRequestParameterAsString("user", parameters);
JsonNode report = getRequestParameter("report", parameters);
```

Cómo resuelve los parámetros: fusión de dos fuentes en el momento de la lectura:

| Fuente | Descripción | Prioridad |
|--------|-------------|----------|
| Petición en vivo (`AweRequest`) | Parámetros HTTP del bean de la petición actual | Base: la más baja |
| Instantánea propagada | Instantánea de trabajo ya fusionada, instalada por `AweMDCTaskDecorator` en los hilos asíncronos | Superposición: prevalece en caso de conflicto |

La instantánea propagada es la instantánea que `AweMDCTaskDecorator` ensambló en el momento de enviar
la tarea a partir de la petición en vivo, cualquier instantánea ancestra y cualquier superposición pendiente escrita mediante
`putPropagatedRequestParameter`.  Cuando `getRequestParameters()` se ejecuta en un hilo asíncrono,
las tres fuentes ya están colapsadas en una única instantánea: `getRequestParameters()` solo
fusiona dos cosas: la petición en vivo (si la hay) y esa instantánea preensamblada.

Cuando no hay ninguna petición en vivo activa (hilos asíncronos, trabajos del planificador), solo se usa la instantánea propagada
y el resultado nunca es `null`.  Si no existe ninguna de las dos fuentes, el método devuelve un
objeto vacío.

Usa esta API para lecturas seguras en servicios, especialmente cuando el método pueda llamarse desde `@Async`, trabajos del planificador, hilos hijos o flujos de servicio reutilizables.

## `getMutableRequestParameters()` {#getmutablerequestparameters}

`getMutableRequestParameters()` significa:

> "Quiero una instantánea que voy a modificar."

Uso esperado:

- construir parámetros para llamadas posteriores;
- añadir valores a un `ObjectNode`;
- preparar datos para llamadas a maintain, correo, informe, impresión o servicio;
- mantener los cambios explícitos en lugar de mutar el estado asociado a la petición.

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "PdfNam", pdfPath);
putRequestParameter(parameters, "ScrTitFil", fileName);
putRequestParameter(parameters, "report", reportNode);
```

Después pasa el objeto a la operación posterior:

```java
maintainService.launchMaintain("SndRep", parameters);
```

Este es el patrón preferido cuando el código en hilos prepara datos para trabajo posterior de maintain, correo, informe o impresión.

## Propagación asíncrona de parámetros {#async-parameter-propagation}

AWE puede propagar una instantánea de los parámetros de la petición a los hilos hijos, pero solo cuando la tarea asíncrona usa un ejecutor de AWE con conciencia de contexto.

La anotación común de Spring es:

```java
@Async("threadPoolTaskExecutor")
```

`threadPoolTaskExecutor` está configurado con la decoración de tareas de AWE necesaria para copiar el contexto. Si un proyecto usa otro ejecutor, ese ejecutor debe proporcionar el mismo comportamiento de propagación de contexto.

:::important
La propagación asíncrona te da una instantánea de parámetros, no un `AweRequest` en vivo.
:::

**Tareas hermanas**: varias tareas asíncronas enviadas dentro de la misma petición reciben todas la misma
superposición propagada, sin necesidad de reescribirla entre envíos.

**Ciclo de vida**: la superposición propagada está asociada a la petición.  Se escribe en el hilo de la petición,
es heredada por cada llamada a `decorate()` como una instantánea inmutable, y se limpia exactamente una vez al
final de la petición mediante `AwePropagationCleanupFilter`.  Nunca se filtra a una petición posterior no relacionada.

## Actualizar la instantánea propagada {#updating-the-propagated-snapshot}

La mayor parte del código debería pasar un `ObjectNode` explícito a la siguiente operación. Normalmente es suficiente.

Actualiza la instantánea propagada solo cuando los nuevos valores deban ser heredados por los saltos asíncronos descendientes.

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "PdfNam", reportPath);

mergePropagatedRequestParameters(parameters);
```

Para un único valor, puedes escribir directamente en la instantánea propagada:

```java
putPropagatedRequestParameter("PdfNam", reportPath);
```

La superposición está asociada a la petición: una vez escrita permanece disponible para todas las tareas asíncronas decoradas
dentro de la misma petición (incluidas las tareas hermanas), sin reescribirla entre envíos.
Se limpia automáticamente al final de la petición, sin necesidad de limpieza manual.

## Guía de escenarios {#scenario-guide}

| Escenario | Leer con | Escribir / preparar con |
| --- | --- | --- |
| Código síncrono que necesita el objeto de la petición | `getRequest()` | `getRequest()` si se pretende mutar la petición en vivo |
| Servicio síncrono que solo necesita parámetros | `getRequestParameters()` / `getRequestParameter(...)` | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Servicio asíncrono con un ejecutor de AWE | `getRequestParameters()` / `getRequestParameter(...)` | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Varias tareas asíncronas hermanas en la misma petición | `getRequestParameters()` en cada tarea | escribir la superposición una vez en el hilo de la petición con `putPropagatedRequestParameter(...)`: todas las hermanas la heredan |
| Flujo asíncrono que llama a otro flujo asíncrono | `getRequestParameters()` / `getRequestParameter(...)` | `ObjectNode` explícito; opcionalmente actualizar la instantánea propagada |
| Maintain, correo, informe o impresión tras trabajo en hilos | `ObjectNode` explícito | `getMutableRequestParameters()` + `putRequestParameter(...)` |
| Clase que no extiende `ServiceConfig` | `QueryUtil` | `QueryUtil` / `ObjectNode` explícito |

## Ejemplos prácticos {#practical-examples}

### Lectura segura {#safe-read}

```java
ObjectNode parameters = getRequestParameters();
String userName = getRequestParameterAsString("user", parameters);
```

### Preparar parámetros de informe {#prepare-report-parameters}

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "ScrTit", screenTitle);
putRequestParameter(parameters, "ScrTitFil", fileName);
putRequestParameter(parameters, "PdfNam", pdfPath);

maintainService.launchMaintain("SndRep", parameters);
```

### Preparar parámetros de correo {#prepare-mail-parameters}

```java
ObjectNode parameters = getMutableRequestParameters();
putRequestParameter(parameters, "subject", subject);
putRequestParameter(parameters, "body", body);
putRequestParameter(parameters, "recipients", recipients);

mailService.sendEmail(parameters);
```

## Lista de comprobación {#checklist}

Antes de elegir una API, pregúntate:

- ¿Necesito el objeto de la petición en vivo, o solo los valores de los parámetros?
- ¿Puede este código ejecutarse en `@Async`, un planificador o un hilo hijo?
- ¿Solo estoy leyendo parámetros?
- ¿Estoy construyendo parámetros para maintain, correo, informe, impresión u otro servicio?
- ¿Deben las tareas asíncronas descendientes heredar los nuevos valores?

## Resumen {#summary}

- Usa `getRequest()` solo para trabajo específico de la petición en vivo.
- Usa `getRequestParameters()` cuando la intención sea una lectura segura de parámetros: fusiona la petición en vivo con la instantánea propagada preensamblada (construida por `AweMDCTaskDecorator` en el momento del envío) y funciona tanto en hilos síncronos como asíncronos.
- Usa `getMutableRequestParameters()` cuando la intención sea modificar una instantánea de parámetros.
- Usa `putRequestParameter(...)` para añadir valores a ese `ObjectNode`.
- Usa `mergePropagatedRequestParameters(...)` o `putPropagatedRequestParameter(...)` solo cuando los saltos asíncronos descendientes deban heredar nuevos valores; la superposición es compartida por todas las tareas hermanas y se limpia al final de la petición.
- Usa `QueryUtil` directamente solo fuera de `ServiceConfig` o para código de infraestructura de más bajo nivel.

## Siguiente paso {#next-step}

Si estás preparando parámetros de informe o de impresión, revisa también la guía del [motor de impresión](./print-guide.md).

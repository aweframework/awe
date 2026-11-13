---
id: schedule-configuration
title: Configuración de planificaciones de tareas
sidebar_label: Configuración de planificaciones de tareas
---

El planificador es una herramienta potente que permite crear cualquier tipo de planificación de forma sencilla.

Con este fin, y teniendo la simplicidad en mente, la creación de planificaciones se ha diseñado con tres opciones principales:

## Planificaciones repetitivas {#repetitive-schedules}

Planificaciones que se lanzan cada cierto tiempo `X`. Podemos elegir el tiempo entre lanzamientos.

La planificación repetitiva, como su nombre indica, permite crear planificaciones que se lanzan cada tiempo `X`, donde `X` es el tiempo entre un lanzamiento y el siguiente.

Dependiendo de la unidad de tiempo, habrá otras opciones disponibles para ayudar a los usuarios a crear planificaciones avanzadas.

Pero primero, los siguientes campos son comunes a todas las opciones posibles:

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Calendario | Un calendario de festivos a usar para la tarea actual; la tarea no se lanzará en las fechas contenidas en el calendario | Opcional | `Execution date` |
| Desde ^1 | La fecha / hora de inicio de la planificación de la tarea actual | Opcional | Ninguno |
| Hasta ^2 | La fecha / hora de fin de la planificación de la tarea actual | Opcional | Ninguno |
| Repetir cada | El tiempo entre ejecuciones | **Obligatorio** | Ninguno |

***

^1: Si el criterio `From` está vacío, la tarea se lanzará por primera vez en el momento en que se cree.

^2: Si el criterio `To` está vacío, la tarea se lanzará indefinidamente hasta que cambie su configuración o se elimine la tarea.

### Segundos {#seconds}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | Ninguno |
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Days` |
| Horas | La hora a la que se lanzará la tarea | Opcional | Ninguno |
| Minutos | Los minutos a los que se lanzará la tarea | Opcional | Ninguno |

### Minutos {#minutes}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | Ninguno |
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Days` |
| Horas | La hora a la que se lanzará la tarea | Opcional | Ninguno |

### Horas {#hours}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | Ninguno |
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Days` |

### Días {#days}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | Ninguno |
| Horas | La hora a la que se lanzará la tarea | Opcional | `Execution time`|
| Minutos | Los minutos a los que se lanzará la tarea | Opcional | `Execution time` |
| Segundos | Los segundos a los que se lanzará la tarea | Opcional | `Execution time` |
| Hora de ejecución | La hora en la que se lanzará la planificación. | Opcional | `Hours`,`Minutes`,`Seconds` |

### Meses {#months}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Days` |
| Horas | La hora a la que se lanzará la tarea | Opcional | `Execution time`|
| Minutos | Los minutos a los que se lanzará la tarea | Opcional | `Execution time` |
| Segundos | Los segundos a los que se lanzará la tarea | Opcional | `Execution time` |
| Hora de ejecución | Para especificar una hora de ejecución | Opcional | `Hours`,`Minutes`,`Seconds` |

### Años {#years}

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | Ninguno |
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Days` |
| Horas | La hora a la que se lanzará la tarea | Opcional | `Execution time`|
| Minutos | Los minutos a los que se lanzará la tarea | Opcional | `Execution time` |
| Segundos | Los segundos a los que se lanzará la tarea | Opcional | `Execution time` |
| Hora de ejecución | Para especificar una hora de ejecución | Opcional | `Hours`,`Minutes`,`Seconds` |


> **Nota:** Si uno de los valores opcionales dentro de la configuración se deja vacío, se aplicará el valor por defecto `All`.


## Planificaciones de una sola vez {#one-time-schedules}

Este tipo de planificación lanzará una tarea una única vez.

Este tipo de planificación lanza una tarea solo una vez, creando un patrón cron con una fecha y hora específicas.

Los campos disponibles para crear este tipo de tarea son:

| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Fecha de ejecución | La fecha de lanzamiento. | **Obligatorio** | Ninguno |
| Hora de ejecución | La hora de lanzamiento. | **Obligatorio** | Ninguno |

> **Nota:** Es necesario rellenar al menos uno de los dos criterios.

## Planificaciones personalizadas {#custom-schedules}

La planificación personalizada permite crear todas las configuraciones posibles con las que puede trabajar el planificador Quartz. La pantalla de configuración de la planificación personalizada usa dependencias para ayudar al usuario en la correcta creación de la planificación, habilitando / deshabilitando criterios cuando es necesario.

La planificación personalizada ofrece todos los campos disponibles que se pueden personalizar para crear el patrón cron.


| Opción       | Definición    | Uso | Deshabilita |
| ------------- |:-------------:| -------- |:-------------: | 
| Calendario | Un calendario de festivos a usar para la tarea actual; la tarea no se lanzará en las fechas contenidas en el calendario | Opcional | `Execution date` |
| Desde ^1 | La fecha / hora de inicio de la planificación de la tarea actual | Opcional | `Execution date`,`Execution time` |
| Hasta ^2 | La fecha / hora de fin de la planificación de la tarea actual | Opcional | `Execution date`,`Execution time` |
| Años | Los años en los que se lanzará la planificación. | Opcional | `Execution date` |
| Meses | Para especificar en qué meses queremos lanzar la tarea | Opcional | `Execution date` |
| Días | Para especificar en qué días del mes queremos lanzar la tarea | Opcional | `Execution date`,`Days of the week` |
| Días de la semana | Para especificar en qué días de la semana queremos lanzar la tarea | Opcional | `Execution date`,`Days` |
| Horas | La hora a la que se lanzará la tarea | Opcional | `Execution time`|
| Minutos | Los minutos a los que se lanzará la tarea | Opcional | `Execution time` |
| Segundos | Los segundos a los que se lanzará la tarea | Opcional | `Execution time` |
| Fecha de ejecución | La fecha en la que se lanzará la planificación | Opcional | `Calendar`,`From`,`To`,`Years`,`Months`,`Days`,`Days of the week` |
| Hora de ejecución | La hora a la que se lanzará la planificación | Opcional | `Hours`,`Minutes`,`Seconds` |

***

^1: Si el criterio `From` está vacío, la tarea se lanzará en el momento en que se cree.

^2: Si el criterio `To` está vacío, la tarea se lanzará indefinidamente hasta que cambie su configuración o se elimine la tarea.

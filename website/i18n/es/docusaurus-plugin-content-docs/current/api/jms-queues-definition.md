---
id: queues
title: Definición de colas
sidebar_label: Definición de colas
---

El motor Jms de AWE permite la integración con servidores de colas. Soporta ambas tipologías JMS, **punto a punto** y **publicación/suscripción**.

:::tip
Cada elemento y atributo de las colas JMS está listado en la [referencia XSD](/reference/queues) generada.
:::

La API `Java Message Service` (JMS) es una API de Java Message Oriented Middleware (MOM) para el envío de mensajes entre dos o más clientes. JMS forma parte de Java Platform, Enterprise Edition, y está definida por una especificación desarrollada bajo el Java Community Process como JSR 914. 

Es un estándar de mensajería que permite a los componentes de aplicación basados en Java Enterprise Edition (Java EE) crear, enviar, recibir y leer mensajes. Permite que la comunicación entre distintos componentes de una aplicación distribuida esté débilmente acoplada, sea fiable y asíncrona.

* **Modelo punto a punto**

En un sistema de mensajería punto a punto, los mensajes se encaminan a un consumidor individual que mantiene una cola de mensajes "entrantes". Este tipo de mensajería se basa en el concepto de colas de mensajes, emisores y receptores. Cada mensaje se dirige a una cola concreta, y los clientes receptores extraen los mensajes de las colas establecidas para guardar sus mensajes. Aunque cualquier número de productores puede enviar mensajes a la cola, se garantiza que cada mensaje se entrega y es consumido por un único consumidor. Las colas retienen todos los mensajes que se les envían hasta que los mensajes se consumen o hasta que caducan. Si no hay consumidores registrados para consumir los mensajes, la cola los retiene hasta que un consumidor se registre para consumirlos.

<img alt="queue_point_to_point" src={require('@docusaurus/useBaseUrl').default('img/queue_point_to_point.png')}/>

* **Modelo publicación - suscripción**

El modelo de publicación/suscripción permite publicar mensajes en un tema (topic) de mensajes concreto. Los suscriptores pueden registrar su interés en recibir mensajes de un tema concreto. En este modelo, ni el publicador ni el suscriptor se conocen entre sí. Una buena analogía es un tablón de anuncios anónimo: cero o más consumidores recibirán el mensaje.

Existe una dependencia temporal entre publicadores y suscriptores. El publicador tiene que crear un tema de mensajes al que los clientes se suscriban. El suscriptor tiene que permanecer activo continuamente para recibir mensajes, a menos que haya establecido una suscripción duradera. En ese caso, los mensajes publicados mientras el suscriptor no está conectado se redistribuirán cuando se vuelva a conectar.

<img alt="topic" src={require('@docusaurus/useBaseUrl').default('img/topic.png')}/>

:::info
**Nota:** Todas las colas se definen en el archivo `Queues.xml` de la **carpeta global**. Consulta la [estructura de proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## **Estructura XML de la cola** {#queue-xml-structure}

La estructura completa de la cola es la siguiente:

```xml
<queues xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:noNamespaceSchemaLocation="https://aweframework.gitlab.io/awe/docs/schemas/queues.xsd">
<queue id="[queue_ID]">
  <request-message destination="[queue_name]" type="[message_type]" [selector="[selector]" separator="[separator_char]" timeout="[timeOut]"]>
   <message-parameter id="[param_message_id]" type="[param_type]"   name="[param_name]" [list="[list]" value="[static_value]"] />
   <message-wrapper name="[name_wrapper]" type="[type_wrapper]" classname="[classname]" />
	… More <message-parameter/>
	… More <message-wrapper/>
  </request-message>
  <response-message destination="[queue_name]" type="[message_type]" [selector="[selector]" separator="[separator_char]" timeout="[timeOut]"]>
   <message-parameter id="[param_message_id]" type="[type_param]" name="[param_name]" [list="[list]" value="[static_value]"] />
   <message-wrapper name="[name_wrapper]" type="[type_wrapper]" classname="[classname]" />
	… More <message-parameter/>
	… More <message-wrapper/>
  </response-message>
 </queue>
 ... More <queue></queue>
</queues>
```

Para facilitar el desarrollo de colas, no todos los elementos son obligatorios.


| Elemento    | Uso      | Varias instancias      | Descripción                                        |
| ----------- | ---------|------------------------|----------------------------------------------------|
| queues | **Obligatorio**| No  | Elemento raíz de la estructura de Queues  |
| [queue](#queue-element) |  **Obligatorio**| Si | Define la cola |
| [request-message](#request-message-element) |  Opcional | No | Se usa para definir la petición en la comunicación jms |
| [response-message](#response-message-element) |  Opcional | No | Se usa para definir la respuesta en la comunicación jms |
| [message-parameter](#message-parameter-element) |  Opcional | Si | Son los parámetros del mensaje de la petición o la respuesta |
| [message-wrapper](#message-wrapper-element) |  Opcional | No | Clase Java que construye la petición o lee la respuesta, en lugar de elementos `message-parameter` |
| [message-status](#message-status-element) |  Opcional | No | Campos de una respuesta `MAP` que transportan el tipo, el título y la descripción de la respuesta |

### Elemento queue {#queue-element}

El elemento `queue` tiene los siguientes atributos:

| Atributo    | Uso      | Tipo      |  Descripción                    |   Valores                                          |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| id | **Obligatorio** | String | Identificador de la cola          | **Nota:** Debe ser el **mismo nombre** del atributo queue en `query.xml` o `maintain.xml`|

### Elemento request-message {#request-message-element}

El elemento `request-message` tiene los siguientes atributos:

| Atributo    | Uso      | Tipo      |  Descripción                    |   Valores                                          |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| destination | **Obligatorio** | String | Nombre físico de la cola en el servidor JMS | **Nota:**  Debe existir un registro en la tabla **AweQue** con este nombre|
| type | **Obligatorio** | String | Tipo de mensaje a enviar/recibir | `MAP` (envía un conjunto de pares nombre-valor) o `TEXT` (envía información de texto)    |
| selector | Opcional | String | Establece un selector para filtrar el consumo de mensajes | **Ej.:** `selector="EUR"` Consumirá solo los mensajes cuya cabecera sea `"JMSType=EUR"`    |
| separator | Opcional | String | Carácter separador de los campos del mensaje | |
| timeout | Opcional | Long | Tiempo de espera del mensaje | |

### Elemento response-message {#response-message-element}

El elemento `response-message` define el mensaje que se recibe como respuesta de la cola. Tiene los mismos
atributos que el [elemento request-message](#request-message-element) (`destination`, `type`, `selector`,
`separator` y `timeout`) y contiene elementos [message-parameter](#message-parameter-element), o un elemento
`message-wrapper` o `message-status`.

### Elemento message-parameter {#message-parameter-element}

El elemento `message-parameter`, dentro de un `request-message` o un `response-message`, tiene los siguientes atributos:

| Atributo    | Uso      | Tipo      |  Descripción                    |   Valores                                          |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| id | **Obligatorio** | String | Identificador del parámetro | |
| type | **Obligatorio** | String | Tipo de dato del parámetro | `STRING`, `INTEGER`, `LONG`, `FLOAT`, `DOUBLE`, `BOOLEAN`, `DATE`, `TIME`, `TIMESTAMP`, `OBJECT`... (los mismos tipos que en los demás archivos de definición) |
| name | Opcional | String | Nombre del parámetro en el mensaje | |
| value | Opcional | String | Valor estático del parámetro | |
| list | Opcional | Boolean | Si el parámetro es una lista de valores | `true` o `false` |

### Elemento message-wrapper {#message-wrapper-element}

El elemento `message-wrapper`, dentro de un `request-message` o un `response-message`, asocia todo el mensaje a una clase
Java en lugar de listar elementos `message-parameter`. Una clase wrapper de respuesta implementa `ResponseWrapper`, y el resultado de su
`toServiceData()` es la respuesta de la cola.

| Atributo    | Uso      | Tipo      |  Descripción                    |   Valores                                          |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| type | **Obligatorio** | String | Cómo se asocia el mensaje a la clase | `XML` (el texto del mensaje, o un campo de un mensaje de tipo mapa, es un documento XML) u `OBJECT` (cada campo de un mensaje de tipo mapa se copia al campo de la clase con el mismo nombre) |
| classname | **Obligatorio** | String | Nombre completo de la clase wrapper | |
| name | Opcional | String | Campo de un mensaje `MAP` que contiene el valor envuelto | Obligatorio con `XML` en una respuesta `MAP`; debe omitirse con `OBJECT` |

### Elemento message-status {#message-status-element}

El elemento `message-status`, dentro de un `response-message` de tipo `MAP`, lee el resultado de la operación de la
respuesta y se lo muestra al usuario como la respuesta de cualquier otra acción. Sus atributos son los **nombres de los campos
del mapa** que contienen cada valor, no los valores en sí.

| Atributo    | Uso      | Tipo      |  Descripción                    |   Valores                                          |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| type | **Obligatorio** | String | Campo con el tipo de respuesta | `ok`, `error`, `warning` o `info` (no distingue mayúsculas de minúsculas) |
| translate | Opcional | String | Grupo de enumerados usado para traducir el valor recibido a un tipo de respuesta | **Ej.:** la respuesta envía `0` y el grupo de enumerados asocia `0` con `ok` |
| title | Opcional | String | Campo con el título del mensaje | |
| description | Opcional | String | Campo con el texto del mensaje | |

## **Mensajes síncronos** {#synchronous-messages}

### **Enviar y recibir datos** {#send-and-receive-data}

### **Enviar datos y recibir mensaje** {#send-data-and-receive-message}

## **Mensajes asíncronos** {#asynchronous-messages}

### **Suscribirse** {#subscribe}

### **Publicar** {#publish}

## Ejemplos {#examples}

---
id: queues
title: Queues definition
sidebar_label: Queues definition
---

The AWE Jms engine allows integration with queue servers. Both JMS typologies, **point to point** and **publish/subscribe**.

:::tip
Every element and attribute of JMS queues is listed in the generated [XSD reference](/reference/queues).
:::

The `Java Message Service` (JMS) API is a Java Message Oriented Middleware (MOM) API for sending messages between two or more clients. JMS is a part of the Java Platform, Enterprise Edition, and is defined by a specification developed under the Java Community Process as JSR 914. 

It is a messaging standard that allows application components based on the Java Enterprise Edition (Java EE) to create, send, receive, and read messages. It allows the communication between different components of a distributed application to be loosely coupled, reliable, and asynchronous.

* **Point-to-point model**

In point-to-point messaging system, messages are routed to an individual consumer which maintains a queue of "incoming" messages. This messaging type is built on the concept of message queues, senders, and receivers. Each message is addressed to a specific queue, and the receiving clients extract messages from the queues established to hold their messages. While any number of producers can send messages to the queue, each message is guaranteed to be delivered, and consumed by one consumer. Queues retain all messages sent to them until the messages are consumed or until the messages expire. If no consumers are registered to consume the messages, the queue holds them until a consumer registers to consume them.

<img alt="queue_point_to_point" src={require('@docusaurus/useBaseUrl').default('img/queue_point_to_point.png')}/>

* **Publish - Subscribe model**

The publish/subscribe model supports publishing messages to a particular message topic. Subscribers may register interest in receiving messages on a particular message topic. In this model, neither the publisher nor the subscriber knows about each other. A good analogy for this is an anonymous bulletin board zero or more consumers will receive the message.

There is a timing dependency between publishers and subscribers. The publisher has to create a message topic for clients to subscribe. The subscriber has to remain continuously active to receive messages, unless it has established a durable subscription. In that case, messages published while the subscriber is not connected will be redistributed whenever it reconnects.

<img alt="topic" src={require('@docusaurus/useBaseUrl').default('img/topic.png')}/>

:::info
**Note:** All queues  are defined in the `Queues.xml` file at **global folder**. View [project structure](../guides/project-structure.md#global-folder)  for more info.
:::

## **Queue xml structure**

The full queue structure is the next one:

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

For easier development of queues, not all elements are required.


| Element     | Use      | Multiples instances    | Description                                        |
| ----------- | ---------|------------------------|----------------------------------------------------|
| queues | **Required**| No  | Root element of Queues structure  |
| [queue](#queue-element) |  **Required**| Yes | It outlines the queue |
| [request-message](#request-message-element) |  Optional | No | Used to define the request in the jms comunication |
| [response-message](#response-message-element) |  Optional | No | Used to define the response in the jms comunication |
| [message-parameter](#message-parameter-element) |  Optional | Yes | Are parameters of message for the request or response |
| [message-wrapper](#message-wrapper-element) |  Optional | No | Java class that builds the request or reads the response, instead of `message-parameter` elements |
| [message-status](#message-status-element) |  Optional | No | Fields of a `MAP` response that carry the answer type, title and description |

### Queue element

The `queue` element has the following attributes:

| Attribute   | Use      | Type      |  Description                    |   Values                                           |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| id | **Required** | String | Queue identifier          | **Note:** Must be the **same name** of attribute queue in `query.xml` or `maintain.xml`|

### request-message element

The `request-message` element has the following attributes:

| Attribute   | Use      | Type      |  Description                    |   Values                                           |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| destination | **Required** | String | Physical name of queue in JMS Server | **Note:**  Must be exist one record in **AweQue** table with this name|
| type | **Required** | String | Message type to send/receive | `MAP` (send a set of name-value pairs) or `TEXT` (sent a text info)    |
| selector | Optional | String | Set a a selector to filter the message consumption | **Ex.:** `selector="EUR"` It will consume only messages whose header `"JMSType=EUR"`    |
| separator | Optional | String | Separator character of the message fields | |
| timeout | Optional | Long | Time to wait for the message | |

### response-message element

The `response-message` element defines the message that is received as the answer of the queue. It has the same
attributes as the [request-message element](#request-message-element) (`destination`, `type`, `selector`,
`separator` and `timeout`) and contains [message-parameter](#message-parameter-element) elements, or a
`message-wrapper` or `message-status` element.

### message-parameter element

The `message-parameter` element, inside a `request-message` or a `response-message`, has the following attributes:

| Attribute   | Use      | Type      |  Description                    |   Values                                           |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| id | **Required** | String | Identifier of the parameter | |
| type | **Required** | String | Data type of the parameter | `STRING`, `INTEGER`, `LONG`, `FLOAT`, `DOUBLE`, `BOOLEAN`, `DATE`, `TIME`, `TIMESTAMP`, `OBJECT`... (the same types as the other definition files) |
| name | Optional | String | Name of the parameter in the message | |
| value | Optional | String | Static value of the parameter | |
| list | Optional | Boolean | Whether the parameter is a list of values | `true` or `false` |

### message-wrapper element

The `message-wrapper` element, inside a `request-message` or a `response-message`, maps the whole message to a Java
class instead of listing `message-parameter` elements. A response wrapper class implements `ResponseWrapper`, and its
`toServiceData()` result is the answer of the queue.

| Attribute   | Use      | Type      |  Description                    |   Values                                           |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| type | **Required** | String | How the message is mapped to the class | `XML` (the message text, or one field of a map message, is an XML document) or `OBJECT` (each field of a map message is copied to the class field with the same name) |
| classname | **Required** | String | Fully qualified name of the wrapper class | |
| name | Optional | String | Field of a `MAP` message that holds the wrapped value | Required with `XML` on a `MAP` response; must be omitted with `OBJECT` |

### message-status element

The `message-status` element, inside a `response-message` of type `MAP`, reads the result of the operation from the
response and shows it to the user like the answer of any other action. Its attributes are the **names of the map
fields** that hold each value, not the values themselves.

| Attribute   | Use      | Type      |  Description                    |   Values                                           |
| ----------- | ---------|-----------|---------------------------------|----------------------------------------------------|
| type | **Required** | String | Field with the answer type | `ok`, `error`, `warning` or `info` (case insensitive) |
| translate | Optional | String | Enumerated group used to translate the received value into an answer type | **Ex.:** the response sends `0` and the enumerated group maps `0` to `ok` |
| title | Optional | String | Field with the title of the message | |
| description | Optional | String | Field with the text of the message | |

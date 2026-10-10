---
id: service
title: Definición de servicios
sidebar_label: Definición de servicios
---

Las operaciones de servicio están diseñadas para realizar tratamientos y cálculos específicos fuera de las utilidades de AWE.

:::tip
Todos los elementos y atributos de los servicios están listados en la [referencia XSD](/reference/services) generada.
:::

Actualmente hay dos tipos de servicios declarados en el motor de AWE: **servicios Java** y **servicios web**

:::info
**Nota:** Todos los servicios se definen en el archivo `Services.xml` de la **carpeta global**. Consulta la [estructura del proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## **Estructura global de los servicios** {#global-service-structure}

La estructura xml de los servicios es:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation = "https://aweframework.gitlab.io/awe/docs/schemas/services.xsd">
  <service id="[Service Id]">
    <java classname="[Java class]" method="[Java method]">
      <service-parameter type="[Type]" name="[Parameter name]" qualifier="[Bean name]" />
      ... (More <service_parameter>)
    </java>
  </service>
  <service id="[Service Id]">
    <microservice name="[Microservice name]" method="[REST method]" endpoint="[Service endpoint]">
      <service-parameter type="[Type]" name="[Parameter name]" />
      ... (More <service_parameter>)
    </microservice>
  </service>
  <service id="[Service Id]">
    <rest method="[REST method]" endpoint="[Service endpoint]" wrapper="[REST service wrapper]">
      <service-parameter type="[Type]" name="[Parameter name]" />
      ... (More <service_parameter>)
    </rest>
  </service>  
  ... (More <service>)
</services>
```

Para facilitar el desarrollo de servicios, no todos los elementos son obligatorios.


| Elemento                                        | Uso             | Varias instancias | Descripción                                                                                           |
|-------------------------------------------------|-----------------|-------------------|-------------------------------------------------------------------------------------------------------|
| services                                        | **Obligatorio** | No                | Elemento raíz del archivo xml de servicios                                                            |
| [service](#service-element)                     | **Obligatorio** | Sí                | Define el servicio. También describe el **tipo de servicio** (servicio java o servicio web)          |
| [java](#java-element)                           | **Opcional**    | No                | Se usa para definir servicios java                                                                    |
| [microservice](#microservice-element)           | **Opcional**    | No                | Se usa para definir microservicios                                                                    |
| [rest](#rest-service-element)                   | **Opcional**    | No                | Se usa para definir servicios rest                                                                    |
| [service-parameter](#service-parameter-element) | **Opcional**    | No                | Se usa para pasar parámetros al servicio                                                              |

### Elemento service {#service-element}

El elemento service tiene los siguientes atributos:

| Atributo                     | Uso             | Tipo   | Descripción                                          | Valores                                                               |
|------------------------------|-----------------|--------|------------------------------------------------------|-----------------------------------------------------------------------|
| id                           | **Obligatorio** | String | Nombre del servicio                                  | **Nota:** El id debe ser único                                        |
| launch-phase                 | Opcional        | String | Lanza el servicio en determinados puntos de la aplicación | `APPLICATION_START`, `APPLICATION_END`,  `CLIENT_START`, `CLIENT_END` |

### Elemento java {#java-element}

El elemento java tiene los siguientes atributos:

| Atributo    | Uso             | Tipo   | Descripción                                                       | Valores                                                                    |
|-------------|-----------------|--------|-------------------------------------------------------------------|----------------------------------------------------------------------------|
| classname   | **Obligatorio** | String | Nombre de la clase del servicio java                              | Ej.: `classname="com.almis.awe.core.services.controller.AccessController"` |
| method      | **Obligatorio** | String | Nombre del método de la clase que se va a ejecutar                | Ej.: `method="login"`                                                      |
| qualifier   | Opcional        | String | Nombre del bean calificador. Si usa la anotación de spring `@Qualifier` | Ej.: `qualifier="myBean"`                                            |

### Elemento microservice {#microservice-element}

El elemento microservice tiene los siguientes atributos:

| Atributo     | Uso             | Tipo    | Descripción                                   | Valores                                                                                                          |
|--------------|-----------------|---------|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| name         | **Obligatorio** | String  | Nombre del servicio web                       | **Nota:** Debe ser único                                                                                         |
| method       | **Obligatorio** | String  | Método REST.                                  | **GET**: Envía los parámetros como parte del endpoint - POST**: Envía los parámetros en el cuerpo de la petición |
| endpoint     | **Obligatorio** | String  | Ruta de la llamada REST                       | Ej: /data/ServiceData o /maintain/ServiceMaintain                                                                |
| wrapper      | Opcional        | String  | Nombre de clase para envolver la respuesta de la llamada REST | Ej: com.almis.awe.test.bean.Postman                                                              |
| content-type | Opcional        | String  | Forma de enviar los parámetros                | `URLENCODED` (por defecto), `JSON`                                                                               |
| cacheable    | Opcional        | Boolean | Se usa para establecer el servicio como cacheable | El valor por defecto es `false`                                                                              |

### Elemento de servicio REST {#rest-service-element}

El elemento de servicio REST tiene los siguientes atributos:

| Atributo     | Uso             | Tipo    | Descripción                                   | Valores                                                                                                            |
|--------------|-----------------|---------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| server       | Opcional        | String  | Propiedad del servidor REST                   | Se usa para recuperar la propiedad `rest.server.[server]`                                                          |
| method       | **Obligatorio** | String  | Método REST.                                  | **GET**: Envía los parámetros como parte del endpoint - **POST**: Envía los parámetros en el cuerpo de la petición |
| endpoint     | **Obligatorio** | String  | Ruta de la llamada REST                       | Ej: /data/ServiceData o /maintain/ServiceMaintain                                                                  |
| wrapper      | Opcional        | String  | Nombre de clase para envolver la respuesta de la llamada REST | Ej: com.almis.awe.test.bean.Postman                                                                |
| content-type | Opcional        | String  | Forma de enviar los parámetros                | `URLENCODED` (por defecto), `JSON`                                                                                 |
| cacheable    | Opcional        | Boolean | Se usa para establecer el servicio como cacheable | El valor por defecto es `false`                                                                                |

### Elemento service-parameter {#service-parameter-element}

El elemento service-parameter son parámetros pasados desde una consulta o un mantenimiento al servicio. Tiene los siguientes atributos:

| Atributo    | Uso             | Tipo      |  Descripción                                |   Valores                                      |
| ----------- | --------------- |-----------|---------------------------------------------|----------------------------------------------- |
| name        | **Obligatorio** | String    | Nombre del parámetro del servicio           | **Nota:** Debe ser único                       |
| type        | **Obligatorio** | String    | Tipo del parámetro del servicio             | Los valores posibles son: `STRING`, `INTEGER`, `FLOAT`, `DOUBLE`, `OBJECT`, `JSON`, `DATE`, `TIME` o `TIMESTAMP` |
| value       | Opcional        | String    | Para establecer el parámetro con un valor estático |                                      |
| bean-class  | Opcional        | String    | Gestiona el parámetro como un Java Bean     | El tipo debe ser `OBJECT` o `JSON`             |

## **Servicios Java** {#java-services}

Son servicios para ejecutar código `java`. Su estructura xml es:

```xml
<service id="[service_name]">
  <java classname="[service_classname]" method="[service_method]">
    <service-parameter type="String" name="[parameter_name]"/>
    ... (more service parameters)
  </java>
</service>
```

### Ejemplos de servicios Java {#java-service-examples}

Definición de servicio con parámetros

```xml
<!-- Store a session variable -->
<service id="insertSchedulerTask">
  <java classname="com.almis.awe.scheduler.controller.SchedulerController" method="insertSchedulerTask">
    <service-parameter name="IdeTsk" type="INTEGER" />
    <service-parameter name="SendStatus" type="INTEGER" list="true" />
    <service-parameter name="SendDestination" type="INTEGER" list="true" />
  </java>
</service>

```

Definición de clase Java con parámetros

```java
@Service
public class SchedulerService extends ServiceConfig {
  /**
   * Insert and schedule a new task
   *
   * @param taskId Task identifier
   * @param sendStatus Status to send list
   * @param sendDestination Destination target list
   * @return ServiceData
   */
  public ServiceData insertSchedulerTask(Integer taskId, List<Integer> sendStatus, List<Integer> sendDestination) throws AWException {
    // Launch function
    // ...
  } 
}
```

Definición de servicio sin parámetros

```xml
<!-- Get screen configuration at begining-->
<service id="LoaScrCfg" launch-phase="APPLICATION_START">
  <java classname="com.almis.awe.core.services.controller.ScreenController" method="initScreenConfigurations"/>
</service>
```

Definición de clase Java sin parámetros

```java
@Service
public class ScreenService extends ServiceConfig {
  /**
   * Initialize singleton with screens configurations info
   *
   */
  public void initScreenConfigurations() {
    // Launch function
    // ...
  } 
}
```

### Cargar beans desde parámetros {#load-beans-from-parameters}

Este es un ejemplo para cargar un bean a partir de varios parámetros

**Ejemplo 1**: Cargar un único bean con parámetros

Definición de consulta para cargar un parámetro bean (cada variable rellenará un atributo del bean)

```xml
<query id="testServiceBeanParameter" service="testServiceBeanParameter">
  <variable id="name" type="STRING" name="name"/>
  <variable id="rotationPeriod" type="STRING" name="rotationPeriod"/>
  <variable id="orbitalPeriod" type="STRING" name="orbitalPeriod"/>
  <variable id="diameter" type="STRING" name="diameter"/>
  <variable id="climate" type="STRING" name="climate"/>
  <variable id="gravity" type="STRING" name="gravity"/>
  <variable id="terrain" type="STRING" name="terrain"/>
  <variable id="population" type="LONG" name="population"/>
  <variable id="created" type="DATE" name="created"/>
  <variable id="edited" type="DATE" name="edited"/>
  <variable id="url" type="STRING" name="url"/>
</query>
```

Definición de servicio con un parámetro bean

```xml

<service id="testServiceBeanParameter">
  <java classname="com.almis.awe.service.DummyService" method="getDummyData">
    <service-parameter type="OBJECT" bean-class="com.almis.awe.test.bean.Planet"/>
  </java>
</service>
```

Definición de clase Java con un parámetro bean

```java
@Service
public class DummyService extends ServiceConfig { 
  /**
   * Retrieve dummy data
   * @param planet Planet bean
   * @return Service data
   */
  public ServiceData getDummyData(Planet planet) {
    ServiceData serviceData = new ServiceData();
    // ...
    return serviceData;
  }
}
```

**Ejemplo 2**: Cargar una lista de beans con parámetros

Definición de consulta para cargar un parámetro bean (cada variable es una lista de parámetros que rellenará un atributo del bean)

```xml
<query id="testServiceBeanParameterList" service="testServiceBeanParameterList">
  <variable id="name" type="STRING" name="name"/>
  <variable id="rotationPeriod" type="STRING" name="rotationPeriod"/>
  <variable id="orbitalPeriod" type="STRING" name="orbitalPeriod"/>
  <variable id="diameter" type="STRING" name="diameter"/>
  <variable id="climate" type="STRING" name="climate"/>
  <variable id="gravity" type="STRING" name="gravity"/>
  <variable id="terrain" type="STRING" name="terrain"/>
  <variable id="population" type="LONG" name="population"/>
  <variable id="created" type="DATE" name="created"/>
  <variable id="edited" type="DATE" name="edited"/>
  <variable id="url" type="STRING" name="url"/>
</query>
```

Definición de servicio con una lista de parámetros bean

```xml

<service id="testServiceBeanParameterList">
  <java classname="com.almis.awe.service.DummyService" method="getDummyData">
    <service-parameter type="OBJECT" bean-class="com.almis.awe.test.bean.Planet" list="true"/>
  </java>
</service>
```

Definición de clase Java con una lista de parámetros bean

```java
@Service
public class DummyService extends ServiceConfig { 
  /**
   * Retrieve dummy data
   * @param planetList Planet bean list
   * @return Service data
   */
  public ServiceData getDummyData(List<Planet> planetList) {
    ServiceData serviceData = new ServiceData();
    // ...
    return serviceData;
  }
}
```

**Ejemplo 3**: Cargar un nodo json desde una rejilla

Definición de consulta para cargar un parámetro bean (cada variable es una lista de parámetros que rellenará un atributo del bean)

```xml
<query id="testLoadJsonAddress" service="testLoadJsonAddress">
  <variable id="address" type="OBJECT" name="[gridId].selectedRowAddress"/>
</query>
```

Definición de servicio con un parámetro Json

```xml

<service id="testLoadJsonAddress">
  <java classname="com.almis.awe.service.DummyService" method="getJsonAddress">
    <service-parameter type="JSON"/>
  </java>
</service>
```

Definición de clase Java con un parámetro json

```java
@Service
public class DummyService extends ServiceConfig { 
  /**
   * Retrieve dummy data
   * @param address Json address
   * @return Service data
   */
  public ServiceData getJsonAddress(JsonNode address) {
    ServiceData serviceData = new ServiceData();
    // ...
    return serviceData;
  }
}
```

**Ejemplo 4**: Cargar una lista de datos almacenada desde una fila de una rejilla

Definición de consulta

```xml
<query id="testLoadJsonBean" service="testLoadJsonBean">
  <variable id="storedDatalist" type="OBJECT" name="storedData.selected"/>
</query>
```

Definición de servicio con un parámetro Json

```xml

<service id="testLoadJsonBean">
  <java classname="com.almis.awe.service.DummyService" method="getJsonBean">
    <service-parameter type="JSON" bean-class="com.almis.awe.model.dto.DataList"/>
  </java>
</service>
```

Definición de clase Java con un parámetro json

```java
@Service
public class DummyService extends ServiceConfig { 
  /**
   * Retrieve dummy data
   * @param datalist DataList from Json
   * @return Service data
   */
  public ServiceData getJsonBean(DataList datalist) {
    ServiceData serviceData = new ServiceData();
    // ...
    return serviceData;
  }
}
```

### Cómo preparar los valores de los parámetros desde Java {#how-to-prepare-parameter-values-from-java}

En los servicios que extienden `ServiceConfig`, prepara los parámetros de consulta y mantenimiento con una copia mutable explícita.
No modifiques la petición activa como patrón por defecto.

**Servicio Java**

```java
  @Service
  public class UserService extends ServiceConfig { 

    /**
     * Get user information
     *
     * @param id User id
     * @return ServiceData
     */
    private ServiceData getUserData(String id) throws AWException {
      DataList userData = null;
      ServiceData srvDat = null;

      ObjectNode parameters = getMutableRequestParameters();
      putRequestParameter(parameters, "opeId", id);

      return getBean(QueryService.class).launchQuery("getUserData", parameters);
    }
  }

```

**Definición de consulta**

```xml
<query id="getUserData">
  <table id="ope" alias="o" />
  <field id="l1_nom" alias="nom" table="o" />
  <field id="l1_lan" alias="lan" table="o" />
  <field id="EmlSrv" alias="eml" table="o" />
  <field id="OpeNam" alias="nam" table="o" />
  <where>
    <and>
      <filter left-field="IdeOpe" left-table="o" condition="eq" right-variable="OpeId" />
    </and>
  </where>
  <variable id="OpeId" type="STRING" name="opeId" />
</query>

```

Usa el mismo patrón de `ObjectNode` con `launchMaintain("targetName", parameters)` al llamar a destinos de mantenimiento.
`QueryUtil` es la API de más bajo nivel para las clases que no extienden `ServiceConfig`.

## **Microservicios** {#microservices}

Los microservicios son conectores con servicios definidos mediante REST. Su estructura xml es:

```xml
<service id="[service_name]">
  <microservice name="alu-microservice" method="GET" endpoint="/[data/maintain]/[service-name]/{param1}">
    <service-parameter name="param1" type="STRING"/>
  </microservice>
</service>
```

### Ejemplos de microservicios {#microservice-examples}

```xml
<!-- GET BACKOFFICE NUMBER -->
<service id="BocNum">
  <microservice name="alu-microservice" method="POST" endpoint="/data/BilGetBoc">
    <service-parameter type="STRING" name="ent" />
    <service-parameter type="STRING" name="suc" />
    <service-parameter type="STRING" name="sns" />
    <service-parameter type="STRING" name="cap" />
    <service-parameter type="STRING" name="prd" />
  </microservice>
</service>
```

```xml
<!-- CONTROL CORRESPONSAL -->
<service id="BilCtlCrr">
  <microservice name="alu-microservice" method="POST" endpoint="/maintain/BilCtlCrr">
    <service-parameter type="STRING" name="Ent" />
    <service-parameter type="STRING" name="Liq" />
    <service-parameter type="STRING" name="Crr" />
  </web>
</service>
```
> **Nota:** Los atributos `microservice.name` son opcionales. El atributo `name` se usa para permitir sobrescribir el nombre del microservicio y establecer la configuración de autenticación.
>
> Puedes definir parámetros específicos del microservicio que se enviarán en todas las peticiones:
>
>```properties
> ################################################
> # Microservice properties
> ################################################
> awe.rest.services.myservice.base-url=http://localhost:18081
> # Microservice parameters
> awe.rest.services.myservice.parameters[0].name=database
> awe.rest.services.myservice.parameters[0].value=_database_
> awe.rest.services.myservice.parameters[0].type=variable
> awe.rest.services.myservice.parameters[1].name=username
> awe.rest.services.myservice.parameters[1].value=user
> awe.rest.services.myservice.parameters[1].type=session
> awe.rest.services.myservice.parameters[2].name=currentDate
> awe.rest.services.myservice.parameters[2].value=currentDate
> awe.rest.services.myservice.parameters[2].type=request
> awe.rest.services.myservice.parameters[3].name=numPar
> awe.rest.services.myservice.parameters[3].value=3
>```

## **Servicios REST** {#rest-services}

Los servicios REST son muy útiles para conectar con APIs REST. Su estructura xml es:

```xml
<service id="[service_name]">
  <rest server="server" method="GET" endpoint="/[data/maintain]/[service-name]/{param1}" wrapper="com.almis.test.ServiceDataWrapper" content-type="URLENCODED">
    <service-parameter name="param1" type="STRING"/>
  </rest>
</service>
```

> **Nota:** Los atributos `server` y `wrapper` son opcionales. El atributo `server` se usa para recuperar la propiedad `rest.server.[server]`, 
> que se añade a la url definida en `endpoint`. El atributo `wrapper` define un nombre de clase que se usará para gestionar la respuesta de la llamada REST
> y traducirla a una clase `ServiceData`, adecuada para AWE.  
> 
> Ejemplo de configuración de servidor REST:
>
>```properties
> awe.rest.services.core.base-url=http://localhost:18080/core
> awe.rest.services.core.authentication.type=basic
> awe.rest.services.core.authentication.username=rest_username
> awe.rest.services.core.authentication.password=ENC(rest_password_encoded)
>```

### Ejemplos de servicios REST {#rest-services-examples}

* **Llamada a una URL local con método GET sin parámetros**
```xml
<service id="testSimpleRestGet">
  <rest method="GET" endpoint="http://localhost:18089/testapi/simple"/>
</service>
```

* **Llamada a una URL local con método POST sin parámetros**
```xml
<service id="testSimpleRestPost">
  <rest method="POST" endpoint="http://localhost:18089/testapi/simple"/>
</service>
```

* **Llamada a una URL local con método GET sin parámetros**
```xml
<service id="testComplexRestGet">
  <rest method="GET" endpoint="http://localhost:18089/testapi/complex/QtyTst"/>
</service>
```

* **Llamada a una URL local con método POST sin parámetros**
```xml
<service id="testComplexRestPost">
  <rest method="POST" endpoint="http://localhost:18089/testapi/complex/testInclude"/>
</service>
```

* **Llamada a una URL local con método GET con parámetros de url**
```xml
<service id="testComplexRestGetParameters">
  <rest server="local" method="GET" endpoint="/testapi/complex/QtyTst{name}/{value}">
    <service-parameter type="STRING" name="name" />
    <service-parameter type="STRING" name="value" />
  </rest>
</service>
```

* **Llamada a una URL local con método POST con parámetros codificados en url (por defecto)**
```xml
<service id="testComplexRestPostParameters">
  <rest method="POST" endpoint="http://localhost:18089/testapi/complex/parameters/testRestParameters">
    <service-parameter type="INTEGER" name="value" />
  </rest>
</service>
```

* **Llamada a una URL local con método POST con parámetros codificados en json**
```xml
<service id="testComplexRestPostParametersJson">
  <rest server="local" method="POST" endpoint="/testapi/complex/parameters/json/testRestParameters" content-type="JSON">
    <service-parameter type="INTEGER" name="value" />
  </rest>
</service>
```

* **Llamada a una URL externa con método GET sin parámetros y un wrapper**
```xml
<service id="testExternalRest">
  <rest server="islandia" method="GET" endpoint="/concerts" wrapper="com.almis.awe.test.bean.Concerts"/>
</service>
```

* **Llamada a una URL externa con método GET sin parámetros y un wrapper**
```xml
<service id="testPostman">
  <rest server="postman" method="GET" endpoint="/gzip" wrapper="com.almis.awe.test.bean.Postman"/>
</service>
```

---
id: protection-mechanism
title: Security mechanisms in Awe applications
sidebar_label: Protection mechanisms
---

<img style={{ width: "40%", margin: "10% 30% 10% 30%" }}
alt="AWE security"
src={require('@docusaurus/useBaseUrl').default('img/undraw_security.svg')}
/>

## Arquitectura {#architecture}

Awe es un framework de servidor, donde toda la lógica de su aplicación, negocio y interfaz de usuario reside en el servidor. A diferencia de los frameworks orientados a cliente, las aplicaciones Awe nunca exponen su código al navegador donde las vulnerabilidades pueden ser aprovechadas por un atacante.

It uses **`Spring Security`** utilities to manage and configure all safety-related aspects.

## Bibliotecas de terceros {#3rd-party-libraries}

AWE actualiza constantemente las dependencias a librerías de terceros cuando se liberan parches de seguridad para ellas. Cuando es necesario se crean nuevas versiones de mantenimiento de Awe para aplicar parches. Furthermore, AWE has a public `SonarCloud` server to be
audited and constantly adapt to new security flaws.
You can check [here](https://sonarcloud.io/component_measures?id=aweframework_awe&metric=Security).

## Protección contra ataques CSRF {#cross-site-request-forgery-csrfxsrf}

Todas las peticiones entre el cliente y el servidor incluyen un token CSRF específico de sesión de usuario. Awe maneja toda comunicación entre el servidor y el cliente, por lo que no necesitas recordar incluir los tokens CSRF manualmente.

```properties title="Security request headers"
Authorization: f910520d-28b8-4a2b-6e98-f32822bb1677
Sec-Fetch-Site: same-origin
X-XSRF-TOKEN: faad4d18-035a-4394-ab5f-be3bae2a1a09
Cookie: XSRF-TOKEN=faad4d18-035a-4394-ab5f-be3bae2a1a09; JSESSIONID=7177A217096E0BF9E4D47C967C74431D
```

## Cross-Site Scripting (XSS)

Awe tiene protección integrada contra ataques de cross-site scripting (XSS). Awe convierte todos los datos para usar entidades HTML antes de que los datos se procesen en el navegador del usuario.

El filtrado está habilitado por defecto, así que al añadir el encabezado normalmente solo asegura que está habilitado e indica al navegador qué hacer cuando se detecta un ataque XSS.

```properties
X-XSS-Protection=1; mode=block
```

## SSL y HTTPS {#ssl-and-https}

Awe recomienda siempre a los desarrolladores que establezcan <code>endpoints</code> seguros y ejecuten toda la comunicación exclusivamente bajo HTTPS.
Awe funciona directamente con HTTPS sin necesidad de que el desarrollador deba configurar nada en su código de aplicación.
Por favor, consulte la documentación de su contenedor servlet para obtener detalles sobre cómo configurar HTTPS en su servidor.

## Validación de datos {#data-validation}

En las aplicaciones desarrolladas con Awe, el API de enlace de datos soporta la validación de datos en el servidor, que no puede ser sobrepasado por ataques en el lado del cliente.
Sin embargo, Awe tiene una acción de validación en el lado del cliente para hacer una doble comprobación y aumentar la capacidad de respuesta de la aplicación,
pero el desarrollador debe ser consciente de que estas acciones deben ser utilizados exclusivamente para conveniencia, ya que son fácilmente eludidos en el navegador.
Además, el desarrollador es libre de usar cualquier API de Java para validar los datos, incluyendo la conexión a servicios externos.
There is also a built-in integration with Java’s Bean Validation (`JSR 303`) standard.

## Inyección SQL {#sql-injections}

Awe es un framework de IU de backend-agnóstico, no trata directamente con acceso backend; en cambio, utiliza un framework backend
(e.. Spring Data) para gestionar esto. Awe provides mitigation for SQL injections using techniques like _Parameterized Queries_ with QueryDSL.
Internally uses `PreparedStatement`and _User data sanitization_.

```java title="QueryDsl Example"
QCustomer customer = new QCustomer("Foo");

SQLTemplates dialect = new HSQLDBTemplates(); // SQL-dialect
SQLQuery query = new SQLQueryImpl(connection, dialect); 
List<String> lastNames = query.from(customer)
    .where(customer.firstName.eq("Foo"))
    .list(customer.lastName);
```

```sql
SELECT c.last_name FROM customer c WHERE c.first_name = 'Foo'
```


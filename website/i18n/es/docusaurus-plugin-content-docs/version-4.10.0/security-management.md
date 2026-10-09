---
id: security
title: Seguridad en aplicaciones realizadas con Awe
sidebar_label: Seguridad
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

## Autenticación y autorización {#authentication-and-authorization}

Awe te permite elegir qué sistema de autenticación y autorización quieres usar, en lugar de definir uno específico.
Awe is fully compatible with the most used security solutions in the Spring Boot ecosystem like `In memory`, `Database`, `LDAP`, `OAuth`, `Oauth2`, ...

:::info You can visit [this](https://spring.io/guides/topicals/spring-security-architecture) for more info.:::

### Spring Security en Awe {#spring-security-in-awe}

Awe proporciona <code>beans</code> de configuración para gestionar la seguridad en su aplicación. Puede usarlos o sobrescribirlos y crear su método de autenticación personalizado.
The security configuration is in `SecurityConfig` and `AWEScreenSecurityAdapter` classes and select the authentication method that you want.

```shell title="Configuration properties"
################################################
# Authentication
################################################
# Authentication mode (ldap | bbdd | in_memory | custom)
awe.security.auth-mode=bbdd

################################################
# Custom authentication
################################################
#Provider class beans, separated by comma for multiple providers.
awe.security.auth-custom-providers=
```

You can always create your own Http web security config class extending `WebSecurityConfigurerAdapter`.

```java title="Custom Http security configuration"
@Configuration
public class CustomSecurityConfig extends WebSecurityConfigurerAdapter {
  
   /**
   * Spring security configuration
   *
   * @param http Http security object
   * @throws Exception Configure error
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Your custom configuration
  }
}
```

### Authentication with Azure EntraID

AWE provide Azure oauth2 authentication service integration. Uses the Spring Boot Starter for Microsoft Entra ID enables you to connect your web application to a Microsoft Entra tenant and protect your resource server with Microsoft Entra ID. It uses the Oauth 2.0 protocol to protect web applications and resource servers.

<img style={{ width: "70%", margin: "30px 15% 0% 15%" }}
alt="Azure Entra ID"
src={require('@docusaurus/useBaseUrl').default('img/Azure_entraID.png')}
/>

<div style={{textAlign:"center",fontStyle:"italic"}}>Azure Entra ID</div>

To enable Azure oauth2 active directory, you have to add spring-cloud-azure starter and configure your organization tenantId and application ID and secret.

```xml title="Add dependency"
    <dependency>
      <groupId>com.azure.spring</groupId>
      <artifactId>spring-cloud-azure-starter-active-directory</artifactId>
    </dependency>
```

```properties title="Configure azure EntraID properties"
# Enable related features.
spring.cloud.azure.active-directory.enabled=true
# Specifies your Active Directory ID:
spring.cloud.azure.active-directory.profile.tenant-id={CONFIGURE YOUR TENANT ID}
# Specifies your App Registration's Application ID:
spring.cloud.azure.active-directory.credential.client-id={CONFIGURE YOUR CLIENT ID}
# Specifies your App Registration's secret key:
spring.cloud.azure.active-directory.credential.client-secret={CONFIGURE YOUR SECRET KEY}
```

:::info You can visit [this](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/spring-boot-starter-for-azure-active-directory-developer-guide?tabs=SpringCloudAzure4x) for more info.:::

By default, if the user logged in the application with this  doesn't exist in database, it  will be provisioned by registering it by adding a new record in the user table.
If you do not want this behavior, you can disable it setting false the configuration property `awe.security.auto-provision-use`.

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

## Autenticación de dos factores (2FA) {#two-factor-authentication-2fa}

We've recently developed a new two-factor authentication system based on _authentication apps_ such as **Google Authenticator**.

There are three ways to manage this two-factor authentication in AWE based on the `awe.totp.security.enabled` property:

- `disabled`: Two-factor authentication is disabled and it won't ask for a temporal code on access.
- `optional`: The user **can enable** two-factor authentication on the **settings screen** and temporal code will be asked on login.

<img style={{ width: "70%", margin: "30px 15% 0% 15%" }}
alt="Settings screen"
src={require('@docusaurus/useBaseUrl').default('img/security-settings.png')}
/>

<div style={{textAlign:"center",fontStyle:"italic"}}>Pantalla de configuración de seguridad</div>

<img style={{ width: "60%", margin: "30px 20% 0% 20%" }}
alt="TOTP Code screen"
src={require('@docusaurus/useBaseUrl').default('img/totp-code.png')}
/>

<div style={{textAlign:"center",fontStyle:"italic",marginBottom:"30px"}}>Pantalla de código TOTP</div>

- `force`: On login, **if user has not enabled two-factor authentication**, a screen will raise with the QR code to force the user to
  enable two-factor authentication. Después de esa pantalla, se pedirá al usuario el código temporal basado en el código secreto previamente generado.

<img style={{ width: "40%", margin: "30px 30% 0% 30%" }}
alt="Force two-factor authentication screen"
src={require('@docusaurus/useBaseUrl').default('img/force-2fa.png')}
/>

<div style={{textAlign:"center",fontStyle:"italic"}}>Pantalla de seguridad de dos factores forzada</div>


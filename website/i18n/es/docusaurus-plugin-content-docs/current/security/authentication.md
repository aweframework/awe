---
id: security-authentication
title: Autenticación
sidebar_label: Autenticación
---

<img style={{ width: "40%", margin: "10% 30% 10% 30%" }}
alt="Seguridad de AWE"
src={require('@docusaurus/useBaseUrl').default('img/undraw_security-on_btwg.svg')}
/>

# Autenticación y autorización {#authentication-and-authorization}
Awe te permite elegir qué sistema de autenticación y autorización quieres usar, en lugar de incluir uno específico.
Awe es totalmente compatible con las soluciones de seguridad más utilizadas en el ecosistema Spring Boot como `In memory`, `Database`, `LDAP`, `OAuth`, `Oauth2`, ...

:::info
Puedes visitar [este enlace](https://spring.io/guides/topicals/spring-security-architecture) para más información.
:::

## Spring Security en Awe {#spring-security-in-awe}
Awe proporciona beans de configuración para gestionar la seguridad en tu aplicación. Puedes usarlos o sobrescribirlos y crear tu propio método de autenticación.
La configuración de seguridad está en las clases `SecurityConfig` y `AWEScreenSecurityAdapter` y selecciona el método de autenticación que quieras.

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

Siempre puedes crear tu propia clase de configuración de seguridad web Http extendiendo `WebSecurityConfigurerAdapter`.

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

## Caducidad de la contraseña (inicio de sesión local) {#password-expiration-local-login}

El parámetro de aplicación `PwdExp` establece el número de días que una contraseña permanece válida. Se aplica al inicio de sesión local
(modo de autenticación `bbdd`, usuarios almacenados en la tabla `ope`). La fecha del último cambio de contraseña es la columna `l1_psd`
del usuario.

| `PwdExp` | Último cambio de contraseña | Resultado |
|---|---|---|
| No definido o inactivo | Cualquiera, incluso nunca | La contraseña nunca caduca. |
| Definido (por ejemplo `30`) | Hace menos de `PwdExp` días | Se acepta la contraseña. |
| Definido | Hace `PwdExp` días o más | La contraseña ha caducado: el usuario tiene que [fijar una nueva](#login-with-an-expired-password). |
| Definido | Nunca (fecha vacía) | La contraseña ha caducado: el usuario tiene que [fijar una nueva](#login-with-an-expired-password) en el primer inicio de sesión. |

### Contraseñas fijadas por un administrador {#passwords-set-by-an-administrator}

Un usuario que nunca ha cambiado la contraseña no tiene fecha de cambio. Este es el caso de un usuario creado sin fecha de
cambio y de un usuario cuya contraseña fija un administrador en la pantalla de usuarios (la pantalla borra la fecha).

**Decisión:** con `PwdExp` definido, una contraseña fijada por un administrador cuenta como caducada, así que el usuario tiene que cambiarla en el
primer inicio de sesión («debe cambiarla en el primer acceso»). El administrador conoce la nueva contraseña, por lo que no debería seguir siendo válida
como secreto propio del usuario. Sin `PwdExp` la fecha ausente no tiene efecto. Si no quieres este comportamiento para una
cuenta, no definas `PwdExp`, o cambia la contraseña de la cuenta mediante la pantalla `change-password`.

### Inicio de sesión con una contraseña caducada {#login-with-an-expired-password}

La caducidad se comprueba después de verificar la contraseña, así que solo la ve el usuario que conoce la contraseña. Las cuentas
que se autentican mediante [SSO](authentication-sso.md) no se ven afectadas, porque allí no se usa la contraseña, y tampoco lo están
los usuarios de LDAP ni de cualquier otro modo de autenticación no local.

En el inicio de sesión web (motores AngularJS y React) un usuario con una contraseña caducada:

1. Recibe el mensaje *Su contraseña ha caducado. Fije una nueva contraseña para continuar.* (localizado en todos los idiomas del
   framework: `ERROR_TITLE_EXPIRED_PASSWORD` y `ERROR_MESSAGE_EXPIRED_PASSWORD`) y es llevado a la pantalla pública
   `change-expired-password`, que pide la nueva contraseña dos veces.
2. Fija la nueva contraseña. La contraseña tiene que cumplir la política de contraseñas de la aplicación
   ([`awe.security.password-pattern` y `awe.security.password-min-length`](../properties.md)), que se comprueba en
   la pantalla y de nuevo en el servidor, y tiene que ser distinta de la caducada. Se almacena la fecha de cambio, así que la
   nueva contraseña no está caducada.
3. Vuelve a la pantalla de acceso e inicia sesión con la nueva contraseña mediante el inicio de sesión normal (que incluye el
   segundo factor, si está activado).

El usuario nunca obtiene una sesión autenticada con la contraseña caducada. Después de que el servidor verifique la contraseña, solo
guarda, en la sesión HTTP de ese intento de inicio de sesión, una solicitud de cambio de la contraseña de ese usuario:

- está ligada al usuario cuya contraseña se verificó, así que el usuario a cambiar nunca se toma del navegador;
- caduca a los cinco minutos y puede usarse una sola vez: se descarta cuando se cambia la contraseña, y también cuando
  caduca o cuando el usuario se deshabilita o se bloquea entre tanto;
- solo permite el destino de mantenimiento `ChdPwdExpired`; el resto de la aplicación sigue respondiendo *no autorizado*;
- una contraseña incorrecta no la crea, y el paso de cambio no puede usarse para adivinar contraseñas: la contraseña la verifica
  el inicio de sesión normal, exactamente como antes, así que lo que limite los inicios de sesión fallidos de tu instalación (una pasarela, un
  proveedor de autenticación personalizado o un administrador que bloquee la cuenta) sigue aplicándose. AWE no tiene un contador
  propio de inicios de sesión fallidos.

:::warning Menús públicos personalizados
El menú público del framework define la opción `change-expired-password`. Una aplicación que sustituya todo el menú
`public` tiene que conservar la opción (`<option name="change-expired-password" screen="change-expired-password"
invisible="true" />`), y una aplicación que sobrescriba la pantalla `signin` puede conservar el flujo de usuario dejando la
acción de servidor `login` tal como está: el servidor añade la redirección.
:::

Para cambiar la contraseña cuando el usuario ha iniciado sesión, la pantalla `change-password` (destino de mantenimiento `ChdPwd`) almacena la
fecha actual como nueva fecha del último cambio. Desde AWE 5 el destino de mantenimiento `ChdPwd` no es público (antes podía
llamarse sin sesión) y cambia solo la contraseña del usuario con sesión iniciada.

:::note
La fecha se almacena sin hora, por lo que una contraseña es válida hasta `PwdExp` días después del inicio del día en que se
cambió.
:::

### Autenticación REST {#rest-authentication}

`POST /api/authenticate` responde `401 Unauthorized` para un usuario cuya contraseña ha caducado, con un mensaje localizado y
el código `PASSWORD_EXPIRED` para distinguirlo de las credenciales no válidas:

```json
{
  "type": "ERROR",
  "title": "Unauthorized",
  "message": "Your password has expired. Set a new password to continue.",
  "code": "PASSWORD_EXPIRED"
}
```

El mensaje usa el idioma por defecto de la aplicación. No hay un flujo REST para cambiar una contraseña caducada: el
cambio necesita la sesión HTTP del inicio de sesión web, así que los clientes REST (y los usuarios técnicos, como el que usa el
planificador para llamar a la API) tienen que cambiar la contraseña en la aplicación web, o un administrador tiene que fijar
`PwdExp` a un valor mayor para ellos. Ten en cuenta que un usuario técnico cuya contraseña fijó un administrador
cuenta como caducado mientras `PwdExp` esté definido.

## Autenticación de dos factores (2fa) {#two-factor-authentication-2fa}
Hemos desarrollado recientemente un nuevo sistema de autenticación de dos factores basado en _aplicaciones de autenticación_ como **Google Authenticator**.

Hay tres formas de gestionar esta autenticación de dos factores en AWE según la propiedad `awe.totp.security.enabled`:

- `disabled`: La autenticación de dos factores está desactivada y no se pedirá un código temporal al acceder.
- `optional`: El usuario **puede activar** la autenticación de dos factores en la **pantalla de ajustes** y se pedirá el código temporal al iniciar sesión.

  <img style={{ width: "70%", margin: "30px 15% 0% 15%" }}
  alt="Pantalla de ajustes"
  src={require('@docusaurus/useBaseUrl').default('img/security-settings.png')}
  />
  <div style={{textAlign:"center",fontStyle:"italic"}}>Pantalla de ajustes de seguridad</div>

  <img style={{ width: "60%", margin: "30px 20% 0% 20%" }}
  alt="Pantalla del código TOTP"
  src={require('@docusaurus/useBaseUrl').default('img/totp-code.png')}
  />
  <div style={{textAlign:"center",fontStyle:"italic",marginBottom:"30px"}}>Pantalla del código TOTP</div>


- `force`: Al iniciar sesión, **si el usuario no ha activado la autenticación de dos factores**, aparecerá una pantalla con el código QR para obligar al usuario a
  activarla. Tras esa pantalla, se pedirá al usuario el código temporal basado en el código secreto generado previamente.

  <img style={{ width: "40%", margin: "30px 30% 0% 30%" }}
  alt="Pantalla de autenticación de dos factores obligatoria"
  src={require('@docusaurus/useBaseUrl').default('img/force-2fa.png')}
  />
  <div style={{textAlign:"center",fontStyle:"italic"}}>Pantalla de seguridad de dos factores obligatoria</div>

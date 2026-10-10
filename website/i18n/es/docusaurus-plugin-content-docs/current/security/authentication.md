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
| Definido | Hace `PwdExp` días o más | La contraseña ha caducado y se rechaza el inicio de sesión. |
| Definido | Nunca (fecha vacía) | La contraseña ha caducado y se rechaza el inicio de sesión: el usuario tiene que cambiarla. |

Un usuario que nunca ha cambiado la contraseña no tiene fecha de cambio. Este es el caso de un usuario creado sin fecha de
cambio y de un usuario cuya contraseña fija un administrador en la pantalla de usuarios (la pantalla borra la fecha). Con `PwdExp` definido, ese usuario se rechaza por caducado, y sin `PwdExp` la
fecha ausente no tiene efecto.

La caducidad se comprueba después de verificar la contraseña. Un inicio de sesión caducado se rechaza con el
mensaje `User credentials have expired`. Las cuentas que se autentican mediante [SSO](authentication-sso.md)
no se ven afectadas, porque allí no se usa la contraseña.

Para cambiar la contraseña, la pantalla `change-password` (destino de mantenimiento `ChdPwd`) almacena la fecha actual como nueva
fecha del último cambio.

:::note
La pantalla `change-password` almacena la fecha sin hora, por lo que una contraseña es válida hasta `PwdExp` días
después del inicio del día en que se cambió.
:::

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

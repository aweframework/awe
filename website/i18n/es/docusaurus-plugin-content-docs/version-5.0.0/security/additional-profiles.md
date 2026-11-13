---
id: additional-profiles
title: Perfiles adicionales de un usuario
sidebar_label: Perfiles adicionales
---

# Perfiles adicionales de un usuario {#additional-profiles-of-a-user}

Un usuario tiene un **perfil principal** y puede tener **perfiles adicionales**. El perfil principal decide cómo se ve
la aplicación para el usuario; el acceso a las ventanas, los módulos y los sitios es la unión de todos los perfiles del
usuario.

| Quiere | Lea |
| --- | --- |
| Saber qué decide cada perfil | [Perfil principal y perfiles adicionales](#main-and-additional-profiles) |
| Saber qué ventanas puede abrir un usuario | [Acceso a las ventanas](#window-access) |
| Comprobar un perfil en Java o en una consulta | [Roles de Spring Security](#spring-security-roles), [Valores de sesión](#session-values) |
| Asignar perfiles adicionales | [Administración](#administration), [Inicio de sesión único](#single-sign-on) |
| Actualizar la base de datos o un `UserDAO` propio | [Base de datos](#database), [DAO de usuarios propio](#custom-user-dao) |

## Perfil principal y perfiles adicionales {#main-and-additional-profiles}

- El **perfil principal** es el perfil del usuario (`ope.IdePro`), como hasta ahora. Solo él decide los ajustes que
  admiten un único valor: la pantalla inicial, el tema y el valor `profile` de la sesión. La configuración de pantallas
  se sigue leyendo para el perfil principal.
- Los **perfiles adicionales** son los perfiles que el usuario tiene además del principal. Se guardan en la tabla
  `AweOpeRol`, una fila por usuario y perfil, y nunca repiten el perfil principal.
- Un perfil adicional **inactivo** (`AwePro.Act` distinto de `1`) no cuenta: no da acceso a ventanas, ni módulos o
  sitios, ni rol.

Un usuario sin perfiles adicionales se comporta exactamente igual que antes. Los cambios en los perfiles adicionales de
un usuario se aplican a la sesión, los roles y el acceso a las ventanas en el siguiente inicio de sesión; los
[módulos y sitios](#modules-and-sites) pueden reflejarlos antes.

## Acceso a las ventanas {#window-access}

El acceso a una opción de menú se calcula para cada perfil del usuario, como para un usuario que solo tuviera ese
perfil: el fichero de restricciones del perfil (`profile/*.xml`, indicado en la *restricción* del perfil, o la
restricción por defecto cuando no tiene), las restricciones globales y las filas de `AweScrRes` del perfil. Después se
unen los resultados:

- Una opción está abierta cuando **algún** perfil la abre. Entre perfiles, permitir gana a restringir: si un perfil
  restringe una opción y otro la permite, el usuario puede abrirla.
- El fichero de restricciones del **usuario** (la *restricción* del usuario), cuando lo tiene, tiene la última palabra
  sobre todos los perfiles: se aplica después de la unión.
- Las restricciones de `AweScrRes` del **usuario** se aplican al final. Un administrador puede seguir cerrando una
  opción a un usuario concreto, permitan lo que permitan sus perfiles y su fichero de restricciones.
- Un fichero de restricciones que no existe no abre nada, como para un usuario con un solo perfil.

Por ejemplo, un usuario con el perfil principal `operator`, que restringe la opción *Informes*, y el perfil adicional
`auditor`, que la permite, ve *Informes* en el menú. Si una fila de `AweScrRes` restringe *Informes* a ese usuario, la
opción queda cerrada.

La misma unión se aplica al [acceso por pantalla](screen-access.md) de los targets de maintain y las consultas que
lanza un cliente (`awe.security.authorization.mode`): un usuario puede lanzarlos cuando alguno de sus perfiles abre una
pantalla que los usa.

### Módulos y sitios {#modules-and-sites}

Los módulos y los sitios disponibles para el usuario incluyen los de sus perfiles adicionales. Leen los perfiles
adicionales del usuario en cada consulta, así que un cambio puede verse ahí antes del siguiente inicio de sesión.

## Roles de Spring Security {#spring-security-roles}

En la sesión web, el usuario recibe una autoridad `ROLE_<nombre del perfil>` por cada perfil, el principal y los
adicionales. `hasRole` y las demás comprobaciones de roles de Spring Security funcionan con cualquier perfil del
usuario:

```java
@PreAuthorize("hasRole('auditor')")
public void exportAudit() {
  // Open to every user that has the auditor profile, as main or as additional profile
}
```

En la [API REST](../rest-module.md):

- El modo `local-jwt` da al usuario del token las mismas autoridades que la sesión web: un `ROLE_<nombre del perfil>`
  por cada perfil, el principal y los adicionales.
- En el modo `oauth2-resource-server`, los perfiles vienen del claim de perfil del token
  (`awe.rest.api.oauth2-resource-server.principal.profile-claim`), asignados como el inicio de sesión único asigna sus
  roles: se quita el prefijo de autoridad y solo se mantienen los roles que son nombres de perfil (sin distinguir
  mayúsculas y minúsculas). El primero que aparece en `awe.security.sso.profile-priority`, o si no el primero en orden
  alfabético, es el perfil principal y el resto son perfiles adicionales, cada uno con una autoridad
  `ROLE_<nombre del perfil>`. Cuando ningún rol corresponde a un perfil se aplica el perfil por defecto.

## Valores de sesión {#session-values}

La sesión conserva los valores del perfil principal (`profile`, `restriction`) y añade estos valores:

| Valor de sesión | Contenido |
| --- | --- |
| `profiles` | Acrónimos de todos los perfiles del usuario, primero el principal, separados por comas. |
| `restrictions` | Ficheros de restricciones de esos perfiles, separados por comas. |
| `userRestriction` | Fichero de restricciones del propio usuario. Solo existe cuando el usuario tiene uno. |

Léalos en una consulta con una variable `STRING_TO_LIST` para filtrar por todos los perfiles del usuario:

```xml
<query id="UserProfileNames">
  <table id="AwePro"/>
  <field id="Nam" alias="label"/>
  <where>
    <filter left-field="Acr" condition="in" right-variable="profiles"/>
  </where>
  <variable id="profiles" type="STRING_TO_LIST" session="profiles"/>
</query>
```

Una variable `STRING_TO_LIST` es siempre una lista, también cuando su valor viene de la sesión o tiene un solo valor,
así que `condition="in"` funciona con valores de sesión. Un valor vacío es una lista vacía. Consulte los
[tipos de variable](../api/query-definition.md#variable-types) de las consultas.

## Administración {#administration}

El panel lateral de la [pantalla de administración de usuarios](../guides/default-screens.md#users) (`user-admin`)
tiene un criterio *Perfiles adicionales*, una selección múltiple de perfiles:

- Al guardar el usuario, sus perfiles adicionales se sustituyen por los seleccionados.
- El perfil principal nunca se guarda como perfil adicional: si la selección lo contiene, o un usuario recibe como
  perfil principal uno de sus perfiles adicionales, esa fila se elimina.
- Borrar un usuario borra sus perfiles adicionales, y borrar un perfil lo quita de los usuarios que lo tenían como
  perfil adicional.

La tabla de la pantalla muestra los perfiles adicionales de cada usuario en la columna *Perfiles adicionales*, como
etiquetas: las dos primeras y una etiqueta *+N* cuyo tooltip lista las demás. *Ver permisos* muestra los
[permisos efectivos](permissions.md#effective-permissions) del usuario, los de todos sus perfiles.

## Inicio de sesión único {#single-sign-on}

Con el [inicio de sesión único](authentication-sso.md) (OAuth2 / OpenID Connect), el perfil principal se elige según
la [regla de varios roles](authentication-sso.md#several-roles-which-profile-wins): el primer perfil de
`awe.security.sso.profile-priority` que envía el proveedor y que existe, o si no el primero por orden alfabético que
existe. Después:

- Cuando el perfil principal viene de un rol del proveedor que existe como perfil, los demás roles del proveedor que
  existen como perfiles pasan a ser los perfiles adicionales del usuario. Sustituyen a los perfiles adicionales
  guardados para ese usuario (el cambio se audita en `HISAweOpeRol`), tanto para usuarios existentes como para
  usuarios aprovisionados en ese inicio de sesión: el proveedor es la fuente de verdad de los perfiles adicionales de
  un usuario SSO.
- Los roles sin un perfil existente se ignoran.
- Cuando ningún rol se mapea a un perfil existente (ningún rol, un rol desconocido, o el rol por defecto aplicado con
  `overwrite-profile-with-default-role`), los perfiles adicionales no se modifican y el inicio de sesión sigue el
  [flujo anterior](authentication-sso.md#synchronization-with-the-stored-profile).

En el primer inicio de sesión después de que el proveedor cambie el rol principal de un usuario, la sesión conserva el
perfil principal anterior, como hasta ahora: el perfil principal guardado se actualiza y se aplica desde el siguiente
inicio de sesión.

## Base de datos {#database}

La migración Flyway `AWE_V1.6.0__authorization.sql` crea la tabla `AweOpeRol` (única por usuario y perfil), su
tabla de auditoría `HISAweOpeRol` y la clave `OpeRolKey`, para H2, HSQLDB, MySQL, Oracle, PostgreSQL y SQL Server. Los
scripts se pueden aplicar de nuevo. Las aplicaciones que no usan las migraciones Flyway de AWE crean estas tablas por su
cuenta (ver el script de su base de datos en `db/migration`).

## DAO de usuarios propio {#custom-user-dao}

`UserDAO` tiene tres nuevos métodos por defecto:

| Método | Por defecto | Se usa para |
| --- | --- | --- |
| `supportsAdditionalProfiles()` | Devuelve `false`. | Permitir que el inicio de sesión SSO escriba los perfiles adicionales del usuario. |
| `findAdditionalProfiles(String userName)` | No devuelve ningún perfil. | Cargar los perfiles adicionales del usuario al iniciar sesión. |
| `findProfileId(String profileName)` | Devuelve un `Optional` vacío. | Buscar los perfiles de los roles del proveedor en el inicio de sesión SSO. |

Una aplicación con su propia implementación de `UserDAO` sigue funcionando solo con el perfil principal. Sobrescriba
los tres métodos para admitir perfiles adicionales. Mientras `supportsAdditionalProfiles()` devuelva `false`, el inicio
de sesión SSO no toca las filas de `AweOpeRol` del usuario: ni las escribe ni las borra.

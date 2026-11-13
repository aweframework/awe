---
id: screen-access
title: Acceso por pantalla a targets y consultas
sidebar_label: Acceso por pantalla
---

# Control de acceso de targets de maintain y consultas por acceso a pantalla {#access-control-of-maintain-targets-and-queries-by-screen-access}

La aplicación web lanza targets de maintain y consultas por su nombre. AWE puede comprobar, antes de ejecutarlos, que
el usuario con sesión puede abrir al menos una pantalla que use el target o la consulta. La [API REST](#rest-api) no se
comprueba por acceso a pantalla: se autoriza solo por los [permisos](permissions.md).

| Quiere | Lea |
| --- | --- |
| Saber qué se comprueba | [Cómo funciona](#how-it-works) |
| Saber cómo se autoriza la API REST | [API REST](#rest-api) |
| Configurar el modo o desplegarlo en una aplicación existente | [Configuración](#configuration), [Despliegue gradual](#rollout) |
| Saber qué no cubre | [Límites](#limits) |
| Encontrar los rechazos | [Auditoría](#audit) |

## Cómo funciona {#how-it-works}

Al arrancar, y cada vez que se recargan los descriptores, AWE construye un índice de los targets y consultas que usa
cada opción de menú:

1. Recorre todas las opciones del menú privado y del menú público que tienen un `screen`.
2. Resuelve los elementos `<include>` de la pantalla y sigue los frames que cargan otra pantalla.
3. Recoge el `target-action` de los componentes y botones, los targets y consultas de los atributos `initial-load` y
   los valores de `check-target`.

Cuando un cliente lanza un target o una consulta que está en el índice, solo se ejecuta si el usuario de la sesión puede
abrir **al menos una** de las opciones de menú cuya pantalla lo usa. La regla es la misma que para abrir la pantalla:
se aplican las restricciones de los perfiles del usuario y de la pantalla (para un usuario con
[perfiles adicionales](additional-profiles.md#window-access), la unión de todos sus perfiles). Las opciones del menú
público están abiertas a todos.

La comprobación se aplica a los puntos de entrada de la aplicación web:

- Las peticiones `/action/...` que lanzan un maintain o una consulta: `maintain` (también las variantes silenciosa y
  asíncrona), `data`, `subscribe`, `value`, `validate`...
- Los endpoints de ficheros `/file/stream/maintain` y `/file/download/maintain`.

Los targets y las consultas marcados como `public` nunca se comprueban por acceso a pantalla: ya se pueden lanzar sin
sesión.

Un target o una consulta que no usa ninguna pantalla indexada conserva su comportamiento anterior, que depende de sus
atributos: `public` (no necesita sesión), una sesión iniciada, o
[`server-only`](../api/maintain-definition.md#server-only-targets) (nunca desde un cliente).

Los lanzamientos que hace el servidor no se ven afectados: los servicios Java que llaman a `launchPrivateMaintain` o
`launchPrivateQuery`, y los targets que un maintain incluye con `<include target="..."/>`.

:::info
`server-only` y el acceso por pantalla se complementan. `server-only` rechaza un target ante **cualquier** petición de
un cliente. El acceso por pantalla solo deja pasar a los usuarios que pueden abrir una pantalla que usa el target.
:::

Los [permisos](permissions.md) son una segunda comprobación después del acceso por pantalla: un target o una consulta
que declara un `permission` exige además que el usuario lo tenga.

## API REST {#rest-api}

La API REST lanza consultas y targets de maintain sin pantallas, así que las peticiones bajo `/api/` (`/api/maintain`,
`/api/data` y `/api/public/...`) no se comprueban por acceso a pantalla. El resto de comprobaciones se mantiene:

- Los [permisos](permissions.md): un target o una consulta que declara un `permission` solo se ejecuta cuando el usuario
  del token lo tiene.
- Los targets `public` y [`server-only`](../api/maintain-definition.md#server-only-targets) conservan sus reglas.
- Una petición rechazada responde HTTP `403` con el mensaje genérico de rechazo, que nunca nombra el target, y registra
  un evento `ACCESS_DENIED` de la [auditoría de seguridad](security-audit.md) en el canal `REST`.

:::caution
Cualquier usuario autenticado puede lanzar por la API REST un target o una consulta privados sin `permission`. Cuando
la API REST esté expuesta, active
[`awe.security.permissions.require-undeclared=true`](permissions.md#require-undeclared) para que esos targets requieran
`default.read` o `default.write`.
:::

## Configuración {#configuration}

El modo es común a todas las autorizaciones de los lanzamientos que pide un cliente: el acceso por
pantalla y los [permisos](permissions.md#mode).

| Propiedad | Por defecto | Descripción |
| --- | --- | --- |
| `awe.security.authorization.mode` | `enforce` | `off`, `warn` o `enforce` |

| Valor | Comportamiento |
| --- | --- |
| `enforce` | Una petición que el usuario no puede hacer se rechaza con el mismo error de *no autorizado* que un target `server-only`, y el rechazo se audita. |
| `warn` | El rechazo solo se escribe en el log y se registra en la [auditoría de seguridad](security-audit.md) (`ACCESS_DENIED`); la petición se ejecuta. |
| `off` | Sin comprobación. |

```properties
# off | warn | enforce (por defecto)
awe.security.authorization.mode=warn
```

## Despliegue gradual {#rollout}

Una aplicación existente, como un producto que migra desde AWE 4, puede adoptar la comprobación poco a poco:

1. Configure `awe.security.authorization.mode=warn` y ejecute la aplicación con un uso normal.
2. Lea el log y los eventos `ACCESS_DENIED` de la [auditoría de seguridad](security-audit.md). Cada uno lleva el usuario
   y, en el detalle, el tipo y el nombre del target o de la consulta que se rechazaría (causa `RESTRICTED`).
3. Revise los rechazos. Corrija las pantallas o las restricciones que no coincidan con el acceso previsto.
4. Quite la propiedad (o configure `enforce`).

`off` mantiene el comportamiento de AWE 4 mientras dura la migración.

## Límites {#limits}

- Las opciones con `dynamic-screen-service` y las opciones `menu-screen` no se indexan, porque sus pantallas se generan
  en tiempo de ejecución. Los targets y consultas que solo usan esas pantallas no se comprueban.
- Un target o una consulta que usa tanto una de esas pantallas como una pantalla estática restringida se rechaza a los
  usuarios que no pueden abrir la estática. Ejecute primero con `warn` para encontrar estos casos.
- La API REST no se comprueba por acceso a pantalla (consulte [API REST](#rest-api)).
- La comprobación sigue las reglas de acceso a pantallas. No sustituye las restricciones de sus datos, que deben seguir
  aplicándose en las definiciones de targets y consultas (por ejemplo, con variables `session`).

## Auditoría {#audit}

Un rechazo registra un evento `ACCESS_DENIED` de la [auditoría de seguridad](security-audit.md) con la causa
`RESTRICTED` y el tipo y el nombre del target o de la consulta como detalle (`query: <id>` o `maintain: <id>`), y un
aviso en el log. En modo `warn` se escribe el mismo registro con el detalle terminado en `(allowed: warn)`, y la
petición continúa.

---
id: openrewrite
title: Recetas de OpenRewrite
sidebar_label: OpenRewrite
---

[OpenRewrite](https://docs.openrewrite.org/) reescribe fuentes Java mediante recetas: puede llevar el código de AWE a
los modismos de Java 21 o actualizar Spring Boot y, más adelante, migrar aplicaciones de AWE 4 a AWE 5. Las recetas
viven en `rewrite.yml` en la raíz del repositorio y se ejecutan únicamente mediante el perfil de Maven `rewrite`, que
nunca está activo por defecto: la compilación normal no lo toca.

## Ver qué cambiaría una receta {#see-what-a-recipe-would-change}

```bash
mvn -Prewrite rewrite:dryRun
```

No se modifica nada. El objetivo imprime los archivos que cambiaría cada receta y escribe los cambios como un parche en
`target/rewrite/rewrite.patch` (el agregado del reactor, en la raíz). Revíselo y aplique lo que desee:

```bash
git apply target/rewrite/rewrite.patch
```

Añada `-pl <module>` para probar un solo módulo, por ejemplo `-pl awe-framework/awe-model`. La primera ejecución
descarga los jars de las recetas. Los módulos del reactor deben poder resolverse (instalados en el repositorio local, o
construidos en el mismo comando).

El job `Rewrite dry-run` de la pipeline hace lo mismo bajo demanda: es manual y nunca bloquea una merge request.
Inícielo y descargue el parche de los artefactos del job.

## Receta activa y otras {#active-recipe-and-others}

| Receta | Estado | Qué hace |
|---|---|---|
| `com.almis.awe.rewrite.Java21Migration` | Activa en el perfil | Ejecuta `org.openrewrite.java.migrate.UpgradeToJava21` |
| `com.almis.awe.rewrite.SpringBoot4Migration` | Definida, no activa | Ejecuta `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` |

Ejecute otra receta sin editar el pom:

```bash
mvn -Prewrite rewrite:dryRun -Drewrite.activeRecipes=com.almis.awe.rewrite.SpringBoot4Migration
```

Liste todas las recetas que conoce el plugin con `mvn -Prewrite rewrite:discover`. Para aplicar una receta en lugar de
previsualizarla, use `rewrite:run` en una rama limpia y revise el diff como cualquier otro cambio.

:::note
Todavía no se ha aplicado ninguna receta al código. Las versiones del plugin y de las bibliotecas de recetas son las
propiedades `plugin-rewrite.version`, `rewrite-migrate-java.version` y `rewrite-spring.version` del `pom.xml` raíz.
:::

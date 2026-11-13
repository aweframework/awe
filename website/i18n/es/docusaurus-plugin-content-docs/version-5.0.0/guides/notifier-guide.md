---
id: notifier
title: Notificaciones
sidebar_label: Notificaciones
---

El módulo **notifier** mejora AWE con un sistema de notificaciones. Este sistema funciona de la siguiente manera:

1. Alguien define una lista de suscripciones (temas) en la aplicación.
2. Los usuarios pueden _suscribirse_ a un tema para recibir las notificaciones (por web o por correo electrónico).
3. El sistema lanza una notificación _a través de_ la **[API de notificaciones](#notification-api)**, y se envía a todos los usuarios suscritos.
4. Los usuarios reciben las notificaciones y pueden marcarlas como leídas o hacer clic sobre ellas (en las notificaciones web).
5. Las notificaciones pueden llevar al usuario a la pantalla relacionada con la notificación.

## Requisitos previos {#prerequisites}

El **módulo notifier** necesita ser configurado antes de usarse.

Para ver cómo configurar el **módulo notifier** en tu aplicación consulta la **[guía de configuración](../notifier-module.md)**.

## Suscripciones {#subscriptions}

La pantalla de suscripciones permite definir nuevos temas de suscripción para la aplicación.

Al definir un tema de suscripción, es necesario definir un acrónimo que se utilizará en la
**[API de notificaciones](#notification-api)** para referirse a la suscripción.

## Notificaciones {#notifications}

La pantalla de notificaciones almacena todas las notificaciones enviadas.

Aquí el usuario puede marcar como leída/no leída cualquier notificación.

## Ajustes de usuario {#user-settings}

La nueva pantalla de ajustes de usuario permite al usuario gestionar sus suscripciones, activando o desactivando
las que necesite recibir.

## Panel de notificaciones {#notification-panel}

El panel de notificaciones es un menú desplegable que se refrescará cada vez que haya una nueva notificación
o cambie el estado de una notificación.

## API de notificaciones {#notification-api}

El módulo notifier añade un bean llamado `NotifierService` que contiene el siguiente método para
añadir notificaciones:

```xml
public void notify(NotificationDto notification) throws AWException
```

Para utilizarlo, simplemente rellena un objeto Dto llamado `NotificationDto` (`new NotificationDto()`)
y llama al método `notify` del bean `NotifierService`:

  * `subscription`: **acrónimo** de la suscripción
  * `title`: título de la notificación (30 caracteres como máximo)
  * `description`: descripción de la notificación (250 caracteres como máximo)
  * `icon`: icono de la notificación (elígelos desde la pantalla de [iconos](/api/icons.md))
  * `type`: tipo de notificación:
    * `NORMAL`: notificación estándar (gris)
    * `OK`: notificación de éxito (verde)
    * `INFO`: notificación informativa (azul)
    * `WARNING`: notificación de advertencia (amarillo)
    * `ERROR`: notificación de error (rojo)
  * `screen`: pantalla de destino (nombre de la opción de la pantalla a la que se redirige al hacer clic en la notificación)
  * `code`: código que se enviará como la variable `selected-notification` al hacer clic en una notificación
  (útil para referirse a un punto concreto de la pantalla de destino).

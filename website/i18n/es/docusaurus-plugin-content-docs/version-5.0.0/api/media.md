---
id: media
title: Imagen, vídeo y enlace
---

:::tip Pruébalo en vivo

Prueba el avatar y el vídeo con controles en vivo, y mira una imagen y un enlace, en el [catálogo de componentes](../components/media-content.mdx): los componentes reales de AWE y el XML que los produce.

:::

Tres elementos muestran contenido en una pantalla: una `image`, un `video` y un `link`. Todos aceptan los atributos comunes de los elementos de pantalla
(`id`, `style`, `label`, `expandible`...).

## Esqueleto XML {#xml-skeleton}

```xml
<image id="[id]" url="[image-url]" alternate-url="[alternative-text]" style="[style]" />

<video id="[id]" src="[video-url]" poster="[poster-url]" controls="[true|false]" loop="[true|false]"
       autoplay="[true|false]" preload="[none|metadata|full-video]" style="[style]" />

<link id="[id]" label="[label]" url="[link-url]" style="[style]" />
```

## Atributos de la imagen {#image-attributes}

| Atributo        | Uso      | Tipo   | Descripción                                                                                            |
|-----------------|----------|--------|--------------------------------------------------------------------------------------------------------|
| `url`           | Opcional | String | Dirección de la imagen                                                                                 |
| `alternate-url` | Opcional | String | Texto que el navegador muestra cuando no puede cargar la imagen. El motor React lo escribe en el `alt` de la imagen |
| `style`         | Opcional | String | Clases CSS de la imagen                                                                                |

## Atributos del vídeo {#video-attributes}

El motor React reproduce el archivo con `react-player`.

| Atributo   | Uso      | Tipo    | Descripción                                                                                              | Valores                               |
|------------|----------|---------|----------------------------------------------------------------------------------------------------------|---------------------------------------|
| `src`      | Opcional | String  | Dirección del archivo de vídeo                                                                           |                                       |
| `poster`   | Opcional | String  | Imagen que se muestra antes de reproducir el vídeo. Con un póster el vídeo no se carga hasta que el usuario hace clic |                                       |
| `controls` | Opcional | Boolean | Muestra los controles del reproductor                                                                    | `true` (por defecto), `false`         |
| `loop`     | Opcional | Boolean | Vuelve a empezar cuando el vídeo termina                                                                 | `true`, `false` (por defecto)         |
| `autoplay` | Opcional | Boolean | Empieza a reproducirse cuando se abre la pantalla                                                        | `true`, `false` (por defecto)         |
| `preload`  | Opcional | String  | Cuánto del archivo carga el navegador antes de que el usuario lo reproduzca                              | `none`, `metadata`, `full-video`      |
| `style`    | Opcional | String  | Clases CSS del reproductor                                                                               |                                       |

## Atributos del enlace {#link-attributes}

Un enlace abre su `url` en una pestaña nueva. El texto es su `label`.

| Atributo | Uso      | Tipo   | Descripción                                                                 |
|----------|----------|--------|-----------------------------------------------------------------------------|
| `url`    | Opcional | String | Dirección que abre el enlace                                                |
| `label`  | Opcional | String | Texto del enlace. **Nota:** Puedes usar archivos de [i18n](i18n-internationalization.md) (locales) |
| `style`  | Opcional | String | Clases CSS del enlace                                                       |

El esquema también declara `icon` para el enlace, pero el motor React no lo dibuja.

## Ejemplos {#examples}

```xml
<image id="Logo" url="img/logo.png" alternate-url="Logotipo de la empresa" />
<video id="Intro" src="media/intro.mp4" poster="media/intro.png" controls="true" />
<link id="Help" label="Lee el manual" url="https://docs.aweframework.com" />
```

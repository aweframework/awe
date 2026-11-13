---
id: media
title: Image, video and link
---

:::tip See it live

Try the avatar and the video with live controls, and see an image and a link, in the [component catalog](../components/media-content.mdx): the real AWE components and the XML that produces them.

:::

Three elements show content in a screen: an `image`, a `video` and a `link`. All of them accept the common attributes of the screen
elements (`id`, `style`, `label`, `expandible`...).

## XML skeleton

```xml
<image id="[id]" url="[image-url]" alternate-url="[alternative-text]" style="[style]" />

<video id="[id]" src="[video-url]" poster="[poster-url]" controls="[true|false]" loop="[true|false]"
       autoplay="[true|false]" preload="[none|metadata|full-video]" style="[style]" />

<link id="[id]" label="[label]" url="[link-url]" style="[style]" />
```

## Image attributes

| Attribute       | Use      | Type   | Description                                                                                            |
|-----------------|----------|--------|--------------------------------------------------------------------------------------------------------|
| `url`           | Optional | String | Address of the image                                                                                   |
| `alternate-url` | Optional | String | Text the browser shows when the image cannot be loaded. The React engine writes it in the `alt` of the image |
| `style`         | Optional | String | CSS classes of the image                                                                               |

## Video attributes

The React engine plays the file with `react-player`.

| Attribute  | Use      | Type    | Description                                                                                              | Values                                |
|------------|----------|---------|----------------------------------------------------------------------------------------------------------|---------------------------------------|
| `src`      | Optional | String  | Address of the video file                                                                                |                                       |
| `poster`   | Optional | String  | Image shown before the video plays. With a poster the video is not loaded until the user clicks it       |                                       |
| `controls` | Optional | Boolean | Show the controls of the player                                                                          | `true` (default), `false`             |
| `loop`     | Optional | Boolean | Start again when the video ends                                                                          | `true`, `false` (default)             |
| `autoplay` | Optional | Boolean | Start playing when the screen opens                                                                      | `true`, `false` (default)             |
| `preload`  | Optional | String  | How much of the file the browser loads before the user plays it                                          | `none`, `metadata`, `full-video`      |
| `style`    | Optional | String  | CSS classes of the player                                                                                |                                       |

## Link attributes

A link opens its `url` in a new tab. The text is its `label`.

| Attribute | Use      | Type   | Description                                                                 |
|-----------|----------|--------|-----------------------------------------------------------------------------|
| `url`     | Optional | String | Address the link opens                                                      |
| `label`   | Optional | String | Text of the link. **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| `style`   | Optional | String | CSS classes of the link                                                     |

The schema also declares `icon` for the link, but the React engine does not draw it.

## Examples

```xml
<image id="Logo" url="img/logo.png" alternate-url="Company logo" />
<video id="Intro" src="media/intro.mp4" poster="media/intro.png" controls="true" />
<link id="Help" label="Read the manual" url="https://docs.aweframework.com" />
```

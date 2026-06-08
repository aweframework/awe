---
id: icons
Title: Icons
sidebar_label: Icons
---

AWE has included some icons families to be used on components and grids.

Simply look for an icon that suits the concept you want to show and put the name of 
the icon in the `icon` attribute (or value in case of columns).

To allow more icon families, the new icon name structure is the following:

```
family:icon_name
```

Where family is the name of the family icon described below

## Font Awesome 4.7.0 icons (fa)

The default icon family (coming from AWE 3 version) is Font Awesome:

[Font Awesome 4.7.0 icons](https://fontawesome.com/v4/icons/)

Here you have a set of 675 icons with brands, web application, arrows and a lot of
concepts very useful to put on options or buttons

To use these icons, simply put in the name of the icon in the `icon` attribute without
any family description:

```
icon="check"
```

or if you want you can set the family name:

```
icon="fa:check"
```

## Material Design Icons (mdi)

Google Material Design icons is a very famous and worldwide used set of icons based 
on Google Material Design theme.

[Material Design icons](https://fonts.google.com/icons?icon.style=Filled&icon.set=Material+Icons)

To use this family, put the family name `mdi` before the icon name:

```
icon="mdi:home"
```


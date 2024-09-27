---
id: developer
title: DevTools Module
sidebar_label: Developer Module
---

This module has some very useful management tools to manage literals: 
- [Literals manager](#literals-manager)

To use this module, follow these steps:

- Add **awe developer dependency** to pom.xml descriptor.

```xml
<dependencies>
...
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-developer-spring-boot-starter</artifactId>
  </dependency>
...
</dependencies>
```

- Configure property value to add `awe-developer` to module list.

```properties
awe.application.module-list = APP, ..., awe-developer, ..., awe
```

## `Literals manager`
The *Literal Manager* is a tool that helps managing existing literals in an application. All the available actions and modifications are stored both in memory and in the multiple XML-s. This way, it is not necessary to restart the *Tomcat* server whenever a change is made. Developer will just have to refresh the page in order to display all the changes.

<img alt="lit-man" src={require('@docusaurus/useBaseUrl').default('img/lit-man.png')} />

### Features

1. Search for existing literals.
2. Create new literals.
3. Modify existing literals.
4. Translate literals automatically to other languages.
5. Delete literals.

### Searching existing literals

<img alt="search-lite" src={require('@docusaurus/useBaseUrl').default('img/search-lite.png')} />

You can search by code or literal. The default search language is the same as the application default language, but it can be configured to search by any other *installed* language. By clicking on the search button next to the textfield, all the results appear in the left grid. In order to manage these literals, it is enough to click on any of them to display the details and translations of the chosen word.

### Deleting literals

<img alt="lit-list" src={require('@docusaurus/useBaseUrl').default('img/lit-list.png')} />

This process is completed by selecting the literal that has to be deleted and clicking the delete button.

### Creating literals

<img alt="new-lit" src={require('@docusaurus/useBaseUrl').default('img/new-lit.png')} />

New literals can be created by clicking on the *New* button. A modal view will appear with two inputs, the first one for the literal *code* and the second one to the literal contend in the default or chosen language. The tool generates the translations for all the other existing languages automatically.

### Modifying literals

<img alt="translation-list" src={require('@docusaurus/useBaseUrl').default('img/translation-list.png')} />

In order to modify existing literals, a literal must be chosen from the list displayed on the left grid. The content of the literal appears in the Text / Markdown editor. Markdown is a markup language with plain text formatting syntax designed so that it can be converted to HTML and many other formats. This makes possible to add styles in a simple way.

### Translating literals

#### Text

<img alt="text-trans" src={require('@docusaurus/useBaseUrl').default('img/text-trans.png')} />

#### Markdown

<img alt="markdown-trans" src={require('@docusaurus/useBaseUrl').default('img/markdown-trans.png')} />

Select the origin language in the markdown editor, then select the destination language and click on the translate button. Voilà!

### Launch

To use it, put a link to these windows somewhere in your project menu `public.xml` or `private.xml`.

Example:

``` XML
  <option name="developer" label="MENU_DEVELOPER" icon="paint-brush">
    <option name="path-manager"  label="MENU_PATH" screen="path-manager" icon="italic"/>
    <option name="local-manager" label="MENU_LANGUAGES" screen="local-manager" icon="language"/>
  </option>
```

- Add the directory of your XML files using path window inside developer menu.

<img alt="path" src={require('@docusaurus/useBaseUrl').default('img/path.png')} />

- The specified directory must have these folders

<img alt="folders" src={require('@docusaurus/useBaseUrl').default('img/folders.png')} />

- Example of AWE directory in a local machine

<img alt="path" src={require('@docusaurus/useBaseUrl').default('img/path.png')} />

* This tool is only intended to be used in local machines.
---
id: screen-personalization
title: Screen personalization
sidebar_label: Screen personalization
---

Screen personalization lets a functional administrator change how a screen looks and behaves without writing code:
rename a button, hide a field, make a criterion optional, change a width. A change can apply to **everyone**, to a
**profile** or to a single **user**. The screen XML is never touched: the changes are stored in the database and applied
when the screen is opened.

The editor works the same way in the React and in the AngularJS applications.

## Open the editor

1. Open **Settings → Personalize screen**.
2. Type in **Screen** and choose one. Only the screens you can open are listed.

The screen appears in the editor with its elements ready to be clicked. The editor has three modes: **Edit**,
**Overview** and **History**.

## Edit a screen

1. Click an element (a button, a criterion, a grid column). Nothing in the screen is triggered by the click.
2. A side panel opens with the options of that element. On narrow windows the panel floats over the screen; from
   1440 px of width it pushes the screen aside.
3. Change a value. Each option accepts only valid values (a switch, a list of choices, a number) and an invalid one
   shows an error and blocks **Save changes**.
4. The screen shows the change at once. Click another element to keep editing.
5. Click **Save changes**. **Discard** drops everything that is not saved yet.

Each option says where its current value comes from (the screen default, everyone, the profile or the user). **Reset**
removes the change of the scope you are editing and goes back to the inherited value; **Reset element** does it for
every option of the element.

## Who the change applies to

**Applies to** chooses everyone, a profile or a user. When several changes affect the same option, the most specific
one wins: **user beats profile, profile beats everyone**.

**View as** shows the screen as another subject sees it, without logging in as them: everyone, a profile or a user.
By default it follows the scope you are editing.

## Overview

Overview lists, for the chosen screen, each personalized element with what each subject sees (everyone, then profiles,
then users). Click a subject to open that element in the editor at that scope.

A block **N change(s) no longer work** lists the changes to clean up:

- The element or the option no longer exists on the screen, or the profile or user was removed: **Delete**.
- There are two changes for the same element, option and scope: **Keep one** (choose the value to keep).

## History

History lists every change made to the screen, newest first: when, who, the element, the option, the scope, and the
value before and after (or **Created** and **Deleted**).

**Undo** appears only on the latest change of each rule; older rows say **Changed later**. Undo is a normal save, so it
is validated and recorded in the history too. A change made before the history existed shows **Earlier value not
recorded** and cannot be undone.

## Export and import

**Export** downloads a JSON file with every change of the screen. Profiles are written by their acronym and users by
their login, so the file works in another environment where the internal ids differ.

**Import** takes a file exported from the same screen and shows a preview first (**N to add, M to change**). Nothing is
saved until you click **Apply import**.

- Import merges: it adds and changes rules, it never deletes any.
- Rules that cannot be applied are skipped and listed with the reason: the profile or the user does not exist here, or
  the element is not on the screen.

## Permissions

| To do this | You need |
|------------|----------|
| Use the editor, the overview, the history and export | `access.read` |
| Save, undo and import | `access.update` |

Both are granted in the access administration, like any other permission. On top of them, you can personalize only
the screens you can open yourself: a screen outside your menu is not listed and a save on it is refused with a clear
message.

## The Screen configuration grid

The older **Screen configuration** grid (see [Default screens](default-screens.md#screen-configuration)) still exists.
It writes the same rules, so its rows appear in the editor, the overview and the history. It is validated the same way:
an unknown element, an option that does not exist for it or an invalid value is rejected before anything is saved.

## Add the option to your menu

An application defines its own menu, so it has to include the option, for example in `private.xml`:

```xml
<option name="screen-personalization" label="MENU_TOOLS_SCR_PER" screen="ScrPer" icon="pencil"/>
```

package com.almis.awe.model.type;

import lombok.Getter;

@Getter
public enum MenuType {

    /**
     *   Horizontal menu
     */
    HORIZONTAL("home_horizontal"),
    /**
     *   Vertical menu
     */
    VERTICAL("home_vertical");

    private final String screen;

    /**
     * MenuType constructor
     * @param screen screen name of menu
     */
    MenuType(String screen) {
        this.screen = screen;
    }
}

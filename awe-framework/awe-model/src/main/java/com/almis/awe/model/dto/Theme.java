package com.almis.awe.model.dto;

import com.almis.awe.model.util.data.StringUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class Theme {
    private String name;
    private String themeColor;
    private String textColor;
    private String primaryColor;
    private String secondaryColor;
    private String primaryTextColor;
    private String secondaryTextColor;
    private String primaryBackgroundColor;
    private String secondaryBackgroundColor;
    private String primaryMenuColor;
    private String secondaryMenuColor;
    private String menuTextColor;
    private String primaryNavbarColor;
    private String secondaryNavbarColor;
    private String navbarDropdownColor;
    private String navbarTextColor;
    private String primaryFontSize;
    private String secondaryFontSize;
    private String disabledColor;
    private String disabledBorderColor;
    private String disabledTextColor;
    private String headerHeight;
    private String panelHeaderColor;
    private String panelBorderColor;
    private String panelBackgroundColor;
    private String panelTextColor;
    private String gridHeaderColor;
    private String gridBorderColor;
    private String dangerColor;
    private String warningColor;
    private String successColor;
    private String infoColor;
    private String borderColor;
    private String borderRadius;
    private String loadingBarColor;
    private String requiredColor;
    private boolean dark;

    /**
     * Generate the CSS of the theme. The variables of a dark theme apply to the dark mode chosen by the user (.dark) and,
     * while the user did not choose a mode (neither dark nor light), when the operating system prefers a dark color scheme
     *
     * @return CSS rules
     */
    @Override
    public String toString() {
        return toCss(true);
    }

    /**
     * Generate the CSS of the theme
     *
     * @param followOsColorScheme A dark theme also applies when the operating system prefers dark and the user did not choose
     * @return CSS rules
     */
    public String toCss(boolean followOsColorScheme) {
        ConfigurablePropertyAccessor parameterBeanAccessor = PropertyAccessorFactory.forBeanPropertyAccess(this);
        String variables = Arrays.stream(getClass().getDeclaredFields()).sequential()
                .filter(f -> !Arrays.asList("name", "dark").contains(f.getName()))
                .map(f -> getCssVariable(f.getName(), (String) parameterBeanAccessor.getPropertyValue(f.getName())))
                .collect(Collectors.joining());
        String selector = ".theme-" + getName();

        if (!isDark()) {
            return selector + " {\n" + variables + "}\n";
        }

        if (!followOsColorScheme) {
            return selector + ".dark {\n" + variables + "}\n";
        }

        return selector + ".dark {\n" + variables + "}\n\n" +
                "@media (prefers-color-scheme: dark) {\n" +
                "  " + selector + ":not(.light):not(.dark) {\n" +
                variables.lines().map(line -> "  " + line + "\n").collect(Collectors.joining()) +
                "  }\n" +
                "}\n";
    }

    private String getCssVariable(String variable, String value) {
        return Optional.ofNullable(value).map(v -> "  --" + StringUtil.camelToSnake(variable) + ": " + v + ";\n").orElse("");
    }
}

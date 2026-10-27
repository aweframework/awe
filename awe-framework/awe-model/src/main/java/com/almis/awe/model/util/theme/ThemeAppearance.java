package com.almis.awe.model.util.theme;

import java.util.Map;

/**
 * Resolved appearance of the AWE 5 theme of a session
 *
 * @param primary Primary palette or hex colour
 * @param surface Surface palette, auto or hex colour
 * @param mode    Mode (light, dark or system)
 * @param density Density (comfortable or compact)
 * @param palette Generated {@code --awe-primary-*} and {@code --awe-surface-*} custom properties of a hex primary or
 *                surface, empty when both are palette names
 */
public record ThemeAppearance(String primary, String surface, String mode, String density, Map<String, String> palette) {
}

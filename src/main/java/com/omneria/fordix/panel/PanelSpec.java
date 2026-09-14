package com.omneria.fordix.panel;

import java.util.List;

/**
 * Immutable description of a panel an admin has authored: what gets sent to the
 * client and rendered by GeneratedPanelScreen. Has no Minecraft client dependency
 * so it's safe to reference from server-only code.
 */
public record PanelSpec(String id, String title, String description, List<PanelButton> buttons) {

    public record PanelButton(String label, String command) {
    }
}

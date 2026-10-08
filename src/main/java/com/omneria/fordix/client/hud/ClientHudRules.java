package com.omneria.fordix.client.hud;

import com.omneria.fordix.Tr;
import com.omneria.fordix.hud.ClientboundHudRulesPayload;
import net.minecraft.client.Minecraft;

/** Receives the server's HUD rules and tells the player when the server suggests a layout. */
public final class ClientHudRules {
    private static String announced = "";

    private ClientHudRules() {
    }

    public static void receive(ClientboundHudRulesPayload payload) {
        HudRules.set(payload.required(), payload.forbidden(), payload.proposed());
        Minecraft minecraft = Minecraft.getInstance();
        String code = HudRules.proposed();
        if (!code.isEmpty() && !code.equals(announced) && minecraft.gui != null) {
            announced = code;
            minecraft.gui.setOverlayMessage(Tr.c("hud.rules.proposed"), false);
        }
    }

    public static void clear() {
        HudRules.clear();
        announced = "";
    }
}

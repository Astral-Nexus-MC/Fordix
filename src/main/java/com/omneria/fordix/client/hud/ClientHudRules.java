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
        HudRules.setAutoMode(payload.autoMode());
        HudRules.setSituations(payload.situationsForced(), payload.situationsTable(), payload.situationsProfiles());
        Minecraft minecraft = Minecraft.getInstance();
        String code = HudRules.proposed();
        if (!code.isEmpty() && !code.equals(announced) && minecraft.gui != null) {
            announced = code;
            HudToast.show(Tr.s("hud.toast.server"), Tr.s("hud.rules.proposed"), 0x3FD1C7);
        }
    }

    public static void clear() {
        HudRules.clear();
        announced = "";
    }
}

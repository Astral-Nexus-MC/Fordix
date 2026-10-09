package com.omneria.fordix.client.hud;

import com.omneria.fordix.Tr;
import com.omneria.fordix.hud.ClientboundHudRulesPayload;

/** Receives the server's HUD rules and tells the player when the server suggests a layout. */
public final class ClientHudRules {
    private static String announced = "";

    private ClientHudRules() {
    }

    public static void receive(ClientboundHudRulesPayload payload) {
        HudRules.set(payload.required(), payload.forbidden(), payload.proposed());
        HudRules.setSituations(payload.situationsTable(), payload.situationsProfiles());
        String code = HudRules.proposed();
        if (!code.isEmpty() && !code.equals(announced)) {
            announced = code;
            applyProposal(code);
        }
    }

    /** Puts the server's layout on screen once per code: what was applied is remembered, so a player's later changes stay. */
    private static void applyProposal(String code) {
        var layout = com.omneria.fordix.client.gui.layout.HudProfileCodec.decode(code);
        if (layout == null) {
            return;
        }
        java.nio.file.Path file = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("fordix-hud-applied.txt");
        String mark = Integer.toHexString(code.hashCode());
        try {
            if (java.nio.file.Files.isRegularFile(file) && java.nio.file.Files.readString(file).strip().equals(mark)) {
                return;
            }
            java.nio.file.Files.writeString(file, mark);
        } catch (java.io.IOException ignored) {
            // not remembered: it is applied again next time
        }
        com.omneria.fordix.client.gui.vanilla.HudProfileActions.applyLayout(layout);
        HudToast.show(Tr.s("hud.toast.server"), Tr.s("hud.rules.proposed"), 0x3FD1C7);
    }

    public static void clear() {
        HudRules.clear();
        announced = "";
    }
}

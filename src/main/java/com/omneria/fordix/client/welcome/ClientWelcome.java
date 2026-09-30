package com.omneria.fordix.client.welcome;

import com.omneria.fordix.client.gui.WelcomeScreen;
import com.omneria.fordix.welcome.ClientboundWelcomePayload;
import net.minecraft.client.Minecraft;

/** Client side of the welcome book: opens the screen, or refreshes it when it is already showing. */
public final class ClientWelcome {
    private ClientWelcome() {
    }

    public static void receive(ClientboundWelcomePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof WelcomeScreen screen) {
            screen.update(payload);
        } else if (payload.open()) {
            minecraft.setScreen(new WelcomeScreen(payload));
        }
    }
}

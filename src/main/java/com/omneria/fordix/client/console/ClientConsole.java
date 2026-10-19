package com.omneria.fordix.client.console;

import com.omneria.fordix.console.ClientboundConsolePayload;
import com.omneria.fordix.console.ConsoleData;
import net.minecraft.client.Minecraft;

/** Client side of the levelled-settings consoles: opens the screen, or refreshes it when it is already showing. */
public final class ClientConsole {
    private ClientConsole() {
    }

    public static void receive(ClientboundConsolePayload payload) {
        ConsoleData data = ConsoleData.fromTag(payload.data());
        if (data == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ConsoleScreen screen && screen.isFor(data)) {
            screen.update(data);
        } else {
            minecraft.setScreen(new ConsoleScreen(data));
        }
    }
}

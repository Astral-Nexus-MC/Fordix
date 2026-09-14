package com.omneria.fordix.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixDemoScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

// This class will not load on dedicated servers - safe to touch client-only rendering code here.
@Mod(value = Fordix.MODID, dist = Dist.CLIENT)
public class FordixClient {
    public static final KeyMapping TOGGLE_DEMO = new KeyMapping(
            "key.fordix.toggle_demo",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.fordix"
    );

    public FordixClient(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DEMO);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (TOGGLE_DEMO.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new FordixDemoScreen());
            }
        }
    }
}

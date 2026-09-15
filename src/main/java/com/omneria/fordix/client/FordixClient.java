package com.omneria.fordix.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.omneria.fordix.Fordix;
import com.omneria.fordix.client.gui.FordixDemoScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Split into two @EventBusSubscriber classes because a class-level subscriber
 * annotation only targets one bus (MOD or FORGE) at a time - key mapping
 * registration fires on the mod bus, tick events on the game (forge) bus.
 */
public final class FordixClient {
    public static final KeyMapping TOGGLE_DEMO = new KeyMapping(
            "key.fordix.toggle_demo",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.fordix"
    );

    private FordixClient() {
    }

    @Mod.EventBusSubscriber(modid = Fordix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_DEMO);
        }
    }

    @Mod.EventBusSubscriber(modid = Fordix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (TOGGLE_DEMO.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new FordixDemoScreen());
                }
            }
        }
    }
}

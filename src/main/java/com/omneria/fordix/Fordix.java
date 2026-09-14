package com.omneria.fordix;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here must match an entry in the META-INF/neoforge.mods.toml file.
// Fordix is a client-side UI library: this common entry point stays intentionally empty,
// all the actual GUI code lives in the client-only Fordix.CLIENT (FordixClient) sub-mod.
@Mod(Fordix.MODID)
public class Fordix {
    public static final String MODID = "fordix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Fordix(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Fordix UI library loaded");
    }
}

package com.omneria.fordix.welcome;

import com.omneria.fordix.Fordix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Fordix's only item: the welcome book, a checklist for new players. */
public final class FordixItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Fordix.MODID);

    public static final RegistryObject<Item> WELCOME_BOOK = ITEMS.register("welcome_book", WelcomeBookItem::new);

    private FordixItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(FordixItems::onCreativeTab);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WELCOME_BOOK);
        }
    }

    /** Right-click: the server sends the checklist and the client opens the screen. */
    public static final class WelcomeBookItem extends Item {
        public WelcomeBookItem() {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                WelcomeService.sendState(serverPlayer, true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}

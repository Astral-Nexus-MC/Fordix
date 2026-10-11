package com.omneria.fordix.client.gui.options;

import com.omneria.fordix.client.gui.widget.FlagRow;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.List;

/** Options > Skin customization in Voidframe 2: one switch per visible part of the skin, and the main hand. */
public final class SkinScreen extends OptionsPageScreen {
    public SkinScreen(Screen parent) {
        super(Component.translatable("options.skinCustomisation.title"), parent);
    }

    @Override
    protected int rowHeight() {
        return 22;
    }

    @Override
    protected int buildRows(int left, int width) {
        Options o = this.minecraft.options;
        startRows(left, width);
        for (PlayerModelPart part : PlayerModelPart.values()) {
            String title = part.getName().getString();
            FlagRow row = new FlagRow(rowX, rowY, rowW, rowHeight(), title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT),
                    0x3FD1C7, title, "", false, () -> o.isModelPartEnabled(part), v -> o.toggleModelPart(part, v));
            addRow(row, () -> o.toggleModelPart(part, true));
            rowY += rowHeight() + rowGap();
        }
        addPick(vanillaText("options.mainHand"), List.of(vanillaText("options.mainHand.left"), vanillaText("options.mainHand.right")),
                () -> o.mainHand().get() == HumanoidArm.LEFT ? 0 : 1, i -> o.mainHand().set(i == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT), 1);
        return rowY;
    }
}

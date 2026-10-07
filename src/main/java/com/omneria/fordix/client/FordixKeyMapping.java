package com.omneria.fordix.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

/**
 * A key of Fordix. Forge's own test for "two keys clash" falls back to comparing the plain keys whenever the contexts differ, so
 * Ctrl + F is reported against F on its own (and Shift + G against G) by the controls screen and by mods that list conflicts
 * (Controlling). A combination is not the plain key: here two keys clash only when they have the same key and the same
 * Shift / Ctrl / Alt, in contexts that can overlap.
 */
public final class FordixKeyMapping extends KeyMapping {
    public FordixKeyMapping(String name, IKeyConflictContext context, KeyModifier modifier, InputConstants.Type type, int code,
                            String category) {
        super(name, context, modifier, type, code, category);
    }

    public FordixKeyMapping(String name, IKeyConflictContext context, InputConstants.Type type, int code, String category) {
        super(name, context, type, code, category);
    }

    /**
     * Forge also warns when a combination uses a modifier that is itself bound alone (Ctrl + F against the Ctrl key, which the
     * game uses to sprint). Controlling lists those warnings with the clashes. A combination of ours is not the lone modifier, and
     * the contexts keep them apart where it matters, so no such warning is raised.
     */
    @Override
    public boolean hasKeyModifierConflict(KeyMapping other) {
        return false;
    }

    @Override
    public boolean same(KeyMapping other) {
        boolean overlap = getKeyConflictContext().conflicts(other.getKeyConflictContext())
                || other.getKeyConflictContext().conflicts(getKeyConflictContext());
        return overlap && getKey().equals(other.getKey()) && getKeyModifier() == other.getKeyModifier();
    }
}

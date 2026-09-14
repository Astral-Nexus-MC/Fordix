package com.omneria.fordix.panel.server;

import com.omneria.fordix.panel.PanelSpec;
import com.omneria.fordix.panel.network.ServerboundPanelActionPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Runs a clicked button's command. The command runs with the same permission level
 * required to author the panel (2, not the clicking player's own level) so a
 * regular player clicking a button can trigger only the finite, admin-authored
 * commands attached to it - never arbitrary commands of their own.
 */
public final class ServerPanelPayloadHandler {

    private ServerPanelPayloadHandler() {
    }

    public static void handle(ServerboundPanelActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            PanelSpec spec = PanelRegistry.getSent(payload.panelId());
            if (spec == null) {
                return;
            }

            List<PanelSpec.PanelButton> buttons = spec.buttons();
            if (payload.buttonIndex() < 0 || payload.buttonIndex() >= buttons.size()) {
                return;
            }

            String command = buttons.get(payload.buttonIndex()).command();
            CommandSourceStack source = serverPlayer.getServer().createCommandSourceStack()
                    .withEntity(serverPlayer)
                    .withPosition(serverPlayer.position())
                    .withLevel(serverPlayer.serverLevel())
                    .withPermission(Commands.LEVEL_GAMEMASTERS)
                    .withSuppressedOutput();

            serverPlayer.getServer().getCommands().performPrefixedCommand(source, command);
        });
    }
}

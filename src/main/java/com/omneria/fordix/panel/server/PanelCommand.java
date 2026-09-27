package com.omneria.fordix.panel.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.omneria.fordix.panel.PanelSpec;
import com.omneria.fordix.panel.network.ClientboundOpenPanelPayload;
import com.omneria.fordix.panel.network.PanelNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;

/**
 * /fordix panel create|describe|button|send - lets an admin build and broadcast a
 * panel entirely from chat, no mod code required. Gated to permission level 2
 * (same as /gamemode, /give) so only ops can author or send panels.
 */
public final class PanelCommand {

    private PanelCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fordix")
                .then(Commands.literal("panel")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(PanelCommand::create))))
                        .then(Commands.literal("describe")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(PanelCommand::describe))))
                        .then(Commands.literal("button")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("label", StringArgumentType.string())
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(PanelCommand::addButton)))))
                        .then(Commands.literal("send")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(PanelCommand::send))))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String title = StringArgumentType.getString(ctx, "title");
        PanelRegistry.draft(ctx.getSource().getServer(), id).setTitle(title);
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' : titre mis a jour."), false);
        return 1;
    }

    private static int describe(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String description = StringArgumentType.getString(ctx, "description");
        PanelRegistry.draft(ctx.getSource().getServer(), id).setDescription(description);
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' : description mise a jour."), false);
        return 1;
    }

    private static int addButton(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String label = StringArgumentType.getString(ctx, "label");
        String command = StringArgumentType.getString(ctx, "command");
        PanelRegistry.draft(ctx.getSource().getServer(), id).addButton(label, command);
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' : bouton '" + label + "' ajoute."), false);
        return 1;
    }

    private static int send(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

        PanelSpec spec = PanelRegistry.draft(ctx.getSource().getServer(), id).toSpec();

        List<String> labels = spec.buttons().stream().map(PanelSpec.PanelButton::label).toList();
        ClientboundOpenPanelPayload payload = new ClientboundOpenPanelPayload(spec.id(), spec.title(), spec.description(), labels);
        for (ServerPlayer player : targets) {
            PanelRegistry.markSent(player.getUUID(), spec);
            PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' envoye a " + count + " joueur(s)."), false);
        return count;
    }
}

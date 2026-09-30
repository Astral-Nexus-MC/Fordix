package com.omneria.fordix.panel.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.omneria.fordix.panel.PanelSpec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * /fordix panel create|describe|button|send|list|delete - lets an admin build and broadcast a
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
                                        .suggests(PanelCommand::suggestPanelIds)
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(PanelCommand::send))))
                        .then(Commands.literal("list")
                                .executes(PanelCommand::list))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(PanelCommand::suggestPanelIds)
                                        .executes(PanelCommand::delete)))));
    }

    private static CompletableFuture<Suggestions> suggestPanelIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PanelRegistry.ids(ctx.getSource().getServer()), builder);
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

        PanelDraft draft = PanelRegistry.find(ctx.getSource().getServer(), id);
        if (draft == null) {
            ctx.getSource().sendFailure(Component.literal("Panel '" + id + "' introuvable."));
            return 0;
        }
        PanelSpec spec = draft.toSpec();

        PanelSender.send(spec, targets);

        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' envoye a " + count + " joueur(s)."), false);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        List<String> ids = PanelRegistry.ids(ctx.getSource().getServer());
        if (ids.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Aucun panel enregistre."), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(ids.size() + " panel(s) : " + String.join(", ", ids)), false);
        }
        return ids.size();
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if (!PanelRegistry.delete(ctx.getSource().getServer(), id)) {
            ctx.getSource().sendFailure(Component.literal("Panel '" + id + "' introuvable."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Panel '" + id + "' supprime."), false);
        return 1;
    }
}

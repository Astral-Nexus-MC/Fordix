package com.omneria.fordix.notification.server;

import com.omneria.fordix.Tr;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.omneria.fordix.notification.FordixNotifications;
import com.omneria.fordix.notification.NotificationHistory;
import com.omneria.fordix.notification.NotificationStyle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * /fordix notify &lt;targets&gt; &lt;style&gt; [seconds] &lt;text&gt; - text is "Title | message" (the message
 * part is optional). Targets are a player selector, or "team &lt;name&gt;", "tag &lt;name&gt;" and
 * "permission &lt;level&gt;" for groups. /fordix notify history [count] lists what was sent. Ops only,
 * like the panel commands.
 */
public final class NotifyCommand {
    private static final int MAX_TEXT_LENGTH = 256;
    private static final int DEFAULT_HISTORY = 10;

    private interface TargetResolver {
        NotificationTargets.Resolved resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private NotifyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fordix")
                .then(Commands.literal("notify")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(styleNode(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    return new NotificationTargets.Resolved(players.size() == 1
                                            ? "joueur " + players.iterator().next().getGameProfile().getName()
                                            : "selection", List.copyOf(players));
                                })))
                        .then(Commands.literal("team")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(styleNode(ctx -> NotificationTargets.team(ctx.getSource().getServer(),
                                                StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("tag")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(styleNode(ctx -> NotificationTargets.tag(ctx.getSource().getServer(),
                                                StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("permission")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                                        .then(styleNode(ctx -> NotificationTargets.permission(ctx.getSource().getServer(),
                                                IntegerArgumentType.getInteger(ctx, "level"))))))
                        .then(Commands.literal("history")
                                .executes(ctx -> history(ctx, DEFAULT_HISTORY))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                        .executes(ctx -> history(ctx, IntegerArgumentType.getInteger(ctx, "count")))))));
    }

    /** The "style [seconds] text" tail shared by every kind of target. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> styleNode(TargetResolver resolver) {
        return Commands.argument("style", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(NotificationStyle.values()).map(NotificationStyle::id), builder))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, FordixNotifications.MAX_SECONDS))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> notify(ctx, resolver, IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> notify(ctx, resolver, FordixNotifications.DEFAULT_SECONDS)));
    }

    private static int notify(CommandContext<CommandSourceStack> ctx, TargetResolver resolver, int seconds) throws CommandSyntaxException {
        NotificationTargets.Resolved target = resolver.resolve(ctx);
        String styleId = StringArgumentType.getString(ctx, "style");
        NotificationStyle style = NotificationStyle.byId(styleId);
        if (style == null) {
            ctx.getSource().sendFailure(Tr.c("cmd.notify.bad_style", styleId));
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "text");
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        int separator = text.indexOf('|');
        String title = separator < 0 ? text.trim() : text.substring(0, separator).trim();
        String message = separator < 0 ? "" : text.substring(separator + 1).trim();

        int count = NotificationSender.send(ctx.getSource().getTextName(), style, seconds, title, message,
                target.label(), target.players());
        ctx.getSource().sendSuccess(() -> Tr.c("cmd.notify.sent", count, target.label()), false);
        return count;
    }

    private static int history(CommandContext<CommandSourceStack> ctx, int count) {
        List<NotificationHistory.Entry> entries = NotificationSender.HISTORY.latest(count);
        if (entries.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Tr.c("cmd.notify.none"), false);
            return 0;
        }
        for (NotificationHistory.Entry entry : entries) {
            ctx.getSource().sendSuccess(() -> Component.literal(NotificationHistory.format(entry)), false);
        }
        return entries.size();
    }
}

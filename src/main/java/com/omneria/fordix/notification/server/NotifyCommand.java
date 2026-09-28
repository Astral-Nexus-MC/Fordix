package com.omneria.fordix.notification.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.omneria.fordix.notification.FordixNotifications;
import com.omneria.fordix.notification.NotificationStyle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;

/**
 * /fordix notify &lt;targets&gt; &lt;style&gt; [seconds] &lt;text&gt; - text is "Title | message"
 * (the message part is optional). Ops only, like the panel commands.
 */
public final class NotifyCommand {
    private static final int MAX_TEXT_LENGTH = 256;

    private NotifyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fordix")
                .then(Commands.literal("notify")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("style", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(NotificationStyle.values()).map(NotificationStyle::id), builder))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, FordixNotifications.MAX_SECONDS))
                                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                                        .executes(ctx -> notify(ctx, IntegerArgumentType.getInteger(ctx, "seconds")))))
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> notify(ctx, FordixNotifications.DEFAULT_SECONDS)))))));
    }

    private static int notify(CommandContext<CommandSourceStack> ctx, int seconds) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String styleId = StringArgumentType.getString(ctx, "style");
        NotificationStyle style = NotificationStyle.byId(styleId);
        if (style == null) {
            ctx.getSource().sendFailure(Component.literal("Style '" + styleId + "' inconnu (info, success, warning, error)."));
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "text");
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        int separator = text.indexOf('|');
        String title = separator < 0 ? text.trim() : text.substring(0, separator).trim();
        String message = separator < 0 ? "" : text.substring(separator + 1).trim();

        for (ServerPlayer player : targets) {
            FordixNotifications.send(player, style, Component.literal(title), Component.literal(message), seconds);
        }
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Notification envoyee a " + count + " joueur(s)."), false);
        return count;
    }
}

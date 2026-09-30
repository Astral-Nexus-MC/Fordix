package com.omneria.fordix.welcome;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/** {@code /fordix book [players]}: gives the welcome book (ops). */
public final class WelcomeCommand {
    private WelcomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fordix")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("book")
                        .executes(context -> give(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> give(context.getSource(), EntityArgument.getPlayers(context, "players"))))));
    }

    private static int give(CommandSourceStack source, Collection<ServerPlayer> players) {
        players.forEach(WelcomeService::give);
        source.sendSuccess(() -> Component.literal("Livre de bienvenue donne a " + players.size() + " joueur(s)."), true);
        return players.size();
    }
}

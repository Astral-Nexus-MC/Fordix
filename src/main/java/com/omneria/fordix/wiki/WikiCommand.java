package com.omneria.fordix.wiki;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.omneria.fordix.Tr;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /fordix wiki ...} (admins): {@code import <wiki> <page>} copies a wiki page into the game, {@code importlinks} also
 * copies the pages it links to, {@code list}, {@code remove <wiki> [page]} and {@code license <wiki> <text>}. A wiki is named
 * after the mod it documents (twilightforest, mowziesmobs, minecraft): its guide tab then opens in the game instead of a web page.
 */
public final class WikiCommand {
    private WikiCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fordix")
                .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("wiki")
                        .then(Commands.literal("import")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("page", StringArgumentType.greedyString())
                                                .executes(context -> startImport(context, false)))))
                        .then(Commands.literal("importlinks")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("page", StringArgumentType.greedyString())
                                                .executes(context -> startImport(context, true)))))
                        .then(Commands.literal("list").executes(WikiCommand::list))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .executes(context -> remove(context, ""))
                                        .then(Commands.argument("page", StringArgumentType.greedyString())
                                                .executes(context -> remove(context, StringArgumentType.getString(context, "page"))))))
                        .then(Commands.literal("license")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(WikiCommand::license))))));
    }

    private static int startImport(CommandContext<CommandSourceStack> context, boolean withLinks) {
        CommandSourceStack source = context.getSource();
        String wiki = StringArgumentType.getString(context, "wiki");
        String page = StringArgumentType.getString(context, "page");
        if (!WikiInfo.validId(wiki)) {
            source.sendFailure(Tr.c("cmd.wiki.bad_id"));
            return 0;
        }
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Tr.c("cmd.wiki.start", page, wiki), true);
        WikiImporter.importPage(WikiService.store(), WikiService.images(), wiki, page, withLinks,
                        message -> server.execute(() -> source.sendSuccess(() -> Tr.c("cmd.wiki.progress", message), false)))
                .whenComplete((report, error) -> server.execute(() -> {
                    if (error != null) {
                        source.sendFailure(Tr.c("cmd.wiki.fail", error.getMessage()));
                        return;
                    }
                    source.sendSuccess(() -> Tr.c("cmd.wiki.done", report.pages(), report.images()), true);
                    for (String failure : report.failures().stream().limit(5).toList()) {
                        source.sendFailure(Tr.c("cmd.wiki.skipped", failure));
                    }
                    WikiInfo info = WikiService.store().info(wiki);
                    if (info != null && info.license.isEmpty()) {
                        source.sendSuccess(() -> Tr.c("cmd.wiki.license_missing", wiki), false);
                    }
                    WikiService.broadcastList(server);
                }));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var wikis = WikiService.store().wikis();
        if (wikis.isEmpty()) {
            source.sendSuccess(() -> Tr.c("cmd.wiki.none"), false);
        }
        for (WikiInfo info : wikis) {
            source.sendSuccess(() -> Tr.c("cmd.wiki.line", info.id, info.title, WikiService.store().count(info.id), info.source,
                    info.license.isEmpty() ? "?" : info.license), false);
        }
        return wikis.size();
    }

    private static int remove(CommandContext<CommandSourceStack> context, String page) {
        CommandSourceStack source = context.getSource();
        String wiki = StringArgumentType.getString(context, "wiki");
        boolean removed = page.isEmpty() ? WikiService.store().removeWiki(wiki) : WikiService.store().remove(wiki, page.replace(' ', '_'));
        if (removed) {
            source.sendSuccess(() -> Tr.c("cmd.wiki.removed", page.isEmpty() ? wiki : wiki + "/" + page), true);
            WikiService.broadcastList(source.getServer());
            return 1;
        }
        source.sendFailure(Tr.c("cmd.wiki.not_found"));
        return 0;
    }

    private static int license(CommandContext<CommandSourceStack> context) {
        String wiki = StringArgumentType.getString(context, "wiki");
        if (WikiService.store().info(wiki) == null) {
            context.getSource().sendFailure(Tr.c("cmd.wiki.not_found"));
            return 0;
        }
        WikiService.store().setLicense(wiki, StringArgumentType.getString(context, "text"));
        context.getSource().sendSuccess(() -> Tr.c("cmd.wiki.license_set", wiki), true);
        WikiService.broadcastList(context.getSource().getServer());
        return 1;
    }
}

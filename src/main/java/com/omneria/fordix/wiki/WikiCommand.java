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
                        .then(Commands.literal("importall")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("what", StringArgumentType.word())
                                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                                        .suggest(java.util.List.of("items", "mobs", "all"), builder))
                                                .executes(WikiCommand::importAll))))
                        .then(Commands.literal("translate")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("lang", StringArgumentType.word())
                                                .then(Commands.argument("address", StringArgumentType.greedyString())
                                                        .executes(WikiCommand::translate)))))
                        .then(Commands.literal("list").executes(WikiCommand::list))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .executes(context -> remove(context, ""))
                                        .then(Commands.argument("page", StringArgumentType.greedyString())
                                                .executes(context -> remove(context, StringArgumentType.getString(context, "page"))))))
                        .then(Commands.literal("asof")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("date", StringArgumentType.word()).executes(WikiCommand::asOf))))
                        .then(Commands.literal("javaonly")
                                .then(Commands.argument("wiki", StringArgumentType.word())
                                        .then(Commands.argument("on", StringArgumentType.word()).executes(WikiCommand::javaOnly))))
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
        if (WikiService.store().info(wiki) == null) {
            WikiInfo created = WikiService.store().ensure(wiki, wiki, "", "");
            applyGameDefaults(created);
            source.sendSuccess(() -> Tr.c("cmd.wiki.defaults", wiki, created.gameVersion,
                    created.asOf.isEmpty() ? "-" : created.asOf, created.javaOnly ? "Java" : "-"), true);
        }
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

    /**
     * {@code translate <wiki> <lang> <address of that language's wiki>}: a wiki "<wiki>_<lang>" with the pages of the same wiki in
     * another language, one for each page the wiki already has (the French players of the server read it instead).
     */
    private static int translate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String wiki = StringArgumentType.getString(context, "wiki");
        String lang = StringArgumentType.getString(context, "lang").toLowerCase(java.util.Locale.ROOT);
        String address = StringArgumentType.getString(context, "address").strip();
        WikiInfo from = WikiService.store().info(wiki);
        if (from == null || from.api.isEmpty()) {
            source.sendFailure(Tr.c("cmd.wiki.not_found"));
            return 0;
        }
        if (!lang.matches("[a-z]{2,3}") || !(address.startsWith("https://") || address.startsWith("http://"))) {
            source.sendFailure(Tr.c("cmd.wiki.bad_translate"));
            return 0;
        }
        String target = wiki + "_" + lang;
        if (!WikiInfo.validId(target)) {
            source.sendFailure(Tr.c("cmd.wiki.bad_id"));
            return 0;
        }
        java.net.URI uri = java.net.URI.create(address);
        String origin = uri.getScheme() + "://" + uri.getAuthority();
        WikiInfo created = WikiService.store().ensure(target, from.title + " (" + lang + ")", origin, "");
        created.source = origin;
        created.gameVersion = from.gameVersion;
        created.asOf = from.asOf;
        created.javaOnly = from.javaOnly;
        WikiService.store().update(created);
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Tr.c("cmd.wiki.translate_start", wiki, lang, WikiService.store().count(wiki)), true);
        WikiImporter.translate(WikiService.store(), WikiService.images(), wiki, target, lang,
                        message -> server.execute(() -> source.sendSuccess(() -> Tr.c("cmd.wiki.progress", message), false)))
                .whenComplete((report, error) -> server.execute(() -> {
                    if (error != null) {
                        source.sendFailure(Tr.c("cmd.wiki.fail", error.getMessage()));
                        return;
                    }
                    source.sendSuccess(() -> Tr.c("cmd.wiki.done", report.pages(), report.images()), true);
                    WikiService.broadcastList(server);
                }));
        return 1;
    }

    /** {@code importall <wiki> <items|mobs|all>}: the page of every item and/or creature the mod of that name adds. */
    private static int importAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String wiki = StringArgumentType.getString(context, "wiki");
        String what = StringArgumentType.getString(context, "what");
        WikiInfo known = WikiService.store().info(wiki);
        if (known == null || known.source.isEmpty()) {
            if (!wiki.equals("minecraft")) {
                source.sendFailure(Tr.c("cmd.wiki.need_first", wiki, wiki));
                return 0;
            }
            // The vanilla wiki's address is known: no need to import a page first.
            WikiInfo created = WikiService.store().ensure(wiki, wiki, "https://minecraft.wiki", "");
            if (created.source.isEmpty()) {
                created.source = "https://minecraft.wiki";
            }
            if (created.gameVersion.isEmpty()) {
                applyGameDefaults(created);
            } else {
                WikiService.store().update(created);
            }
        }
        if (!what.equals("items") && !what.equals("mobs") && !what.equals("all")) {
            source.sendFailure(Tr.c("cmd.wiki.bad_what"));
            return 0;
        }
        java.util.LinkedHashSet<String> titles = new java.util.LinkedHashSet<>();
        if (!what.equals("mobs")) {
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().stream().filter(key -> key.getNamespace().equals(wiki))
                    .sorted().forEach(key -> titles.add(WikiImporter.titleOf(key.getPath())));
        }
        if (!what.equals("items")) {
            net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKeys().stream().filter(key -> key.getNamespace().equals(wiki))
                    .sorted().forEach(key -> titles.add(WikiImporter.titleOf(key.getPath())));
        }
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Tr.c("cmd.wiki.all_start", titles.size(), wiki), true);
        WikiImporter.importTitles(WikiService.store(), WikiService.images(), wiki, new java.util.ArrayList<>(titles),
                        message -> server.execute(() -> source.sendSuccess(() -> Tr.c("cmd.wiki.progress", message), false)))
                .whenComplete((report, error) -> server.execute(() -> {
                    if (error != null) {
                        source.sendFailure(Tr.c("cmd.wiki.fail", error.getMessage()));
                        return;
                    }
                    source.sendSuccess(() -> Tr.c("cmd.wiki.done", report.pages(), report.images()), true);
                    WikiService.broadcastList(server);
                }));
        return titles.size();
    }

    /**
     * A new wiki is set for the game version the server runs: its version is noted and, for the vanilla wiki, it is frozen two
     * weeks after that version's release (the wiki then documents it) and keeps only the Java Edition.
     */
    private static void applyGameDefaults(WikiInfo info) {
        var version = net.minecraft.SharedConstants.getCurrentVersion();
        info.gameVersion = version.getName();
        if (info.id.equals("minecraft")) {
            info.asOf = java.time.LocalDate.ofInstant(version.getBuildTime().toInstant(), java.time.ZoneOffset.UTC).plusDays(14).toString();
            info.javaOnly = true;
        }
        WikiService.store().update(info);
    }

    private static int asOf(CommandContext<CommandSourceStack> context) {
        String wiki = StringArgumentType.getString(context, "wiki");
        String date = StringArgumentType.getString(context, "date");
        WikiInfo info = WikiService.store().info(wiki);
        if (info == null) {
            context.getSource().sendFailure(Tr.c("cmd.wiki.not_found"));
            return 0;
        }
        if (date.equals("off")) {
            info.asOf = "";
            WikiService.store().update(info);
            context.getSource().sendSuccess(() -> Tr.c("cmd.wiki.asof_off", wiki), true);
            return 1;
        }
        if (!WikiInfo.validDate(date)) {
            context.getSource().sendFailure(Tr.c("cmd.wiki.bad_date"));
            return 0;
        }
        info.asOf = date;
        WikiService.store().update(info);
        context.getSource().sendSuccess(() -> Tr.c("cmd.wiki.asof_set", wiki, date), true);
        return 1;
    }

    private static int javaOnly(CommandContext<CommandSourceStack> context) {
        String wiki = StringArgumentType.getString(context, "wiki");
        WikiInfo info = WikiService.store().info(wiki);
        if (info == null) {
            context.getSource().sendFailure(Tr.c("cmd.wiki.not_found"));
            return 0;
        }
        info.javaOnly = StringArgumentType.getString(context, "on").equalsIgnoreCase("on");
        WikiService.store().update(info);
        context.getSource().sendSuccess(() -> Tr.c(info.javaOnly ? "cmd.wiki.javaonly_on" : "cmd.wiki.javaonly_off", wiki), true);
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

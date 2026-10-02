package com.omneria.fordix.admin;

import com.omneria.fordix.Tr;
import com.omneria.fordix.guide.GuidePage;
import com.omneria.fordix.guide.server.GuideBroadcaster;
import com.omneria.fordix.notification.FordixNotifications;
import com.omneria.fordix.notification.NotificationHistory;
import com.omneria.fordix.notification.NotificationStyle;
import com.omneria.fordix.notification.server.NotificationScheduler;
import com.omneria.fordix.notification.server.NotificationSender;
import com.omneria.fordix.notification.server.NotificationTargets;
import com.omneria.fordix.panel.PanelSpec;
import com.omneria.fordix.panel.network.PanelNetwork;
import com.omneria.fordix.panel.server.PanelDraft;
import com.omneria.fordix.panel.server.PanelRegistry;
import com.omneria.fordix.panel.server.PanelSender;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Answers the in-game admin screen. Every request is checked against the same permission level
 * as the /fordix commands (ops), then runs the same server code: no command string is built from
 * client text, so nothing the client sends can reach beyond panels and notifications.
 */
public final class AdminHandler {
    private static final Pattern PANEL_ID = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final int MAX_BUTTONS = 12;
    private static final int MAX_TEXT = 256;
    private static final int HISTORY_LINES = 12;
    private static final int MAX_MINUTES = 60 * 24 * 30;
    private static final Pattern TEMPLATE_NAME = Pattern.compile("[^\\t\\n]{1,32}");

    private AdminHandler() {
    }

    public static void handle(ServerboundAdminPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (!player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                reply(player, "denied", List.of(Tr.encode("msg.denied")));
                return;
            }
            MinecraftServer server = player.getServer();
            List<String> args = payload.args();
            switch (payload.action()) {
                case "panels" -> reply(player, "panels", PanelRegistry.ids(server));
                case "panel" -> panel(player, server, arg(args, 0));
                case "save_panel" -> savePanel(player, server, args);
                case "delete_panel" -> {
                    String id = arg(args, 0);
                    boolean existed = PanelRegistry.delete(server, id);
                    status(player, existed ? "msg.panel.deleted" : "msg.panel.missing", id);
                    reply(player, "panels", PanelRegistry.ids(server));
                }
                case "send_panel" -> {
                    PanelDraft draft = PanelRegistry.find(server, arg(args, 0));
                    if (draft == null) {
                        status(player, "msg.panel.missing", arg(args, 0));
                    } else {
                        PanelSender.send(draft.toSpec(), List.of(player));
                    }
                }
                case "notify" -> notify(player, server, args);
                case "history" -> history(player);
                case "schedules" -> schedules(player);
                case "schedule_add" -> scheduleAdd(player, args);
                case "schedule_delete" -> {
                    int id = parseInt(arg(args, 0), -1);
                    boolean removed = NotificationScheduler.store().deleteSchedule(id);
                    NotificationScheduler.save();
                    status(player, removed ? "msg.schedule.deleted" : "msg.schedule.missing", id);
                    schedules(player);
                }
                case "pages" -> pageList(player);
                case "page" -> pageGet(player, arg(args, 0));
                case "page_save" -> pageSave(player, server, args);
                case "page_delete" -> {
                    String id = arg(args, 0);
                    boolean removed = NotificationScheduler.store().deletePage(id);
                    NotificationScheduler.save();
                    GuideBroadcaster.broadcastPages(server);
                    status(player, removed ? "msg.page.deleted" : "msg.page.missing", id);
                    pageList(player);
                }
                case "book" -> book(player);
                case "book_save" -> bookSave(player, args);
                case "book_reset" -> {
                    NotificationScheduler.store().resetWelcomeBook();
                    NotificationScheduler.save();
                    status(player, "msg.book.reset");
                    book(player);
                }
                case "templates" -> templates(player);
                case "template_save" -> templateSave(player, args);
                case "template_delete" -> {
                    String name = arg(args, 0);
                    boolean removed = NotificationScheduler.store().deleteTemplate(name);
                    NotificationScheduler.save();
                    status(player, removed ? "msg.template.deleted" : "msg.template.missing", name);
                    templates(player);
                }
                default -> status(player, "msg.unknown_action");
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void panel(ServerPlayer player, MinecraftServer server, String id) {
        PanelDraft draft = PanelRegistry.find(server, id);
        if (draft == null) {
            status(player, "msg.panel.missing", id);
            return;
        }
        PanelSpec spec = draft.toSpec();
        List<String> lines = new ArrayList<>(List.of(spec.id(), spec.title(), spec.description()));
        for (PanelSpec.PanelButton button : spec.buttons()) {
            lines.add(button.label() + "\t" + button.command());
        }
        reply(player, "panel", lines);
    }

    /** Args: id, title, description, then "label TAB command" per button. Creates the panel if it is new. */
    private static void savePanel(ServerPlayer player, MinecraftServer server, List<String> args) {
        String id = arg(args, 0);
        if (!PANEL_ID.matcher(id).matches()) {
            status(player, "msg.bad_id");
            return;
        }
        List<PanelSpec.PanelButton> buttons = new ArrayList<>();
        for (int i = 3; i < args.size() && buttons.size() < MAX_BUTTONS; i++) {
            String[] parts = args.get(i).split("\t", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                buttons.add(new PanelSpec.PanelButton(clip(parts[0].trim()), clip(parts[1].trim())));
            }
        }
        PanelDraft draft = PanelRegistry.draft(server, id);
        draft.setTitle(clip(arg(args, 1).isBlank() ? id : arg(args, 1)));
        draft.setDescription(clip(arg(args, 2)));
        draft.replaceButtons(buttons);
        status(player, "msg.panel.saved", id, buttons.size());
        reply(player, "panels", PanelRegistry.ids(server));
    }

    /** Args: style, seconds, title, message, target kind, target value. */
    private static void notify(ServerPlayer player, MinecraftServer server, List<String> args) {
        NotificationStyle style = NotificationStyle.byId(arg(args, 0));
        if (style == null) {
            status(player, "msg.bad_style");
            return;
        }
        int seconds = FordixNotifications.DEFAULT_SECONDS;
        try {
            seconds = Integer.parseInt(arg(args, 1));
        } catch (NumberFormatException ignored) {
            // keep the default
        }
        String title = clip(arg(args, 2));
        if (title.isBlank()) {
            status(player, "msg.empty_title");
            return;
        }
        NotificationTargets.Resolved target = NotificationTargets.byKind(server, arg(args, 4), arg(args, 5));
        int count = NotificationSender.send(player.getGameProfile().getName(), style, seconds, title, clip(arg(args, 3)),
                target.label(), target.players());
        status(player, "msg.notify.sent", count, target.label());
        history(player);
    }

    /** Args: style, seconds, title, message, target kind, target value, mode ("once" or "every"), minutes. */
    private static void scheduleAdd(ServerPlayer player, List<String> args) {
        NotificationStyle style = NotificationStyle.byId(arg(args, 0));
        String title = clip(arg(args, 2));
        if (style == null || title.isBlank()) {
            status(player, style == null ? "msg.bad_style" : "msg.empty_title");
            return;
        }
        int minutes = parseInt(arg(args, 7), 0);
        if (minutes < 1 || minutes > MAX_MINUTES) {
            status(player, "msg.bad_delay", MAX_MINUTES);
            return;
        }
        boolean repeating = arg(args, 6).equals("every");
        long now = System.currentTimeMillis();
        long delay = minutes * 60_000L;
        var entry = NotificationScheduler.store().schedule(style.id(), Math.max(1, Math.min(120, parseInt(arg(args, 1), 5))),
                title, clip(arg(args, 3)), arg(args, 4), clip(arg(args, 5)), now + delay, repeating ? delay : 0,
                player.getGameProfile().getName());
        if (entry == null) {
            status(player, "msg.too_many_schedules", com.omneria.fordix.notification.AdminStore.MAX_SCHEDULES);
            return;
        }
        NotificationScheduler.save();
        status(player, "msg.schedule.saved", entry.id);
        schedules(player);
    }

    private static void schedules(ServerPlayer player) {
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        for (var entry : NotificationScheduler.store().schedules) {
            lines.add(String.join("\t", Integer.toString(entry.id), entry.everyMs > 0 ? "every" : "once",
                    Long.toString(entry.minutes(now)), entry.title, entry.kind, entry.value));
        }
        reply(player, "schedules", lines);
    }

    /** Args: name, style, seconds, title, message. */
    private static void templateSave(ServerPlayer player, List<String> args) {
        String name = arg(args, 0).trim();
        NotificationStyle style = NotificationStyle.byId(arg(args, 1));
        if (!TEMPLATE_NAME.matcher(name).matches() || style == null) {
            status(player, style == null ? "msg.bad_style" : "msg.bad_name");
            return;
        }
        boolean saved = NotificationScheduler.store().saveTemplate(name, style.id(),
                Math.max(1, Math.min(120, parseInt(arg(args, 2), 5))), clip(arg(args, 3)), clip(arg(args, 4)));
        if (!saved) {
            status(player, "msg.too_many_templates", com.omneria.fordix.notification.AdminStore.MAX_TEMPLATES);
            return;
        }
        NotificationScheduler.save();
        status(player, "msg.template.saved", name);
        templates(player);
    }

    private static void templates(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        NotificationScheduler.store().templates.forEach((name, template) -> lines.add(String.join("\t", name, template.style,
                Integer.toString(template.seconds), template.title, template.message)));
        reply(player, "templates", lines);
    }

    /** Reply: the reward command, then one config-format line per step. */
    private static void book(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add(com.omneria.fordix.welcome.WelcomeService.rewardCommand());
        lines.addAll(com.omneria.fordix.welcome.WelcomeService.stepLines());
        reply(player, "book", lines);
    }

    /** Args: the reward command, then one config-format line per step. */
    private static void bookSave(ServerPlayer player, List<String> args) {
        List<String> steps = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) {
            String line = args.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            if (com.omneria.fordix.welcome.WelcomeStep.parse(line) == null) {
                status(player, "msg.book.bad_step", clip(line));
                return;
            }
            steps.add(line);
        }
        if (!NotificationScheduler.store().setWelcomeBook(steps, arg(args, 0).strip())) {
            status(player, "msg.book.too_many", com.omneria.fordix.notification.AdminStore.MAX_BOOK_STEPS);
            return;
        }
        NotificationScheduler.save();
        status(player, "msg.book.saved", steps.size());
        book(player);
    }

    private static void pageList(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        NotificationScheduler.store().pages.values().forEach(page -> lines.add(page.id + "\t" + page.title));
        reply(player, "pages", lines);
    }

    private static void pageGet(ServerPlayer player, String id) {
        GuidePage page = NotificationScheduler.store().pages.get(id);
        if (page == null) {
            status(player, "msg.page.missing", id);
            return;
        }
        reply(player, "page", List.of(page.id, page.title, page.body));
    }

    /** Args: id, title, body. Saves and pushes the pages to every player. */
    private static void pageSave(ServerPlayer player, MinecraftServer server, List<String> args) {
        String id = arg(args, 0);
        if (!PANEL_ID.matcher(id).matches()) {
            status(player, "msg.bad_id");
            return;
        }
        String body = arg(args, 2);
        if (body.length() > GuidePage.MAX_BODY) {
            body = body.substring(0, GuidePage.MAX_BODY);
        }
        String title = clip(arg(args, 1).isBlank() ? id : arg(args, 1));
        if (!NotificationScheduler.store().savePage(id, title, body, player.getGameProfile().getName(), System.currentTimeMillis())) {
            status(player, "msg.too_many_pages", com.omneria.fordix.notification.AdminStore.MAX_PAGES);
            return;
        }
        NotificationScheduler.save();
        GuideBroadcaster.broadcastPages(server);
        status(player, "msg.page.saved", id);
        pageList(player);
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void history(ServerPlayer player) {
        List<String> lines = NotificationSender.HISTORY.latest(HISTORY_LINES).stream()
                .map(NotificationHistory::format).toList();
        reply(player, "history", lines);
    }

    private static String arg(List<String> args, int index) {
        return index < args.size() ? args.get(index) : "";
    }

    private static String clip(String text) {
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }

    private static void status(ServerPlayer player, String key, Object... args) {
        reply(player, "status", List.of(Tr.encode(key, args)));
    }

    private static void reply(ServerPlayer player, String kind, List<String> lines) {
        PanelNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundAdminPayload(kind, lines));
    }
}

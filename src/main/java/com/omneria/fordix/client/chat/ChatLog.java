package com.omneria.fordix.client.chat;

import com.omneria.fordix.Fordix;
import com.omneria.fordix.FordixConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The chat's lines as the Voidframe chat shows them. Vanilla stays the source of truth: every line that reaches its
 * chat component, whatever sent it (server, command feedback, another mod), is picked up on the next tick and
 * classified, timed and, for a player's line, split into name and message. Client side only.
 */
public final class ChatLog {
    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final Field ALL_MESSAGES = findMessages();

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<ChatTab, Integer> UNREAD = new EnumMap<>(ChatTab.class);
    private static GuiMessage lastTop;

    private ChatLog() {
    }

    /** One line of chat, with what it needs to be drawn. */
    public static final class Entry {
        public final long addedAt = System.currentTimeMillis();
        public final String time = LocalTime.now().format(CLOCK);
        public final ChatKind kind;
        /** The whole line for anything but a player; the message alone (name apart) for a player. */
        public final Component body;
        /** The player's name as sent (colored by team), or null. */
        public final Component name;
        public final UUID sender;
        public final String senderName;
        public final boolean mention;
        private String plain;
        private List<FormattedCharSequence> lines = List.of();
        private int linesWidth = -1;

        Entry(ChatKind kind, Component body, Component name, UUID sender, String senderName, boolean mention) {
            this.kind = kind;
            this.body = body;
            this.name = name;
            this.sender = sender;
            this.senderName = senderName;
            this.mention = mention;
        }

        /** The line as plain text (name and message), what the search looks through. */
        public String plainText() {
            if (plain == null) {
                plain = (name == null ? "" : name.getString() + " ") + body.getString();
            }
            return plain;
        }

        /** The text wrapped to a width: "Name message" for a player, the line itself otherwise. */
        public List<FormattedCharSequence> lines(Font font, int width) {
            if (linesWidth != width) {
                Component whole = name == null ? body
                        : Component.empty().append(name.copy().withStyle(style -> style.withBold(true))).append(" ").append(body);
                if (kind == ChatKind.COMMAND) {
                    whole = whole.copy().withStyle(ChatFormatting.GRAY);
                }
                lines = font.split(whole, Math.max(20, width));
                linesWidth = width;
            }
            return lines;
        }

        /** The sender's skin, when the line is a player's and that player is online. */
        public ResourceLocation skin() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() == null || kind != ChatKind.PLAYER) {
                return null;
            }
            PlayerInfo info = sender != null ? minecraft.getConnection().getPlayerInfo(sender)
                    : senderName != null ? minecraft.getConnection().getPlayerInfo(senderName) : null;
            return info == null ? null : info.getSkinLocation();
        }
    }

    private static Field findMessages() {
        for (Field field : ChatComponent.class.getDeclaredFields()) {
            if (field.getType() == List.class && field.getGenericType() instanceof ParameterizedType type
                    && type.getActualTypeArguments().length == 1 && type.getActualTypeArguments()[0] == GuiMessage.class) {
                field.setAccessible(true);
                return field;
            }
        }
        Fordix.LOGGER.warn("Fordix: vanilla chat history not found, the Voidframe chat stays empty");
        return null;
    }

    /** Once per client tick: picks up the lines vanilla received since last time. */
    @SuppressWarnings("unchecked")
    public static void tick(Minecraft minecraft) {
        if (ALL_MESSAGES == null || minecraft.player == null) {
            return;
        }
        try {
            List<GuiMessage> all = (List<GuiMessage>) ALL_MESSAGES.get(minecraft.gui.getChat());
            if (all.isEmpty()) {
                if (lastTop != null) {
                    ENTRIES.clear();
                    UNREAD.clear();
                    lastTop = null;
                }
                return;
            }
            GuiMessage top = all.get(0);
            if (top == lastTop) {
                return;
            }
            int fresh = 0;
            while (fresh < all.size()) {
                GuiMessage candidate = all.get(fresh);
                if (candidate == lastTop || lastTop != null && candidate.addedTime() < lastTop.addedTime()) {
                    break;
                }
                fresh++;
            }
            for (int i = fresh - 1; i >= 0; i--) {
                add(minecraft, all.get(i).content());
            }
            lastTop = top;
        } catch (IllegalAccessException | RuntimeException e) {
            Fordix.LOGGER.warn("Fordix: could not read the chat history: {}", e.toString());
        }
    }

    private static void add(Minecraft minecraft, Component content) {
        String key = content.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
        ChatKind kind = ChatKind.classify(key, content.getString());
        Component name = null;
        Component body = content;
        UUID sender = null;
        String senderName = null;
        if (kind == ChatKind.PLAYER) {
            Object[] args = content.getContents() instanceof TranslatableContents translatable ? translatable.getArgs() : null;
            if (args != null && "chat.type.emote".equals(key)) {
                // "* Name does something": no name column, the line reads as is
            } else if (args != null && args.length >= 2) {
                // Plain-text arguments (a /tellraw, some servers) arrive as strings, vanilla's own as components.
                name = asComponent(args[args.length - 2]);
                body = asComponent(args[args.length - 1]);
            } else if (args == null) {
                String[] parts = ChatKind.splitPlain(content.getString());
                if (parts != null) {
                    name = Component.literal(parts[0]);
                    body = Component.literal(parts[1]);
                }
            }
            if (name != null) {
                sender = senderOf(name);
                senderName = name.getString();
            }
        }
        boolean mention = false;
        if (kind == ChatKind.PLAYER && name != null && minecraft.player != null) {
            String me = minecraft.player.getGameProfile().getName().toLowerCase(Locale.ROOT);
            mention = !me.equalsIgnoreCase(senderName) && body.getString().toLowerCase(Locale.ROOT).contains(me);
        }
        boolean whisper = ChatKind.isIncomingPrivate(key, content.getString());
        Entry entry = new Entry(kind, body, name, sender, senderName, mention || whisper);
        ENTRIES.add(entry);
        if ((mention || whisper) && FordixConfig.CHAT_MENTION_SOUND.get()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.get(), 1.4F, 0.7F));
        }
        if (ENTRIES.size() > MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        if (!ChatPanel.isReading(kind)) {
            for (ChatTab tab : ChatTab.values()) {
                if (tab != ChatTab.ALL && tab.matches(kind)) {
                    UNREAD.merge(tab, 1, Integer::sum);
                }
            }
        }
    }

    private static Component asComponent(Object argument) {
        return argument instanceof Component component ? component : Component.literal(String.valueOf(argument));
    }

    /** The player's id carried by the click and hover of a name the server sent (vanilla names hover as an entity). */
    private static UUID senderOf(Component name) {
        HoverEvent hover = name.getStyle().getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            HoverEvent.EntityTooltipInfo info = hover.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (info != null) {
                return info.id;
            }
        }
        for (Component sibling : name.getSiblings()) {
            UUID found = senderOf(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Oldest first. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<Entry> filtered(ChatTab tab) {
        return filtered(tab, "");
    }

    /** The lines a tab lists that also contain every word of the search. */
    public static List<Entry> filtered(ChatTab tab, String query) {
        if (tab == ChatTab.ALL && (query == null || query.isBlank())) {
            return ENTRIES;
        }
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (tab.matches(entry.kind) && ChatSearch.matches(entry.plainText(), query)) {
                result.add(entry);
            }
        }
        return result;
    }

    public static int unread(ChatTab tab) {
        return UNREAD.getOrDefault(tab, 0);
    }

    public static void markRead(ChatTab tab) {
        if (tab == ChatTab.ALL) {
            UNREAD.clear();
        } else {
            UNREAD.remove(tab);
        }
    }

    public static void clear() {
        ENTRIES.clear();
        UNREAD.clear();
        lastTop = null;
    }
}

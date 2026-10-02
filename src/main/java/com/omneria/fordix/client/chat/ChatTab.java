package com.omneria.fordix.client.chat;

/** The tabs above the chat: which kinds of line each one lists. Pure Java, unit tested. */
public enum ChatTab {
    ALL("all"),
    PLAYERS("players"),
    SERVER("server"),
    SYSTEM("system");

    private final String id;

    ChatTab(String id) {
        this.id = id;
    }

    /** Suffix of the "chat.tab.*" translation key. */
    public String id() {
        return id;
    }

    public boolean matches(ChatKind kind) {
        return switch (this) {
            case ALL -> true;
            case PLAYERS -> kind == ChatKind.PLAYER;
            case SERVER -> kind == ChatKind.SERVER;
            case SYSTEM -> kind == ChatKind.SYSTEM || kind == ChatKind.COMMAND;
        };
    }
}

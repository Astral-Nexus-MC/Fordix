package com.omneria.fordix.notification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.omneria.fordix.guide.GuidePage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the admin screen keeps on the server between restarts: notifications scheduled for later
 * (once, or repeating) and named notification templates. Pure Java apart from Gson, unit tested;
 * the server side loads and saves it as JSON (see NotificationScheduler).
 */
public final class AdminStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_SCHEDULES = 50;
    public static final int MAX_TEMPLATES = 50;
    public static final int MAX_PAGES = 30;
    public static final int MAX_BOOK_STEPS = 20;

    /** A notification to send at {@code nextAt} (epoch ms); repeats every {@code everyMs} when it is above 0. */
    public static final class Scheduled {
        public int id;
        public String style = "info";
        public int seconds = 5;
        public String title = "";
        public String message = "";
        public String kind = "tous";
        public String value = "";
        public long nextAt;
        public long everyMs;
        public String createdBy = "";

        /** The delay shown for this schedule: the repeat period, or the time left before its single run; at least 1. */
        public long minutes(long now) {
            long millis = everyMs > 0 ? everyMs : Math.max(0, nextAt - now);
            return Math.max(1, Math.round(millis / 60000.0));
        }
    }

    /** A saved notification form. */
    public static final class Template {
        public String style = "info";
        public int seconds = 5;
        public String title = "";
        public String message = "";
    }

    public List<Scheduled> schedules = new ArrayList<>();
    public Map<String, Template> templates = new LinkedHashMap<>();
    public int nextId = 1;
    /** Guide pages shown to every player, by id. */
    public Map<String, GuidePage> pages = new LinkedHashMap<>();
    /** Welcome book steps set from the admin screen (config-format lines); null keeps the server config's steps. */
    public List<String> welcomeSteps;
    /** Welcome book reward command set from the admin screen; null keeps the server config's. */
    public String welcomeReward;

    /** Adds a schedule; returns it, or null when the list is full. */
    public Scheduled schedule(String style, int seconds, String title, String message, String kind, String value,
                              long firstAt, long everyMs, String createdBy) {
        if (schedules.size() >= MAX_SCHEDULES) {
            return null;
        }
        Scheduled entry = new Scheduled();
        entry.id = nextId++;
        entry.style = style;
        entry.seconds = seconds;
        entry.title = title;
        entry.message = message;
        entry.kind = kind;
        entry.value = value;
        entry.nextAt = firstAt;
        entry.everyMs = everyMs;
        entry.createdBy = createdBy;
        schedules.add(entry);
        return entry;
    }

    public boolean deleteSchedule(int id) {
        return schedules.removeIf(entry -> entry.id == id);
    }

    /**
     * The schedules due at {@code now}. A one-shot one is removed; a repeating one moves to its
     * next slot after now (missed slots, say after a long stop, are skipped rather than replayed).
     */
    public List<Scheduled> due(long now) {
        List<Scheduled> fired = new ArrayList<>();
        for (Iterator<Scheduled> it = schedules.iterator(); it.hasNext(); ) {
            Scheduled entry = it.next();
            if (entry.nextAt > now) {
                continue;
            }
            fired.add(entry);
            if (entry.everyMs > 0) {
                long missed = (now - entry.nextAt) / entry.everyMs;
                entry.nextAt += (missed + 1) * entry.everyMs;
            } else {
                it.remove();
            }
        }
        return fired;
    }

    /** Saves a template under a name; returns false when the store is full of other names. */
    public boolean saveTemplate(String name, String style, int seconds, String title, String message) {
        if (!templates.containsKey(name) && templates.size() >= MAX_TEMPLATES) {
            return false;
        }
        Template template = new Template();
        template.style = style;
        template.seconds = seconds;
        template.title = title;
        template.message = message;
        templates.put(name, template);
        return true;
    }

    public boolean deleteTemplate(String name) {
        return templates.remove(name) != null;
    }

    /** Saves (or replaces) a guide page; false when the store is full of other pages. */
    public boolean savePage(String id, String title, String body, String author, long now) {
        if (!pages.containsKey(id) && pages.size() >= MAX_PAGES) {
            return false;
        }
        pages.put(id, new GuidePage(id, title, body, author, now));
        return true;
    }

    public boolean deletePage(String id) {
        return pages.remove(id) != null;
    }

    /** Replaces the welcome book's steps and reward; false when there are too many steps. */
    public boolean setWelcomeBook(List<String> steps, String reward) {
        if (steps.size() > MAX_BOOK_STEPS) {
            return false;
        }
        welcomeSteps = new ArrayList<>(steps);
        welcomeReward = reward;
        return true;
    }

    /** Goes back to the server config's steps and reward. */
    public void resetWelcomeBook() {
        welcomeSteps = null;
        welcomeReward = null;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /** Parses saved JSON; anything unreadable gives an empty store. */
    public static AdminStore fromJson(String json) {
        try {
            AdminStore store = GSON.fromJson(json, AdminStore.class);
            if (store == null) {
                return new AdminStore();
            }
            if (store.schedules == null) {
                store.schedules = new ArrayList<>();
            }
            if (store.templates == null) {
                store.templates = new LinkedHashMap<>();
            }
            if (store.pages == null) {
                store.pages = new LinkedHashMap<>();
            }
            for (Scheduled entry : store.schedules) {
                store.nextId = Math.max(store.nextId, entry.id + 1);
            }
            return store;
        } catch (RuntimeException e) {
            return new AdminStore();
        }
    }
}

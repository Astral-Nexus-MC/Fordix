package com.omneria.fordix.wiki;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Translates the pages of a wiki into another language with the Claude API ({@code POST /v1/messages}, plain HTTP like the rest
 * of the wiki code: the mod ships as one jar). Each page is cut into lines of the guide markup; headings and bullets keep their
 * marker, pictures are left alone, and the answer must be a JSON array with exactly as many lines as were sent, or the chunk is
 * retried once and then kept in its source language. The translated pages go into another wiki under the same names, so the
 * client's own lookups work unchanged.
 */
public final class WikiTranslator {
    public record Result(int translated, int copied, int failed, long inputTokens, long outputTokens, List<String> failures) {
    }

    /** Dollars per million tokens of the default model (claude-opus-5-5), for the estimate only. */
    private static final double INPUT_PRICE = 4.0;
    private static final double OUTPUT_PRICE = 20.0;
    private static final int MAX_CHUNK_CHARS = 5000;
    private static final int MAX_CHUNK_LINES = 80;
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fordix-wiki-translate");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private final String apiKey;
    private final String model;
    private final String languageName;
    private long inputTokens;
    private long outputTokens;

    private WikiTranslator(String apiKey, String model, String languageName) {
        this.apiKey = apiKey;
        this.model = model;
        this.languageName = languageName;
    }

    /** The pages that still need a translation, and what translating them would roughly cost in dollars. */
    public record Estimate(int pages, long characters, double dollars) {
    }

    public static Estimate estimate(WikiStore store, String sourceWiki, String targetWiki) {
        int pages = 0;
        long characters = 0;
        for (WikiStore.Entry entry : store.index(sourceWiki)) {
            WikiPage page = store.page(sourceWiki, entry.slug());
            if (page == null || !page.redirectTo.isEmpty() || page.body.isBlank() || store.page(targetWiki, entry.slug()) != null) {
                continue;
            }
            pages++;
            characters += page.body.length() + page.title.length();
        }
        // About 3.5 characters a token; the French text is a little longer than the English; a small fixed prompt per chunk.
        double inTokens = characters / 3.5 + pages * 400.0;
        double outTokens = characters / 3.5 * 1.25;
        return new Estimate(pages, characters, (inTokens * INPUT_PRICE + outTokens * OUTPUT_PRICE) / 1_000_000.0);
    }

    /**
     * Translates every page of {@code sourceWiki} the target wiki does not have yet (so a stopped run picks up where it was).
     * The target wiki must exist. Reports every 10 pages.
     */
    public static CompletableFuture<Result> translateWiki(WikiStore store, String sourceWiki, String targetWiki, String lang,
                                                          String apiKey, String model, Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new WikiTranslator(apiKey, model, languageName(lang)).run(store, sourceWiki, targetWiki, progress);
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }, WORKER);
    }

    static String languageName(String lang) {
        java.util.Locale locale = java.util.Locale.forLanguageTag(lang);
        String name = locale.getDisplayLanguage(java.util.Locale.ENGLISH);
        return name.isEmpty() ? lang : name;
    }

    private Result run(WikiStore store, String sourceWiki, String targetWiki, Consumer<String> progress) throws IOException {
        List<WikiStore.Entry> entries = store.index(sourceWiki);
        List<String> failures = new ArrayList<>();
        int translated = 0;
        int copied = 0;
        int failed = 0;
        int seen = 0;
        for (WikiStore.Entry entry : entries) {
            if (store.page(targetWiki, entry.slug()) != null) {
                continue;
            }
            WikiPage page = store.page(sourceWiki, entry.slug());
            if (page == null) {
                continue;
            }
            WikiPage out = new WikiPage(page.slug, page.title, page.body, page.source, System.currentTimeMillis());
            out.links = new ArrayList<>(page.links);
            out.redirectTo = page.redirectTo;
            if (page.redirectTo.isEmpty() && !page.body.isBlank()) {
                try {
                    translatePage(page, out);
                    translated++;
                } catch (AuthException e) {
                    throw e;
                } catch (IOException e) {
                    // Not saved: a later run tries this page again.
                    failed++;
                    if (failures.size() < 20) {
                        failures.add(page.title + " : " + e.getMessage());
                    }
                    continue;
                }
            } else {
                copied++;
            }
            store.save(targetWiki, out);
            if (++seen % 10 == 0) {
                progress.accept(seen + "/" + entries.size());
            }
        }
        return new Result(translated, copied, failed, inputTokens, outputTokens, failures);
    }

    /** Fills {@code out} with the translated title and body of {@code page}. */
    private void translatePage(WikiPage page, WikiPage out) throws IOException {
        String[] lines = page.body.split("\n", -1);
        List<Integer> at = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        at.add(-1);
        texts.add(page.title.replace('_', ' '));
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isBlank() && !lines[i].startsWith("@image")) {
                at.add(i);
                texts.add(lines[i]);
            }
        }
        List<String> result = new ArrayList<>();
        int from = 0;
        while (from < texts.size()) {
            int to = from;
            int chars = 0;
            while (to < texts.size() && to - from < MAX_CHUNK_LINES && (to == from || chars + texts.get(to).length() <= MAX_CHUNK_CHARS)) {
                chars += texts.get(to).length();
                to++;
            }
            result.addAll(translateChunk(texts.subList(from, to)));
            from = to;
        }
        if (!result.get(0).isBlank()) {
            out.title = result.get(0).strip();
        }
        for (int i = 1; i < result.size(); i++) {
            lines[at.get(i)] = result.get(i);
        }
        out.body = String.join("\n", lines);
    }

    private List<String> translateChunk(List<String> chunk) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                List<String> answer = ask(chunk);
                if (answer.size() == chunk.size()) {
                    return answer;
                }
                last = new IOException("the answer had " + answer.size() + " lines instead of " + chunk.size());
            } catch (AuthException e) {
                throw e;
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }

    private List<String> ask(List<String> chunk) throws IOException {
        JsonArray lines = new JsonArray();
        chunk.forEach(lines::add);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", 16000);
        JsonObject output = new JsonObject();
        output.addProperty("effort", "low");
        body.add("output_config", output);
        body.addProperty("system", "You translate pages of a Minecraft wiki into " + languageName + ". The user sends a JSON array of "
                + "strings, each one line of a page; the first string is the page title. Answer with ONLY a JSON array of the same length, "
                + "string for string, in the same order. Rules: keep a leading \"# \", \"## \" or \"- \" marker unchanged and translate "
                + "what follows; keep numbers, units, \" | \" separators and \" : \" separators in place; translate the cells of a row. "
                + "Use the official " + languageName + " Minecraft names for vanilla blocks, items, mobs, biomes and mechanics. Keep the "
                + "proper names the mod itself adds (creatures, bosses, items, dimensions) in their original form, unless the text "
                + "obviously describes them. Never add, merge, drop or comment on lines. No code fence, no text outside the array.");
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", lines.toString());
        messages.add(message);
        body.add("messages", messages);
        JsonObject answer = post(body.toString());
        if ("refusal".equals(string(answer, "stop_reason")) || "max_tokens".equals(string(answer, "stop_reason"))) {
            throw new IOException("stopped: " + string(answer, "stop_reason"));
        }
        if (answer.has("usage") && answer.get("usage").isJsonObject()) {
            JsonObject usage = answer.getAsJsonObject("usage");
            inputTokens += usage.has("input_tokens") ? usage.get("input_tokens").getAsLong() : 0;
            outputTokens += usage.has("output_tokens") ? usage.get("output_tokens").getAsLong() : 0;
        }
        StringBuilder text = new StringBuilder();
        if (answer.has("content") && answer.get("content").isJsonArray()) {
            for (JsonElement block : answer.getAsJsonArray("content")) {
                if (block.isJsonObject() && "text".equals(string(block.getAsJsonObject(), "type"))) {
                    text.append(string(block.getAsJsonObject(), "text"));
                }
            }
        }
        return parseLines(text.toString());
    }

    /** The strings of a JSON array answer; a code fence around it is ignored. Public for tests. */
    public static List<String> parseLines(String answer) throws IOException {
        String text = answer.strip();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < start) {
            throw new IOException("the answer is not a JSON array");
        }
        try {
            List<String> lines = new ArrayList<>();
            for (JsonElement element : JsonParser.parseString(text.substring(start, end + 1)).getAsJsonArray()) {
                lines.add(element.getAsString());
            }
            return lines;
        } catch (RuntimeException e) {
            throw new IOException("the answer is not a JSON array of strings");
        }
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    /** A wrong or missing key: no point in trying the next page. */
    private static final class AuthException extends IOException {
        AuthException(String message) {
            super(message);
        }
    }

    private JsonObject post(String json) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofMinutes(5))
                        .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01")
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status == 401 || status == 403) {
                    throw new AuthException("HTTP " + status + " (check the API key)");
                }
                if ((status == 429 || status == 529 || status >= 500) && attempt < 3) {
                    Thread.sleep(2000L << attempt);
                    continue;
                }
                if (status != 200) {
                    throw new IOException("HTTP " + status + " " + response.body().substring(0, Math.min(200, response.body().length())));
                }
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IOException("unreadable answer");
            }
        }
    }
}

package com.example.examplemod.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimeConfigLoader {
    private RuntimeConfigLoader() {
    }

    public static RuntimeConfig load(Logger logger) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("runtime.json");
        return load(path, logger);
    }

    static RuntimeConfig load(Path path, Logger logger) {
        if (!Files.exists(path)) {
            logger.warn("runtime.json not found at {}. Using defaults.", path);
            return RuntimeConfig.defaults();
        }

        try {
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            validateRoot(root);
            return parseRuntimeConfig(root);
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Failed to load runtime.json", e);
        }
    }

    private static void validateRoot(JsonObject root) {
        requireObject(root, "connection");
        requireObject(root, "conversation");
        requireObject(root, "llm");
        requireObject(root, "action");
        requireObject(root, "safety");
    }

    private static RuntimeConfig parseRuntimeConfig(JsonObject root) {
        Connection connection = parseConnection(root.getAsJsonObject("connection"));
        Conversation conversation = parseConversation(root.getAsJsonObject("conversation"));
        Llm llm = parseLlm(root.getAsJsonObject("llm"));
        Action action = parseAction(root.getAsJsonObject("action"));
        Safety safety = parseSafety(root.getAsJsonObject("safety"));
        return new RuntimeConfig(connection, conversation, llm, action, safety);
    }

    private static Connection parseConnection(JsonObject object) {
        Retry retry = parseRetry(object.getAsJsonObject("retry"));
        return new Connection(
                getString(object, "mode", "offline"),
                getString(object, "endpoint", ""),
                getInt(object, "timeoutMs", 5000),
                retry
        );
    }

    private static Retry parseRetry(JsonObject object) {
        if (object == null) {
            return new Retry(3, 1000);
        }
        return new Retry(
                getInt(object, "maxAttempts", 3),
                getInt(object, "backoffMs", 1000)
        );
    }

    private static Conversation parseConversation(JsonObject object) {
        return new Conversation(
                getInt(object, "historyLimit", 20),
                getString(object, "systemPrompt", ""),
                getString(object, "locale", "ja-JP")
        );
    }

    private static Llm parseLlm(JsonObject object) {
        return new Llm(
                getString(object, "provider", "openai"),
                getString(object, "model", "gpt-4o-mini"),
                getDouble(object, "temperature", 0.7),
                getInt(object, "maxTokens", 1024)
        );
    }

    private static Action parseAction(JsonObject object) {
        return new Action(
                getBoolean(object, "enabled", true),
                getStringList(object, "allowedActions"),
                getInt(object, "cooldownMs", 500)
        );
    }

    private static Safety parseSafety(JsonObject object) {
        return new Safety(
                getBoolean(object, "allowUnsafe", false),
                getBoolean(object, "redactPii", true),
                getInt(object, "maxRequestsPerMinute", 60)
        );
    }

    private static void requireObject(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            throw new IllegalStateException("Missing required object: " + name);
        }
    }

    private static String getString(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsString();
    }

    private static int getInt(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsInt();
    }

    private static double getDouble(JsonObject object, String name, double fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsDouble();
    }

    private static boolean getBoolean(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static List<String> getStringList(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonArray()) {
            return Collections.emptyList();
        }
        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (JsonElement item : array) {
            if (item.isJsonPrimitive()) {
                values.add(item.getAsString());
            }
        }
        return values;
    }

    public record RuntimeConfig(Connection connection,
                                Conversation conversation,
                                Llm llm,
                                Action action,
                                Safety safety) {
        public static RuntimeConfig defaults() {
            return new RuntimeConfig(
                    new Connection("offline", "", 5000, new Retry(3, 1000)),
                    new Conversation(20, "", "ja-JP"),
                    new Llm("openai", "gpt-4o-mini", 0.7, 1024),
                    new Action(true, List.of(), 500),
                    new Safety(false, true, 60)
            );
        }
    }

    public record Connection(String mode, String endpoint, int timeoutMs, Retry retry) {
    }

    public record Retry(int maxAttempts, int backoffMs) {
    }

    public record Conversation(int historyLimit, String systemPrompt, String locale) {
    }

    public record Llm(String provider, String model, double temperature, int maxTokens) {
    }

    public record Action(boolean enabled, List<String> allowedActions, int cooldownMs) {
    }

    public record Safety(boolean allowUnsafe, boolean redactPii, int maxRequestsPerMinute) {
    }
}

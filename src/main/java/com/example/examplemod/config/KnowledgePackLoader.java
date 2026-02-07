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

public final class KnowledgePackLoader {
    private KnowledgePackLoader() {
    }

    public static KnowledgePack load(Logger logger) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("knowledge.vanilla.json");
        return load(path, logger);
    }

    static KnowledgePack load(Path path, Logger logger) {
        if (!Files.exists(path)) {
            logger.warn("knowledge.vanilla.json not found at {}. Loading empty pack.", path);
            return KnowledgePack.empty("vanilla");
        }

        try {
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            validateRoot(root);
            return parsePack(root, logger);
        } catch (IOException | JsonParseException e) {
            logger.error("Failed to load knowledge.vanilla.json. Disabling knowledge pack.", e);
            return KnowledgePack.empty("vanilla");
        }
    }

    private static void validateRoot(JsonObject root) {
        requireObject(root, "pack");
        requireArray(root, "entries");
        JsonObject pack = root.getAsJsonObject("pack");
        requireString(pack, "id");
        requireString(pack, "version");
    }

    private static KnowledgePack parsePack(JsonObject root, Logger logger) {
        JsonObject packObject = root.getAsJsonObject("pack");
        PackMetadata metadata = new PackMetadata(
                getString(packObject, "id", "vanilla"),
                getString(packObject, "name", "Vanilla Knowledge"),
                getString(packObject, "version", "0.0.0"),
                getString(packObject, "description", "")
        );

        List<KnowledgeEntry> entries = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("entries");
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                logger.warn("Skipping non-object knowledge entry.");
                continue;
            }
            entries.add(parseEntry(element.getAsJsonObject()));
        }

        return new KnowledgePack(metadata, entries);
    }

    private static KnowledgeEntry parseEntry(JsonObject object) {
        return new KnowledgeEntry(
                getString(object, "id", ""),
                getString(object, "title", ""),
                getStringList(object, "tags"),
                getString(object, "content", ""),
                getString(object, "source", ""),
                getBoolean(object, "enabled", true)
        );
    }

    private static void requireObject(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            throw new IllegalStateException("Missing required object: " + name);
        }
    }

    private static void requireArray(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonArray()) {
            throw new IllegalStateException("Missing required array: " + name);
        }
    }

    private static void requireString(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonPrimitive()) {
            throw new IllegalStateException("Missing required string: " + name);
        }
    }

    private static String getString(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsString();
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

    public record KnowledgePack(PackMetadata pack, List<KnowledgeEntry> entries) {
        public static KnowledgePack empty(String id) {
            return new KnowledgePack(
                    new PackMetadata(id, "", "0.0.0", ""),
                    List.of()
            );
        }
    }

    public record PackMetadata(String id, String name, String version, String description) {
    }

    public record KnowledgeEntry(String id, String title, List<String> tags, String content, String source, boolean enabled) {
    }
}

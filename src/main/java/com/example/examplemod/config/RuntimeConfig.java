package com.example.examplemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record RuntimeConfig(ReconnectConfig reconnect, String logFile) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "runtime.json";

    public static RuntimeConfig load(Path configDir, Logger logger) {
        Path configPath = configDir.resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            copyDefault(configPath, logger);
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configPath), StandardCharsets.UTF_8)) {
            RuntimeConfig config = GSON.fromJson(reader, RuntimeConfig.class);
            if (config == null || config.reconnect == null) {
                return defaultConfig();
            }
            return config;
        } catch (IOException | JsonSyntaxException exception) {
            logger.warn("Failed to read runtime.json; using defaults.", exception);
            return defaultConfig();
        }
    }

    public Path resolveLogFile(Path configDir) {
        if (logFile == null || logFile.isBlank()) {
            return null;
        }
        return configDir.resolve(logFile);
    }

    private static void copyDefault(Path target, Logger logger) {
        try (InputStream stream = RuntimeConfig.class.getResourceAsStream("/runtime.json")) {
            if (stream == null) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            logger.warn("Failed to write default runtime.json.", exception);
        }
    }

    private static RuntimeConfig defaultConfig() {
        return new RuntimeConfig(new ReconnectConfig(5, 10), "bot-runtime.log");
    }

    public record ReconnectConfig(int maxAttempts, int intervalSeconds) {
    }
}

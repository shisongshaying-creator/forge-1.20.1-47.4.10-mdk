package com.example.examplemod.runtime;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class BotLogger {
    private final Logger logger;
    private final Path logFile;

    public BotLogger(Logger logger, Path logFile) {
        this.logger = logger;
        this.logFile = logFile;
    }

    public void logTransition(BotState state) {
        String message = String.format("[%s] Transitioned to %s", Instant.now(), state.status());
        logger.info(message);
        appendToFile(message);
    }

    public void logTick(BotState state) {
        String message = String.format("[%s] Tick %d decision=%s", Instant.now(), state.tickCount(), state.lastDecision());
        logger.info(message);
        appendToFile(message);
    }

    public void logReconnectScheduled(int attempt, long delaySeconds) {
        String message = String.format("[%s] Reconnect attempt %d scheduled in %d seconds", Instant.now(), attempt, delaySeconds);
        logger.warn(message);
        appendToFile(message);
    }

    public void logReconnectFailed(int attempts, int maxAttempts) {
        String message = String.format("[%s] Reconnect stopped after %d/%d attempts", Instant.now(), attempts, maxAttempts);
        logger.error(message);
        appendToFile(message);
    }

    private void appendToFile(String message) {
        if (logFile == null) {
            return;
        }
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, message + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.warn("Failed to write bot runtime log file.", exception);
        }
    }
}

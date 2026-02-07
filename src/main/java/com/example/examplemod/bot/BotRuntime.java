package com.example.examplemod.bot;

import com.example.examplemod.config.RuntimeConfig;
import com.example.examplemod.runtime.BotLogger;
import com.example.examplemod.runtime.BotState;
import com.example.examplemod.runtime.BotStatus;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BotRuntime {
    private static final Duration TICK_INTERVAL = Duration.ofSeconds(2);

    private final RuntimeConfig runtimeConfig;
    private final Decision decision;
    private final BotLogger botLogger;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger reconnectAttempts;
    private final Logger logger;

    private volatile BotState state;
    private volatile ScheduledFuture<?> tickFuture;

    public BotRuntime(RuntimeConfig runtimeConfig, Decision decision, Logger logger, Path configDir) {
        this.runtimeConfig = Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        this.decision = Objects.requireNonNull(decision, "decision");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.botLogger = new BotLogger(logger, runtimeConfig.resolveLogFile(configDir));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bot-runtime");
            thread.setDaemon(true);
            return thread;
        });
        this.reconnectAttempts = new AtomicInteger(0);
        this.state = new BotState(BotStatus.DISCONNECTED, Instant.now());
    }

    public synchronized void start() {
        if (tickFuture != null && !tickFuture.isCancelled()) {
            return;
        }
        connect();
        scheduleTicks();
    }

    public synchronized void stop() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        scheduler.shutdownNow();
        transitionTo(BotStatus.DISCONNECTED);
    }

    private void scheduleTicks() {
        tickFuture = scheduler.scheduleAtFixedRate(this::tickSafely, 0L, TICK_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception exception) {
            handleDisconnect(exception);
        }
    }

    private void tick() {
        if (state.status() != BotStatus.CONNECTED) {
            return;
        }
        String result = decision.decide(state);
        state = state.withDecision(result).incrementTick();
        botLogger.logTick(state);
    }

    private void connect() {
        transitionTo(BotStatus.CONNECTING);
        transitionTo(BotStatus.CONNECTED);
        reconnectAttempts.set(0);
    }

    private void handleDisconnect(Exception exception) {
        logger.error("Bot runtime tick failed; disconnecting.", exception);
        transitionTo(BotStatus.DISCONNECTED);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        int attempt = reconnectAttempts.incrementAndGet();
        int maxAttempts = runtimeConfig.reconnect().maxAttempts();
        if (attempt > maxAttempts) {
            botLogger.logReconnectFailed(attempt - 1, maxAttempts);
            return;
        }
        long delaySeconds = runtimeConfig.reconnect().intervalSeconds();
        botLogger.logReconnectScheduled(attempt, delaySeconds);
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private void transitionTo(BotStatus nextStatus) {
        state = state.transitionTo(nextStatus, Instant.now());
        botLogger.logTransition(state);
    }
}

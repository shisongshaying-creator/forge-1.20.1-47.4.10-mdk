package com.example.examplemod.runtime;

import java.time.Instant;

public record BotState(BotStatus status, Instant lastTransition, long tickCount, String lastDecision) {
    public BotState(BotStatus status, Instant lastTransition) {
        this(status, lastTransition, 0L, "");
    }

    public BotState transitionTo(BotStatus nextStatus, Instant transitionTime) {
        return new BotState(nextStatus, transitionTime, tickCount, lastDecision);
    }

    public BotState incrementTick() {
        return new BotState(status, lastTransition, tickCount + 1, lastDecision);
    }

    public BotState withDecision(String decision) {
        return new BotState(status, lastTransition, tickCount, decision);
    }
}

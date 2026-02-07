package com.example.examplemod.bot;

import com.example.examplemod.runtime.BotState;

public class DefaultDecision implements Decision {
    @Override
    public String decide(BotState state) {
        return "tick-" + state.tickCount();
    }
}

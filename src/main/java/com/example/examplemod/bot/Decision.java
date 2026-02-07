package com.example.examplemod.bot;

import com.example.examplemod.runtime.BotState;

public interface Decision {
    String decide(BotState state);
}

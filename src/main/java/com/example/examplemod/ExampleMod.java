package com.example.examplemod;

import com.example.examplemod.bot.BotRuntime;
import com.example.examplemod.bot.DefaultDecision;
import com.example.examplemod.config.RuntimeConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(ExampleMod.MOD_ID)
public class ExampleMod {
    public static final String MOD_ID = "examplemod";
    private static final Logger LOGGER = LogManager.getLogger();

    private final BotRuntime botRuntime;

    public ExampleMod() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        RuntimeConfig runtimeConfig = RuntimeConfig.load(configDir, LOGGER);
        this.botRuntime = new BotRuntime(runtimeConfig, new DefaultDecision(), LOGGER, configDir);
        this.botRuntime.start();

        Runtime.getRuntime().addShutdownHook(new Thread(botRuntime::stop));
    }
}

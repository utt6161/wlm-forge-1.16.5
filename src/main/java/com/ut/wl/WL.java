package com.ut.wl;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.server.MinecraftServer;
// The value here should match an entry in the META-INF/mods.toml file
@Mod(WL.MODID)
public class WL
{
    static final String MODID = "wl";
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public WL() {
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () ->{
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG);
//        Config.WlDbConfig.loadConfig(Config.WlDbConfig.config, FMLPaths.CONFIGDIR.get().resolve("wl-server.toml").toString());
            MinecraftForge.EVENT_BUS.register(WLCommands.class);
            MinecraftForge.EVENT_BUS.register(this);
        });
//        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

    }

    int tick = 0;
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event){
        if(Config.toggle) {
            tick++;
        } else {
            tick = 0;
            return;
        }
        if(tick >= Config.whitelistreloadtime){
            Updater.getInstance().run();
            tick = 0;
        }
    }

//    @SubscribeEvent
//    public static void onRegisterCommandsEvent(RegisterCommandsEvent event){
//        LOGGER.info("DA-WHITELIST: Registering command for full restart");
//        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
//        ReloadAndRestart.registerCommand(dispatcher);
//
//    }

//    @SubscribeEvent
//    public static void onRegisterCommands(final RegisterCommandsEvent event) {
//
//        Reload.registerCommand(event.getDispatcher());
//    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartedEvent event){
        Updater.getInstance().StartSyncing();
    }
}

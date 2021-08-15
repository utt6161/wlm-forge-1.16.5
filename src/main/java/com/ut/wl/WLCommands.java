package com.ut.wl;

import com.mojang.brigadier.CommandDispatcher;
import io.socket.client.Url;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.ut.wl.WL.LOGGER;

public class WLCommands {

    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event){
        LOGGER.info("DA-WHITELIST: Registering commands for WL");
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        UrlCommand.registerCommand(dispatcher);
        UserCommand.registerCommand(dispatcher);
        PasswordCommand.registerCommand(dispatcher);
        DbCredCommand.registerCommand(dispatcher);
        WhitelistStatusCommand.registerCommand(dispatcher);
        MonthlyPaymentCommand.registerCommand(dispatcher);
        DATokenCommand.registerCommand(dispatcher);
        RestartCommand.registerCommand(dispatcher);
        ReloadCommand.registerCommand(dispatcher);
        ToggleReloadingCommand.registerCommand(dispatcher);
        UserAndPasswordCommand.registerCommand(dispatcher);
        AddUserCommand.registerCommand(dispatcher);
        DeleteUserCommand.registerCommand(dispatcher);
        infoCommand.registerCommand(dispatcher);
        LOGGER.info("DA-WHITELIST: Completed");
    }
}

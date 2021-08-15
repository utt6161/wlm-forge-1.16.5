package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static com.ut.wl.WL.LOGGER;

public class ReloadCommand implements Command<CommandSource> {
//public class ReloadAndRestart{
    private static final ReloadCommand CMD = new ReloadCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> reloadCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("reload")
                        .executes(CMD));

        dispatcher.register(reloadCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Updater.getInstance().reload_from_db();
        return 0;
    }


}

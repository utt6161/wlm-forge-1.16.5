package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;

import static com.ut.wl.WL.LOGGER;

public class WhitelistStatusCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final WhitelistStatusCommand CMD = new WhitelistStatusCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> whitelistStatus = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("status")
                        .executes(CMD));

        dispatcher.register(whitelistStatus);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        LOGGER.info(Config.status);
        return 0;
    }


}

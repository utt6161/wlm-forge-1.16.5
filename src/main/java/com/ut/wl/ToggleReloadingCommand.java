package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;

public class ToggleReloadingCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final ToggleReloadingCommand CMD = new ToggleReloadingCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> toggleCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("toggle")
                        .executes(CMD));

        dispatcher.register(toggleCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Config.toggleReload();
        return 0;
    }


}

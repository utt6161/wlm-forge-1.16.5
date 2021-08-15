package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

import java.awt.*;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class ReloadTimeCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final ReloadTimeCommand CMD = new ReloadTimeCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> reloadTimeCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("reloadtime")
                        .then(Commands.argument("reloadTime", word())
                                .executes(CMD)));

        dispatcher.register(reloadTimeCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        try {
            Config.changeReloadTime(getString(context, "reloadTime"));
        } catch(NumberFormatException e){
            throw new SimpleCommandExceptionType(new StringTextComponent("Reload time doesnt seem to be a number")).create();
        }
        return 0;
    }


}

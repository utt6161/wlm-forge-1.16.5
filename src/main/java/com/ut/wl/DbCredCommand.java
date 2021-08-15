package com.ut.wl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class DbCredCommand implements Command<CommandSource> {
    private static final DbCredCommand CMD = new DbCredCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> dbCredCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("dbcred")
                        .then(Commands.argument("url",word())
                                .then(Commands.argument("user",word())
                                        .then(Commands.argument("password", word())
                                .executes(CMD)))));

        dispatcher.register(dbCredCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Config.changeDbCred(getString(context, "url"), getString(context, "user"), getString(context, "password") );
        return 0;
    }
}

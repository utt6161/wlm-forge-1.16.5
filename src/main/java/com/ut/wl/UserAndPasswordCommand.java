package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class UserAndPasswordCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final UserAndPasswordCommand CMD = new UserAndPasswordCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> userAndPasswordCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("lognpass")
                        .then(Commands.argument("user",word())
                                .then(Commands.argument("password", word())
                                        .executes(CMD))));

        dispatcher.register(userAndPasswordCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Config.changeUserAndPassword(getString(context, "user"),getString(context,"password"));
        return 0;
    }


}
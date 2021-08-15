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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.ut.wl.WL.LOGGER;

public class AddUserCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final AddUserCommand CMD = new AddUserCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> addUserCommand = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("add")
                        .then(Commands.argument("user",word())
                                .then(Commands.argument("months", word())
                                        .executes(CMD))));

        dispatcher.register(addUserCommand);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Updater.getInstance().addPlayerFromCommand(getString(context, "user"), getString(context, "months"));
        return 0;
    }


}

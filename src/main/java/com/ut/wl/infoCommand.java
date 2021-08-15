package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;

import static com.ut.wl.WL.LOGGER;

public class infoCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final infoCommand CMD = new infoCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> whitelistStatus = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("info")
                        .executes(CMD));

        dispatcher.register(whitelistStatus);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        LOGGER.info("ALL COMMANDS: ");
        LOGGER.info("'/wl restart' - full mod restart");
        LOGGER.info("'/wl reload' - get new data from config");
        LOGGER.info("'/wl toggle' - off/on whitelist timer");
        LOGGER.info("'/wl add <username> <months>' - add new player, if months are negative, then i'll try to subtract them from existing player");
        LOGGER.info("'/wl delete <username>' - remove player from database and whitelist");
        LOGGER.info("'/wl url <database_url>' - change config field 'url'");
        LOGGER.info("'/wl user <database_user>' - change config field 'user'");
        LOGGER.info("'/wl password <database_user_password>' - change config field 'password'");
        LOGGER.info("'/wl lognpass <user> <pass>' - change pair of user and password");
        LOGGER.info("'/wl dbcred <url> <user> <pass>' - change all data for database");
        LOGGER.info("'/wl money <money>' - change config field 'money'");
        LOGGER.info("'/wl reloadtime <time_in_ticks>' - change config field 'whitelistreloadtime'");
        LOGGER.info("'/wl datoken <da_token>' - change config field 'token'");
        LOGGER.info("'/wl status' - change config field 'user'");
        LOGGER.info("'/wl info' - the command that you just used :^)");

        return 0;
    }

}
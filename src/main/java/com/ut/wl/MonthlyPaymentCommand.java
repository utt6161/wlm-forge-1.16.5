package com.ut.wl;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class MonthlyPaymentCommand implements Command<CommandSource> {
    //public class ReloadAndRestart{
    private static final MonthlyPaymentCommand CMD = new MonthlyPaymentCommand();

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> monthlyPayment = Commands.literal("wl")
                .requires(source -> source.hasPermissionLevel(4))
                .then(Commands.literal("money")
                        .then(Commands.argument("monthlyPayment",word())
                                .executes(CMD)));

        dispatcher.register(monthlyPayment);
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        try {
            Config.changeMonthlyPayment(getString(context, "monthlyPayment"));
        } catch(NumberFormatException e){
            throw new SimpleCommandExceptionType(new StringTextComponent("Monthly payment doesnt seem to be a number")).create();
        }
        return 0;
    }


}

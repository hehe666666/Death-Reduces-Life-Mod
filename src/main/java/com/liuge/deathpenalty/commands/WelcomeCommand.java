package com.liuge.deathpenalty.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class WelcomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deathpenalty")
                .then(Commands.literal("welcome")
                        .then(Commands.literal("confirm").executes(WelcomeCommand::confirmWelcome))
                        .then(Commands.literal("dontshowagain").executes(WelcomeCommand::dontShowAgain))
                )
        );
    }

    private static int confirmWelcome(CommandContext<CommandSourceStack> context) {
        // 什么都不做，只是确认
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a欢迎使用死亡惩罚模组，祝您游戏愉快！"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int dontShowAgain(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            // 设置玩家不再显示欢迎消息
            var rootTag = player.getPersistentData();
            var persistedTag = rootTag.getCompound("PlayerPersisted");
            persistedTag.putBoolean("deathpenalty_dont_show_again", true);
            rootTag.put("PlayerPersisted", persistedTag);

            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已设置不再显示欢迎消息"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
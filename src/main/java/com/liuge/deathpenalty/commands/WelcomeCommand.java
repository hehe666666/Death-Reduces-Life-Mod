package com.liuge.deathpenalty.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.liuge.deathpenalty.DeathPenaltyCore;
import com.liuge.deathpenalty.VersionSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WelcomeCommand {
    private WelcomeCommand() {
    }

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
            player.sendSystemMessage(Component.literal("§a欢迎使用死亡惩罚模组，祝您游戏愉快！"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int dontShowAgain(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // 设置玩家不再显示欢迎消息
            CompoundTag rootTag = player.getPersistentData();
            CompoundTag persistedTag = VersionSupport.getCompound(rootTag, DeathPenaltyCore.PERSISTED_KEY);
            persistedTag.putBoolean(DeathPenaltyCore.DONT_SHOW_AGAIN_KEY, true);
            rootTag.put(DeathPenaltyCore.PERSISTED_KEY, persistedTag);

            player.sendSystemMessage(Component.literal("§a已设置不再显示欢迎消息"));
        }
        return Command.SINGLE_SUCCESS;
    }
}

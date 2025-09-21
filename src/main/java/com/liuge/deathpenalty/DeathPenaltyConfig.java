package com.liuge.deathpenalty;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class DeathPenaltyConfig {
    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<String> welcomeMessage;
        public final ForgeConfigSpec.BooleanValue showWelcomeMessage;
        public final ForgeConfigSpec.ConfigValue<String> welcomeMessageTitle;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Death Penalty Mod Configuration")
                    .push("General");

            welcomeMessageTitle = builder
                    .comment("Title for the welcome message")
                    .define("welcomeMessageTitle", "温馨提示");

            welcomeMessage = builder
                    .comment("Welcome message shown to players when they first join")
                    .define("welcomeMessage",
                            "§6欢迎使用死亡惩罚模组！\n" +
                                    "§a- 每次死亡会减少2点最大生命值\n" +
                                    "§c- 当生命值降至0时，你的存档将被删除\n" +
                                    "§e- 请谨慎游戏，珍惜生命！");

            showWelcomeMessage = builder
                    .comment("Whether to show the welcome message to new players")
                    .define("showWelcomeMessage", true);

            builder.pop();
        }
    }
}
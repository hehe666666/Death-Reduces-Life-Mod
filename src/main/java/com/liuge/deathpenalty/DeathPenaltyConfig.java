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
        public final ForgeConfigSpec.BooleanValue deleteSave;
        public final ForgeConfigSpec.DoubleValue deathPenalty;
        public final ForgeConfigSpec.DoubleValue minHealthKeep;
        public final ForgeConfigSpec.IntValue deleteDelayTicks;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("死亡惩罚模组配置 / Death Reduces Life configuration")
                    .push("general");

            deleteSave = builder
                    .comment("当最大生命值降至 0 时是否删除存档",
                            "Whether to delete the save when max health reaches 0",
                            "true: 生命值耗尽时删除存档; false: 最低保留 minHealthKeep 点生命值")
                    .define("deleteSave", true);

            deathPenalty = builder
                    .comment("每次死亡减少的最大生命值（1 点 = 半颗心）",
                            "Max health lost per death (1 point = half a heart)")
                    .defineInRange("deathPenalty", 2.0D, 0.0D, 40.0D);

            minHealthKeep = builder
                    .comment("deleteSave=false 时最大生命值的下限",
                            "Minimum max health kept when deleteSave=false")
                    .defineInRange("minHealthKeep", 1.0D, 1.0D, 20.0D);

            deleteDelayTicks = builder
                    .comment("生命值耗尽后延迟多少 tick 再删除存档",
                            "Ticks to wait before deleting the world")
                    .defineInRange("deleteDelayTicks", 20, 0, 200);

            builder.pop();
        }
    }
}

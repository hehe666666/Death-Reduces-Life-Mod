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

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            deleteSave = builder.define("deleteSave", true);
        }
    }
}
package com.liuge.deathpenalty;

import com.liuge.deathpenalty.commands.WelcomeCommand;
import com.liuge.deathpenalty.events.PlayerDeathListener;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(DeathPenaltyMod.MOD_ID)
public class DeathPenaltyMod {
    public static final String MOD_ID = "deathpenalty";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DeathPenaltyMod() {
        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DeathPenaltyConfig.COMMON_SPEC, "deathreduceslife.toml");

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PlayerDeathListener.class);

        LOGGER.info("DeathPenaltyMod 已加载，事件监听器已注册");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WelcomeCommand.register(event.getDispatcher());
    }
}
package com.liuge.deathpenalty;

import com.liuge.deathpenalty.events.PlayerDeathListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DeathPenaltyMod.MOD_ID)
public class DeathPenaltyMod {
    public static final String MOD_ID = "deathpenalty";

    public DeathPenaltyMod() {
        // 消除FMLJavaModLoadingContext.get()的过时警告
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(PlayerDeathListener.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 初始化设置
    }
}
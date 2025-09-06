package com.liuge.deathpenalty.events;

import com.liuge.deathpenalty.DeathPenaltyMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathPenaltyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDeathListener {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!player.level().isClientSide) {
                double currentMaxHealth = player.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                double newMaxHealth = Math.max(2.0, currentMaxHealth - 2.0);

                // 保存最大生命值到玩家数据
                CompoundTag playerData = player.getPersistentData();
                playerData.putDouble("deathpenalty:max_health", newMaxHealth);

                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
                if (player.getHealth() > newMaxHealth) {
                    player.setHealth((float) newMaxHealth);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        // 兼容旧版Java语法，不使用模式匹配
        if (event.getOriginal() instanceof Player && event.getEntity() instanceof Player) {
            Player originalPlayer = (Player) event.getOriginal();
            Player newPlayer = (Player) event.getEntity();

            if (!newPlayer.level().isClientSide) {
                CompoundTag originalData = originalPlayer.getPersistentData();
                if (originalData.contains("deathpenalty:max_health")) {
                    double savedMaxHealth = originalData.getDouble("deathpenalty:max_health");
                    newPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(savedMaxHealth);
                    newPlayer.setHealth((float) savedMaxHealth);
                }
            }
        }
    }
}
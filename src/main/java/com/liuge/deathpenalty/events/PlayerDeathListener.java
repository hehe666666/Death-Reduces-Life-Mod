package com.liuge.deathpenalty.events;

import com.liuge.deathpenalty.DeathPenaltyConfig;
import com.liuge.deathpenalty.DeathPenaltyMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Mod.EventBusSubscriber(modid = DeathPenaltyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDeathListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeathPenaltyMod.MOD_ID);
    private static final String PERSISTED_KEY = "PlayerPersisted";
    private static final String MAX_HEALTH_NBT_KEY = "deathpenalty_max_health";
    private static final String DONT_SHOW_AGAIN_KEY = "deathpenalty_dont_show_again";
    private static boolean shouldDeleteWorld = false;
    private static Path worldToDelete = null;
    private static int deleteCountdown = -1;
    private static MinecraftServer serverInstance = null;
    private static boolean isServerStopping = false;

    // 添加玩家登录事件监听
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("[欢迎] 玩家登录: " + player.getName().getString());

            // 获取玩家持久化数据
            CompoundTag rootTag = player.getPersistentData();
            CompoundTag persistedTag = rootTag.getCompound(PERSISTED_KEY);

            // 检查玩家是否选择不再显示欢迎消息
            boolean dontShowAgain = persistedTag.contains(DONT_SHOW_AGAIN_KEY) &&
                    persistedTag.getBoolean(DONT_SHOW_AGAIN_KEY);

            LOGGER.info("[欢迎] 玩家是否选择不再显示: " + dontShowAgain);
            LOGGER.info("[欢迎] 配置是否显示欢迎消息: " + DeathPenaltyConfig.COMMON.showWelcomeMessage.get());

            if (DeathPenaltyConfig.COMMON.showWelcomeMessage.get() && !dontShowAgain) {
                // 发送欢迎消息和可点击选项
                String title = DeathPenaltyConfig.COMMON.welcomeMessageTitle.get();
                String message = DeathPenaltyConfig.COMMON.welcomeMessage.get();

                player.sendSystemMessage(Component.literal("§6§l" + title));

                // 分割多行消息
                String[] lines = message.split("\\\\n");
                for (String line : lines) {
                    player.sendSystemMessage(Component.literal(line));
                }

                // 添加可点击的选项
                player.sendSystemMessage(Component.literal("§a[确定] §7- 点击继续游戏")
                        .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/deathpenalty welcome confirm"
                        ))));

                player.sendSystemMessage(Component.literal("§c[下次不再提示] §7- 点击并不再显示此消息")
                        .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/deathpenalty welcome dontshowagain"
                        ))));

                LOGGER.info("[欢迎] 已向玩家 " + player.getName().getString() + " 显示欢迎信息和选项");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 确保事件只处理一次，避免多次触发
            if (event.isCanceled()) return;

            // 检查是否已经计划删除世界
            if (shouldDeleteWorld) {
                LOGGER.info("[死亡] 世界删除已计划，跳过处理");
                return;
            }

            CompoundTag rootTag = player.getPersistentData();
            CompoundTag persistedTag = rootTag.getCompound(PERSISTED_KEY);
            LOGGER.info("[死亡] 初始持久化NBT: " + persistedTag);

            double currentMaxHealth;
            if (persistedTag.contains(MAX_HEALTH_NBT_KEY)) {
                currentMaxHealth = persistedTag.getDouble(MAX_HEALTH_NBT_KEY);
                LOGGER.info("[死亡] 从持久化NBT读取到生命值: " + currentMaxHealth);
            } else {
                currentMaxHealth = player.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                LOGGER.info("[死亡] 首次死亡，初始生命值: " + currentMaxHealth);
            }

            // 死亡惩罚：每次减少2点生命值
            double newMaxHealth = Math.max(currentMaxHealth - 2.0D, 0.0D);
            LOGGER.info("[死亡] 计算后新生命值: " + newMaxHealth);

            // 保存新的最大生命值到持久化NBT
            persistedTag.putDouble(MAX_HEALTH_NBT_KEY, newMaxHealth);
            rootTag.put(PERSISTED_KEY, persistedTag);
            LOGGER.info("[死亡] 写入后持久化NBT: " + persistedTag);

            // 更新当前最大生命值
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);

            // 最大生命值为0时：踢出并标记删除整个存档
            if (newMaxHealth <= 0) {
                String playerName = player.getName().getString();
                MinecraftServer server = player.getServer();

                // 存储服务器实例供后续使用
                serverInstance = server;

                // 踢出玩家
                if (server != null && server.isSingleplayer()) {
                    player.connection.disconnect(Component.literal("最大生命值已耗尽，世界将被删除"));
                } else {
                    player.connection.disconnect(Component.literal("最大生命值已耗尽，存档已删除"));
                }

                LOGGER.info("[死亡] 玩家 " + playerName + " 生命值耗尽，准备删除存档");

                if (server != null) {
                    // 获取世界路径
                    Path worldDirectory = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                    shouldDeleteWorld = true;
                    worldToDelete = worldDirectory;
                    deleteCountdown = 20; // 减少延迟 tick数 (约1秒)

                    LOGGER.info("[删档] 世界删除已计划，路径: " + worldDirectory);

                    // 单机模式下，我们计划在服务器tick中延迟删除
                    if (server.isSingleplayer()) {
                        LOGGER.info("[删档] 单机模式，使用延迟删除");
                    } else {
                        // 专用服务器可以直接处理
                        server.execute(() -> {
                            deleteWorldDirectory(worldDirectory);
                            server.stopServer();
                        });
                    }
                }
            } else {
                player.sendSystemMessage(Component.literal("死亡惩罚：最大生命值变为 " + newMaxHealth));
            }
        }
    }

    // 服务器每tick事件
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shouldDeleteWorld && worldToDelete != null && !isServerStopping) {
            if (deleteCountdown > 0) {
                deleteCountdown--;
                LOGGER.info("[删档] 倒计时: " + deleteCountdown);
            } else if (deleteCountdown == 0) {
                LOGGER.info("[删档] 执行延迟世界删除");
                deleteWorldDirectory(worldToDelete);
                shouldDeleteWorld = false;
                worldToDelete = null;
                deleteCountdown = -1;

                // 单机模式下，关闭游戏
                if (serverInstance != null && serverInstance.isSingleplayer()) {
                    isServerStopping = true;
                    serverInstance.stopServer();
                }

                // 重置服务器实例
                serverInstance = null;
            }
        }
    }

    // 服务器停止事件
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // 如果服务器停止时还有待删除的世界，立即删除
        if (shouldDeleteWorld && worldToDelete != null) {
            LOGGER.info("[删档] 服务器已停止，立即删除世界目录");
            deleteWorldDirectory(worldToDelete);
            shouldDeleteWorld = false;
            worldToDelete = null;
            deleteCountdown = -1;
        }
        isServerStopping = false;
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag rootTag = player.getPersistentData();
            CompoundTag persistedTag = rootTag.getCompound(PERSISTED_KEY);
            LOGGER.info("[重生] 读取到的持久化NBT: " + persistedTag);

            if (persistedTag.contains(MAX_HEALTH_NBT_KEY)) {
                double savedHealth = persistedTag.getDouble(MAX_HEALTH_NBT_KEY);
                LOGGER.info("[重生] 从持久化NBT恢复生命值: " + savedHealth);
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(savedHealth);
                player.sendSystemMessage(Component.literal("重生后最大生命值：" + savedHealth));
            } else {
                LOGGER.error("[重生] 未找到持久化NBT数据！键：" + MAX_HEALTH_NBT_KEY);
                // 这里可以设置一个默认值，比如20.0D
                double defaultHealth = 20.0D;
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(defaultHealth);
                player.sendSystemMessage(Component.literal("警告：未找到生命值数据，重置为默认值" + defaultHealth));
            }
        }
    }

    // 实际执行世界目录删除
    private static void deleteWorldDirectory(Path worldDirectory) {
        try {
            if (Files.exists(worldDirectory) && Files.isDirectory(worldDirectory)) {
                LOGGER.info("[删档] 开始删除世界存档: " + worldDirectory.toAbsolutePath());

                // 使用更可靠的方法删除整个目录
                deleteDirectoryRecursively(worldDirectory);

                LOGGER.info("[删档] 世界存档已成功删除");
            } else {
                LOGGER.error("[删档] 世界存档路径不存在: " + worldDirectory.toAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("[删档] 删除世界存档失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 使用NIO API递归删除目录（更可靠的方法）
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                    LOGGER.debug("[删档] 已删除文件: " + file);
                } catch (IOException e) {
                    LOGGER.warn("无法删除文件: " + file + ", 错误: " + e.getMessage());
                    // 继续删除其他文件
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                    LOGGER.debug("[删档] 已删除目录: " + dir);
                } catch (IOException e) {
                    LOGGER.warn("无法删除目录: " + dir + ", 错误: " + e.getMessage());
                    // 继续删除其他目录
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}//666这个bug终于修好了,666修好又出新bug了


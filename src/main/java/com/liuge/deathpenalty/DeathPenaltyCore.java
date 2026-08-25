package com.liuge.deathpenalty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Shared, version-agnostic core logic. Only depends on vanilla Minecraft classes and the
 * Forge config spec, so it can be compiled against every supported Minecraft version.
 */
public final class DeathPenaltyCore {
    private static final Logger LOGGER = LoggerFactory.getLogger("deathpenalty");

    public static final String PERSISTED_KEY = "PlayerPersisted";
    public static final String MAX_HEALTH_NBT_KEY = "deathpenalty_max_health";
    public static final String DONT_SHOW_AGAIN_KEY = "deathpenalty_dont_show_again";

    public static final double DEFAULT_MAX_HEALTH = 20.0D;

    private static boolean shouldDeleteWorld = false;
    private static Path worldToDelete = null;

    private DeathPenaltyCore() {
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (shouldDeleteWorld) {
            LOGGER.info("[死亡] 世界删除已计划，跳过处理");
            return;
        }

        CompoundTag rootTag = player.getPersistentData();
        CompoundTag persistedTag = VersionSupport.getCompound(rootTag, PERSISTED_KEY);

        double currentMaxHealth = persistedTag.contains(MAX_HEALTH_NBT_KEY)
                ? VersionSupport.getDouble(persistedTag, MAX_HEALTH_NBT_KEY)
                : currentMaxHealth(player);

        boolean deleteSave = DeathPenaltyConfig.COMMON.deleteSave.get();
        double penalty = DeathPenaltyConfig.COMMON.deathPenalty.get();
        double floor = deleteSave ? 0.0D : DeathPenaltyConfig.COMMON.minHealthKeep.get();
        double newMaxHealth = Math.max(currentMaxHealth - penalty, floor);

        persistedTag.putDouble(MAX_HEALTH_NBT_KEY, newMaxHealth);
        rootTag.put(PERSISTED_KEY, persistedTag);
        setMaxHealth(player, newMaxHealth);
        LOGGER.info("[死亡] 玩家 {} 最大生命值 {} -> {}", player.getName().getString(), currentMaxHealth, newMaxHealth);

        if (newMaxHealth <= 0.0D && deleteSave) {
            handleWorldDeletion(player);
        } else {
            player.sendSystemMessage(Component.literal("死亡惩罚：最大生命值变为 " + newMaxHealth));
            if (newMaxHealth <= DeathPenaltyConfig.COMMON.minHealthKeep.get() && !deleteSave) {
                player.sendSystemMessage(Component.literal("§c警告：你的最大生命值已降至最低！"));
                LOGGER.info("[死亡] 玩家 {} 生命值已降至最低，配置禁用了删除存档功能", player.getName().getString());
            }
        }
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        CompoundTag rootTag = player.getPersistentData();
        CompoundTag persistedTag = VersionSupport.getCompound(rootTag, PERSISTED_KEY);

        double savedHealth;
        if (persistedTag.contains(MAX_HEALTH_NBT_KEY)) {
            savedHealth = VersionSupport.getDouble(persistedTag, MAX_HEALTH_NBT_KEY);
            LOGGER.info("[重生] 玩家 {} 恢复最大生命值: {}", player.getName().getString(), savedHealth);
        } else {
            // 玩家从未死亡过（首次加入世界）：数据缺失是正常情况，写回默认值，避免每次重生都报错
            savedHealth = DEFAULT_MAX_HEALTH;
            persistedTag.putDouble(MAX_HEALTH_NBT_KEY, savedHealth);
            rootTag.put(PERSISTED_KEY, persistedTag);
            LOGGER.debug("[重生] 玩家 {} 没有生命值数据，已初始化默认值 {}", player.getName().getString(), savedHealth);
        }

        setMaxHealth(player, savedHealth);
        player.sendSystemMessage(Component.literal("重生后最大生命值：" + savedHealth));
    }

    public static void onServerStopped() {
        if (shouldDeleteWorld && worldToDelete != null) {
            LOGGER.info("[删档] 服务器已停止，立即删除世界目录");
            deleteWorldDirectory(worldToDelete);
            shouldDeleteWorld = false;
            worldToDelete = null;
        }
    }

    private static void handleWorldDeletion(ServerPlayer player) {
        MinecraftServer server = VersionSupport.serverOf(player);
        if (server == null) {
            LOGGER.error("[删档] 无法获取服务器实例，跳过存档删除");
            return;
        }

        player.connection.disconnect(Component.literal(
                server.isSingleplayer() ? "最大生命值已耗尽，世界将被删除" : "最大生命值已耗尽，存档已删除"));

        Path worldDirectory = server.getWorldPath(LevelResource.ROOT);
        shouldDeleteWorld = true;
        worldToDelete = worldDirectory;
        LOGGER.info("[删档] 玩家 {} 生命值耗尽，世界删除已计划: {}", player.getName().getString(), worldDirectory.toAbsolutePath());

        if (server.isSingleplayer()) {
            scheduleWorldDeletion(server, worldDirectory, DeathPenaltyConfig.COMMON.deleteDelayTicks.get());
        } else {
            VersionSupport.schedule(server, () -> {
                deleteWorldDirectory(worldDirectory);
                shouldDeleteWorld = false;
                worldToDelete = null;
                stopServerSafely(server);
            });
        }
    }

    private static void scheduleWorldDeletion(MinecraftServer server, Path worldDirectory, int ticksLeft) {
        if (ticksLeft <= 0) {
            LOGGER.info("[删档] 执行延迟世界删除");
            deleteWorldDirectory(worldDirectory);
            shouldDeleteWorld = false;
            worldToDelete = null;
            if (server.isSingleplayer()) {
                stopServerSafely(server);
            }
        } else {
            LOGGER.debug("[删档] 倒计时: {}", ticksLeft);
            VersionSupport.schedule(server, () -> scheduleWorldDeletion(server, worldDirectory, ticksLeft - 1));
        }
    }

    private static void setMaxHealth(ServerPlayer player, double value) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            LOGGER.warn("[死亡惩罚] 玩家 {} 缺少 MAX_HEALTH 属性，无法设置最大生命值 {}", player.getName().getString(), value);
            return;
        }
        attribute.setBaseValue(value);
    }

    private static double currentMaxHealth(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        return attribute != null ? attribute.getBaseValue() : DEFAULT_MAX_HEALTH;
    }

    // ========== 新增：安全停止服务器 ==========
    private static void stopServerSafely(MinecraftServer server) {
        try {
            // 使用反射调用 protected 方法 stopServer()
            java.lang.reflect.Method method = MinecraftServer.class.getDeclaredMethod("stopServer");
            method.setAccessible(true);
            method.invoke(server);
            LOGGER.info("[服务] 服务器已停止");
        } catch (Exception e) {
            // 反射失败时，尝试通过命令停止（兼容旧版本或异常情况）
            LOGGER.warn("[服务] 反射停止服务器失败，尝试命令方式", e);
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        "stop"
                );
            } catch (Exception ex) {
                LOGGER.error("[服务] 停止服务器失败", ex);
            }
        }
    }

    private static void deleteWorldDirectory(Path worldDirectory) {
        if (Files.exists(worldDirectory) && Files.isDirectory(worldDirectory)) {
            LOGGER.info("[删档] 开始删除世界存档: {}", worldDirectory.toAbsolutePath());
            try {
                deleteDirectoryRecursively(worldDirectory);
                LOGGER.info("[删档] 世界存档已成功删除");
            } catch (Exception e) {
                LOGGER.error("[删档] 删除世界存档失败: {}", e.getMessage(), e);
            }
        } else {
            LOGGER.error("[删档] 世界存档路径不存在: {}", worldDirectory.toAbsolutePath());
        }
    }

    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                    LOGGER.debug("[删档] 已删除文件: {}", file);
                } catch (IOException e) {
                    LOGGER.warn("无法删除文件: {}，错误: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                    LOGGER.debug("[删档] 已删除目录: {}", dir);
                } catch (IOException e) {
                    LOGGER.warn("无法删除目录: {}，错误: {}", dir, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
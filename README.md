# Death Penalty Mod

一个Minecraft模组，为游戏添加死亡惩罚机制。

## 功能特性

- **死亡惩罚**：玩家每死亡一次，最大生命值减少 2 点
- **配置功能**：可配置是否在生命值耗尽时删除存档
- **灵活配置**：通过简单的配置文件控制模组行为

## 配置说明

模组首次启动时会在 `config` 目录生成 `deathreduceslife.toml` 文件，内容如下：

```toml
deleteSave = true （默认） 
```
或
```toml
deleteSave = false
```
- `deleteSave = true`：当生命值降至 0 时，删除存档
- `deleteSave = false`：当生命值降至 1 时停止减少，保留玩家生命

## 模组机制

- **初始状态**：玩家拥有默认最大生命值
- **死亡惩罚**：每次死亡减少 2 点最大生命值
- **生命值上限**：
  - 当 `deleteSave = true` 时，最低可降至 0（此时删除存档）
  - 当 `deleteSave = false` 时，最低保留 1 点（不会删除存档）

---

# Death Penalty Mod

A Minecraft mod that adds death penalty mechanics to the game.

## Features

- **Death Penalty**: Each death reduces player's maximum health by 2 points
- **Save Protection**: Configurable whether to delete save when health is exhausted
- **Flexible Configuration**: Control mod behavior through simple configuration file

## Configuration

When the mod starts for the first time, it will generate a `deathreduceslife.toml` file in the `config` directory with the following content:

```toml
deleteSave = true （Default） or
deleteSave = false
```

- `deleteSave = true`: Delete save when health reaches 0
- `deleteSave = false`: Stop reducing health when it reaches 1, keep player alive

## Mod Mechanism

- **Initial State**: Player has default maximum health
- **Death Penalty**: Each death reduces maximum health by 2 points
- **Health Limit**:
  - When `deleteSave = true`, minimum can drop to 0 (save deleted at this point)
  - When `deleteSave = false`, minimum keeps 1 point (save not deleted)




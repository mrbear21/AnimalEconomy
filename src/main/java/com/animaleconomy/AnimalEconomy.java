package com.animaleconomy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimalEconomy implements Listener {

    private final File dataFile;
    private FileConfiguration dataConfig;

    private static final Map<UUID, Map<EntityType, Integer>> playerKills = new HashMap<>();

    public AnimalEconomy() {
        dataFile = new File(Brain.getInstance().getDataFolder(), "db.yml");
        loadData();
    }

    private Map<UUID, Long> lastKillTime = new HashMap<>();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            UUID playerId = player.getUniqueId();
            EntityType entityType = event.getEntity().getType();

            long currentTime = System.currentTimeMillis();

            if (lastKillTime.containsKey(playerId)) {
                long lastTime = lastKillTime.get(playerId);
                long timeDifference = currentTime - lastTime;

                if (timeDifference < 1000) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Вбивство не зараховано через підозру на використання мобо-ферми!"));
                    return;
                }
            }

            lastKillTime.put(playerId, currentTime);

            playerKills.putIfAbsent(playerId, new HashMap<>());
            Map<EntityType, Integer> kills = playerKills.get(playerId);
            kills.put(entityType, kills.getOrDefault(entityType, 0) + 1);

            saveData();
        }
    }


    public int getKills(UUID playerId, EntityType entityType) {
        return playerKills.getOrDefault(playerId, new HashMap<>()).getOrDefault(entityType, 0);
    }

    public void setKills(UUID playerId, EntityType entityType, int count) {
        playerKills.putIfAbsent(playerId, new HashMap<>());
        playerKills.get(playerId).put(entityType, count);
        saveData();
    }

	public void addKills(UUID playerId, EntityType entityType, int count) {
        int currentKills = getKills(playerId, entityType);
        setKills(playerId, entityType, currentKills + count);
	}

	public void removeKills(UUID playerId, EntityType entityType, int count) {
        int currentKills = getKills(playerId, entityType);
        setKills(playerId, entityType, Math.max(0, currentKills - count));
	}
    
    public Map<EntityType, Integer> getAllKills(UUID playerId) {
        return playerKills.getOrDefault(playerId, new HashMap<>());
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String playerId : dataConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(playerId);
            Map<EntityType, Integer> kills = new HashMap<>();

            for (String entityType : dataConfig.getConfigurationSection(playerId).getKeys(false)) {
                EntityType type = EntityType.valueOf(entityType);
                int count = dataConfig.getInt(playerId + "." + entityType);
                kills.put(type, count);
            }

            playerKills.put(uuid, kills);
        }
    }

    private void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        for (UUID playerId : playerKills.keySet()) {
            for (EntityType entityType : playerKills.get(playerId).keySet()) {
                int count = playerKills.get(playerId).get(entityType);
                dataConfig.set(playerId.toString() + "." + entityType.name(), count);
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


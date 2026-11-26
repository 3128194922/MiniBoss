package com.rainbow.miniboss.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MiniBossData {
    public static final String MINI_BOSS_KEY = "isMiniBoss";
    public static final String POWER_KEY = "POWER";
    public static final String POWER_LEVELS_KEY = "POWER_LEVELS";
    public static final String ADAPT_TYPES_KEY = "_mb_adapt_types";
    public static final String REINFORCE_SPAWNED_KEY = "_mb_reinforce_spawned";
    public static final String PARASITE_SPAWNED_KEY = "_mb_parasite_spawned";
    public static final String POWER_PREFIX = "_mb_p_";
    public static final String LEVEL_PREFIX = "_mb_lvl_";

    private MiniBossData() {
    }

    public static CompoundTag tag(LivingEntity entity) {
        return entity.getPersistentData();
    }

    public static boolean isMiniBoss(LivingEntity entity) {
        return entity != null && tag(entity).getBoolean(MINI_BOSS_KEY);
    }

    public static boolean hasPower(LivingEntity entity, MiniBossPower power) {
        return isMiniBoss(entity) && tag(entity).getBoolean(POWER_PREFIX + power.id());
    }

    public static int getPowerLevel(LivingEntity entity, MiniBossPower power) {
        if (entity == null) return 1;
        int level = tag(entity).getInt(LEVEL_PREFIX + power.id());
        return level <= 0 ? 1 : Math.min(level, power.maxLevel());
    }

    public static List<MiniBossPower> getPowers(LivingEntity entity) {
        List<MiniBossPower> powers = new ArrayList<>();
        if (entity == null) return powers;

        String json = tag(entity).getString(POWER_KEY);
        if (!json.isEmpty()) {
            try {
                JsonElement parsed = JsonParser.parseString(json);
                if (parsed.isJsonArray()) {
                    JsonArray array = parsed.getAsJsonArray();
                    for (JsonElement element : array) {
                        MiniBossPower power = MiniBossPower.byId(element.getAsString());
                        if (power != null && !powers.contains(power)) powers.add(power);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }

        if (powers.isEmpty()) {
            for (MiniBossPower power : MiniBossPower.values()) {
                if (tag(entity).getBoolean(POWER_PREFIX + power.id())) powers.add(power);
            }
        }
        return powers;
    }

    public static Map<MiniBossPower, Integer> getPowerLevels(LivingEntity entity) {
        Map<MiniBossPower, Integer> levels = new EnumMap<>(MiniBossPower.class);
        if (entity == null) return levels;

        String json = tag(entity).getString(POWER_LEVELS_KEY);
        if (!json.isEmpty()) {
            try {
                JsonElement parsed = JsonParser.parseString(json);
                if (parsed.isJsonObject()) {
                    JsonObject object = parsed.getAsJsonObject();
                    for (String key : object.keySet()) {
                        MiniBossPower power = MiniBossPower.byId(key);
                        if (power != null) {
                            int level = object.get(key).getAsInt();
                            levels.put(power, Math.max(1, Math.min(level, power.maxLevel())));
                        }
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }

        for (MiniBossPower power : getPowers(entity)) {
            levels.putIfAbsent(power, getPowerLevel(entity, power));
        }
        return levels;
    }

    public static void writePowers(LivingEntity entity, List<MiniBossPower> powers, Map<MiniBossPower, Integer> levels) {
        CompoundTag tag = tag(entity);
        for (MiniBossPower power : MiniBossPower.values()) {
            tag.remove(POWER_PREFIX + power.id());
            tag.remove(LEVEL_PREFIX + power.id());
        }

        JsonArray array = new JsonArray();
        JsonObject levelObject = new JsonObject();
        for (MiniBossPower power : powers) {
            int level = levels.getOrDefault(power, 1);
            level = Math.max(1, Math.min(level, power.maxLevel()));
            array.add(power.id());
            levelObject.addProperty(power.id(), level);
            tag.putBoolean(POWER_PREFIX + power.id(), true);
            tag.putInt(LEVEL_PREFIX + power.id(), level);
        }

        tag.putBoolean(MINI_BOSS_KEY, true);
        tag.putString(POWER_KEY, array.toString());
        tag.putString(POWER_LEVELS_KEY, levelObject.toString());
    }

    public static void addPower(LivingEntity entity, MiniBossPower power, int level) {
        List<MiniBossPower> powers = getPowers(entity);
        if (!powers.contains(power)) powers.add(power);
        Map<MiniBossPower, Integer> levels = getPowerLevels(entity);
        levels.put(power, Math.max(1, Math.min(level, power.maxLevel())));
        writePowers(entity, powers, levels);
    }

    public static void removePower(LivingEntity entity, MiniBossPower power) {
        List<MiniBossPower> powers = getPowers(entity);
        powers.remove(power);
        Map<MiniBossPower, Integer> levels = getPowerLevels(entity);
        levels.remove(power);
        writePowers(entity, powers, levels);
    }

    public static void clearPowers(LivingEntity entity) {
        CompoundTag tag = tag(entity);
        tag.remove(MINI_BOSS_KEY);
        tag.remove(POWER_KEY);
        tag.remove(POWER_LEVELS_KEY);
        tag.remove(ADAPT_TYPES_KEY);
        for (MiniBossPower power : MiniBossPower.values()) {
            tag.remove(POWER_PREFIX + power.id());
            tag.remove(LEVEL_PREFIX + power.id());
        }
    }

    public static List<String> getAdaptTypes(LivingEntity entity) {
        List<String> types = new ArrayList<>();
        String json = tag(entity).getString(ADAPT_TYPES_KEY);
        if (json.isEmpty()) return types;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonArray()) {
                for (JsonElement element : parsed.getAsJsonArray()) types.add(element.getAsString());
            }
        } catch (RuntimeException ignored) {
        }
        return types;
    }

    public static void setAdaptTypes(LivingEntity entity, List<String> types) {
        JsonArray array = new JsonArray();
        for (String type : types) array.add(type);
        tag(entity).putString(ADAPT_TYPES_KEY, array.toString());
    }
}

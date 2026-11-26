package com.rainbow.miniboss.logic;

import com.rainbow.miniboss.MiniBoss;
import com.rainbow.miniboss.MiniBossConfig;
import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MiniBossConversion {
    private static final TagKey<EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("fieldguide", "bosses"));

    private MiniBossConversion() {
    }

    public static boolean convert(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) return false;
        if (entity.isDeadOrDying() || entity instanceof Player || entity instanceof TamableAnimal) return false;

        var tag = MiniBossData.tag(entity);
        if (tag.getBoolean(MiniBossData.MINI_BOSS_KEY)
                || tag.getBoolean(MiniBossData.REINFORCE_SPAWNED_KEY)
                || tag.getBoolean(MiniBossData.PARASITE_SPAWNED_KEY)) {
            return false;
        }
        if (entity.getType().is(BOSS_TAG)) return false;
        if (isEntityBlacklisted(entity)) return false;

        List<MiniBossPower> powers = rollPowers(entity);
        if (powers.isEmpty()) return false;

        Map<MiniBossPower, Integer> levels = new EnumMap<>(MiniBossPower.class);
        for (MiniBossPower power : powers) {
            levels.put(power, power.rollLevel(entity.getRandom()));
        }

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return false;
        double health = maxHealth.getValue();

        if (levels.containsKey(MiniBossPower.EVOLVE) && entity instanceof Slime slime) {
            slime.setSize(slime.getSize() + 1, true);
            health = maxHealth.getValue();
        }

        MiniBossData.writePowers(entity, powers, levels);
        maxHealth.setBaseValue(health * MiniBossConfig.healthMultiplier());
        entity.setHealth(entity.getMaxHealth());

        if (levels.containsKey(MiniBossPower.HEAVY_ARMOR)) {
            AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(armor.getBaseValue() + levels.get(MiniBossPower.HEAVY_ARMOR) * 5.0D);
            }
        }

        if (levels.containsKey(MiniBossPower.EVOLVE) && entity instanceof Skeleton) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 1, false, false));
        }

        if (MiniBossConfig.dyeingArmor()) applyDyeingArmor(entity);
        if (entity instanceof Mob mob) MiniBossGoals.attach(mob);

        if (MiniBossConfig.logConversions()) {
            MiniBoss.LOGGER.info("Converted {} with {}", entity.getType(),
                    powers.stream().map(power -> power.id() + ":" + levels.get(power)).toList());
        }
        return true;
    }

    private static List<MiniBossPower> rollPowers(LivingEntity entity) {
        List<MiniBossPower> available = new ArrayList<>();
        for (MiniBossPower power : MiniBossPower.values()) {
            if (!isPowerBlacklisted(entity, power)) available.add(power);
        }
        if (available.isEmpty()) return List.of();

        int draws = 1 + entity.getRandom().nextInt(Math.max(1, MiniBossConfig.maxDraws()));
        List<MiniBossPower> result = new ArrayList<>();
        for (int i = 0; i < draws; i++) {
            MiniBossPower power = available.get(entity.getRandom().nextInt(available.size()));
            if (!result.contains(power)) result.add(power);
        }
        return result;
    }

    private static boolean isEntityBlacklisted(LivingEntity entity) {
        String id = entityId(entity);
        if (id == null) return false;
        for (String pattern : MiniBossConfig.entityBlacklist()) {
            if (patternMatches(pattern, id)) return true;
        }
        return false;
    }

    private static boolean isPowerBlacklisted(LivingEntity entity, MiniBossPower power) {
        String id = entityId(entity);
        if (id == null) return false;
        for (String entry : MiniBossConfig.powerBlacklist()) {
            int equals = entry.indexOf('=');
            if (equals <= 0 || equals >= entry.length() - 1) continue;
            String pattern = entry.substring(0, equals).trim();
            if (!patternMatches(pattern, id)) continue;
            for (String value : entry.substring(equals + 1).split(",")) {
                if (value.trim().equalsIgnoreCase(power.id())) return true;
            }
        }
        return false;
    }

    private static String entityId(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key == null ? null : key.toString();
    }

    private static boolean patternMatches(String pattern, String id) {
        if (pattern == null || pattern.isEmpty()) return false;
        if (pattern.equals(id)) return true;
        return pattern.endsWith(":*") && id.startsWith(pattern.substring(0, pattern.length() - 1));
    }

    private static void applyDyeingArmor(LivingEntity entity) {
        if (!ModList.get().isLoaded("dyeing")) return;
        try {
            entity.getServer().getCommands().performPrefixedCommand(
                    entity.createCommandSourceStack().withSuppressedOutput(),
                    "dyeing uv add scroll mini_boss_creeper_armor " + entity.getUUID()
                            + " rainbow:textures/miniboss/miniboss_armor.png 0.01 0.01 1");
        } catch (RuntimeException exception) {
            MiniBoss.LOGGER.warn("Failed to apply Dyeing mini-boss armor to {}", entity.getUUID(), exception);
        }
    }
}

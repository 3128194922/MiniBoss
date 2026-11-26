package com.rainbow.miniboss;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class MiniBossConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST = BUILDER
            .comment("Entity id patterns that never become mini-bosses. Supports exact ids and namespace:* wildcards.")
            .defineListAllowEmpty("entityBlacklist", List.of(), MiniBossConfig::isString);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> POWER_BLACKLIST = BUILDER
            .comment("Power blacklist entries: 'entityPattern=Power1,Power2' or '*=Power'. Example: 'minecraft:creeper=Explode,Evolve'.")
            .defineListAllowEmpty("powerBlacklist", List.of(), MiniBossConfig::isString);

    private static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER = BUILDER
            .comment("Base max-health multiplier applied once at conversion.")
            .defineInRange("healthMultiplier", 3.0D, 1.0D, 20.0D);

    private static final ForgeConfigSpec.IntValue MAX_DRAWS = BUILDER
            .comment("Maximum number of power draws at conversion. Range 1-8; duplicate draws do not add a second power.")
            .defineInRange("maxDraws", 4, 1, 8);

    private static final ForgeConfigSpec.BooleanValue LOG_CONVERSIONS = BUILDER
            .comment("Log each successful conversion with entity id and rolled powers.")
            .define("logConversions", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MiniBossConfig() {
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    public static List<String> entityBlacklist() {
        return ENTITY_BLACKLIST.get().stream().map(String::valueOf).toList();
    }

    public static List<String> powerBlacklist() {
        return POWER_BLACKLIST.get().stream().map(String::valueOf).toList();
    }

    public static double healthMultiplier() {
        return HEALTH_MULTIPLIER.get();
    }

    public static int maxDraws() {
        return MAX_DRAWS.get();
    }

    public static boolean logConversions() {
        return LOG_CONVERSIONS.get();
    }
}

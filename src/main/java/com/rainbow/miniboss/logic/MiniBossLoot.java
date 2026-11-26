package com.rainbow.miniboss.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class MiniBossLoot {
    private record Drop(String id, int weight) {
    }

    private static final List<Drop> DROPS = List.of(
            new Drop("minecraft:diamond", 8),
            new Drop("minecraft:emerald", 8),
            new Drop("minecraft:gold_ingot", 10),
            new Drop("minecraft:iron_ingot", 10),
            new Drop("minecraft:experience_bottle", 6),
            new Drop("minecraft:ender_pearl", 5),
            new Drop("minecraft:obsidian", 4),
            new Drop("rainbow:super_mechanism", 3),
            new Drop("rainbow:miracle", 2),
            new Drop("minecraft:netherite_scrap", 3)
    );

    private MiniBossLoot() {
    }

    public static void addDrops(LivingDropsEvent event, LivingEntity entity) {
        int rolls = 1 + entity.getRandom().nextInt(3);
        for (int i = 0; i < rolls; i++) {
            Drop drop = pick(entity);
            if (drop == null) continue;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(drop.id()));
            if (item == null || item == Items.AIR) continue;
            ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5D,
                    entity.getZ(), new ItemStack(item));
            itemEntity.setDefaultPickUpDelay();
            event.getDrops().add(itemEntity);
        }
    }

    private static Drop pick(LivingEntity entity) {
        int total = 0;
        for (Drop drop : DROPS) total += drop.weight();
        if (total <= 0) return null;
        int roll = entity.getRandom().nextInt(total);
        for (Drop drop : DROPS) {
            roll -= drop.weight();
            if (roll < 0) return drop;
        }
        return DROPS.get(DROPS.size() - 1);
    }
}

package com.rainbow.miniboss.logic;

import com.rainbow.miniboss.MiniBoss;
import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MiniBoss.MODID)
public final class MiniBossEvents {
    private MiniBossEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        var source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        boolean changed = false;

        if (attacker != null && attacker != victim && attacker.isAlive()) {
            int attackerLevel = 0;

            if (MiniBossData.hasPower(attacker, MiniBossPower.SHADOW)) {
                amount += 15.0F - lightLevel(attacker);
                changed = true;
            }
            if (MiniBossData.hasPower(attacker, MiniBossPower.ASSASSIN)
                    && attacker.hasEffect(MobEffects.INVISIBILITY)) {
                amount += MiniBossData.getPowerLevel(attacker, MiniBossPower.ASSASSIN) * 2.0F;
                changed = true;
            }
            if (MiniBossData.hasPower(attacker, MiniBossPower.FURY)) {
                float ratio = attacker.getHealth() / Math.max(1.0F, attacker.getMaxHealth());
                amount *= 1.0F + (1.0F - Math.max(0.0F, Math.min(1.0F, ratio)));
                changed = true;
            }

            attackerLevel = MiniBossData.getPowerLevel(attacker, MiniBossPower.DISARM);
            if (MiniBossData.hasPower(attacker, MiniBossPower.DISARM)
                    && attacker.getRandom().nextFloat() < attackerLevel * 0.05F) {
                disarm(victim);
            }
            attackerLevel = MiniBossData.getPowerLevel(attacker, MiniBossPower.DISPEL);
            if (MiniBossData.hasPower(attacker, MiniBossPower.DISPEL)
                    && attacker.getRandom().nextFloat() < attackerLevel * 0.05F) {
                dispel(victim);
            }
            if (MiniBossData.hasPower(attacker, MiniBossPower.DRAIN)) {
                attacker.heal(amount * 0.5F);
            }
        }

        if (MiniBossData.isMiniBoss(victim)) {
            if (MiniBossData.hasPower(victim, MiniBossPower.RIGIDITY)) {
                if (amount < 15.0F) {
                    amount *= 0.2F;
                    changed = true;
                } else if (amount > 15.0F) {
                    amount *= 1.8F;
                    changed = true;
                }
            }

            if (recordAdaptDamage(victim, source.getMsgId())) {
                int cap = Math.min(3, MiniBossData.getPowerLevel(victim, MiniBossPower.ADAPT));
                amount *= 1.0F - cap * 0.25F;
                changed = true;
            }

            if (MiniBossData.hasPower(victim, MiniBossPower.PARASITE)) {
                parasite(victim, attacker);
            }
        }

        if (changed) event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!MiniBossData.isMiniBoss(entity) || !MiniBossData.hasPower(entity, MiniBossPower.EXPLODE)) return;
        int strength = MiniBossData.getPowerLevel(entity, MiniBossPower.EXPLODE) * 5;
        entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(), strength, false,
                Level.ExplosionInteraction.BLOCK);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob && MiniBossData.isMiniBoss(mob)) {
            MiniBossGoals.attach(mob);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MiniBossCommands.register(event.getDispatcher());
    }

    private static int lightLevel(LivingEntity entity) {
        var level = entity.level();
        var pos = entity.blockPosition();
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        int light = Math.max(block, sky);
        return Math.max(0, Math.min(15, light));
    }

    private static void disarm(LivingEntity victim) {
        if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        EquipmentSlot[] slots = {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        int dropped = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = victim.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            ItemEntity item = victim.spawnAtLocation(stack);
            if (item != null) item.setPickUpDelay(40);
            victim.setItemSlot(slot, ItemStack.EMPTY);
            dropped++;
        }
        if (dropped > 0) {
            victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    private static void dispel(LivingEntity victim) {
        List<MobEffect> toRemove = new ArrayList<>();
        for (MobEffectInstance instance : victim.getActiveEffects()) {
            if (instance.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                toRemove.add(instance.getEffect());
            }
        }
        for (MobEffect effect : toRemove) victim.removeEffect(effect);
        if (!toRemove.isEmpty()) {
            victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    private static void parasite(LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        var tag = MiniBossData.tag(victim);
        long now = level.getGameTime();
        if (now - tag.getLong("_mb_parasite_t") < 40L) return;
        tag.putLong("_mb_parasite_t", now);

        int count = MiniBossData.getPowerLevel(victim, MiniBossPower.PARASITE);
        for (int i = 0; i < count; i++) {
            Silverfish fish = EntityType.SILVERFISH.create(level);
            if (fish == null) continue;
            double ox = (victim.getRandom().nextDouble() - 0.5D) * 3.0D;
            double oz = (victim.getRandom().nextDouble() - 0.5D) * 3.0D;
            fish.moveTo(victim.getX() + ox, victim.getY(), victim.getZ() + oz,
                    victim.getRandom().nextFloat() * 360.0F, 0.0F);
            fish.finalizeSpawn(level, level.getCurrentDifficultyAt(fish.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            if (attacker != null && attacker.isAlive()) fish.setTarget(attacker);
            fish.getPersistentData().putBoolean(MiniBossData.PARASITE_SPAWNED_KEY, true);
            level.addFreshEntity(fish);
        }
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private static boolean recordAdaptDamage(LivingEntity victim, String damageType) {
        if (!MiniBossData.hasPower(victim, MiniBossPower.ADAPT) || damageType == null || damageType.isEmpty()) {
            return false;
        }
        int cap = Math.min(3, MiniBossData.getPowerLevel(victim, MiniBossPower.ADAPT));
        List<String> types = MiniBossData.getAdaptTypes(victim);
        if (types.size() > cap) {
            types = new ArrayList<>(types.subList(0, cap));
            MiniBossData.setAdaptTypes(victim, types);
        }
        if (types.contains(damageType)) return true;
        if (types.size() < cap) {
            types.add(damageType);
            MiniBossData.setAdaptTypes(victim, types);
        }
        return false;
    }
}

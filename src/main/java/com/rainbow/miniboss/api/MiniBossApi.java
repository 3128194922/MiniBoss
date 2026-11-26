package com.rainbow.miniboss.api;

import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.logic.MiniBossConversion;
import com.rainbow.miniboss.logic.MiniBossGoals;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public final class MiniBossApi {
    private MiniBossApi() {
    }

    public static boolean convert(Entity entity) {
        return entity instanceof LivingEntity living && MiniBossConversion.convert(living);
    }

    public static boolean isMiniBoss(Entity entity) {
        return entity instanceof LivingEntity living && MiniBossData.isMiniBoss(living);
    }

    public static boolean hasPower(Entity entity, String power) {
        if (!(entity instanceof LivingEntity living)) return false;
        MiniBossPower resolved = MiniBossPower.byId(power);
        return resolved != null && MiniBossData.hasPower(living, resolved);
    }

    public static int getPowerLevel(Entity entity, String power) {
        if (!(entity instanceof LivingEntity living)) return 0;
        MiniBossPower resolved = MiniBossPower.byId(power);
        return resolved == null ? 0 : MiniBossData.getPowerLevel(living, resolved);
    }

    public static List<String> getPowers(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return List.of();
        return MiniBossData.getPowers(living).stream().map(MiniBossPower::id).toList();
    }

    public static boolean addPower(Entity entity, String power, int level) {
        if (!(entity instanceof LivingEntity living)) return false;
        MiniBossPower resolved = MiniBossPower.byId(power);
        if (resolved == null) return false;
        MiniBossData.addPower(living, resolved, level);
        if (living instanceof Mob mob) MiniBossGoals.attach(mob);
        return true;
    }

    public static boolean removePower(Entity entity, String power) {
        if (!(entity instanceof LivingEntity living)) return false;
        MiniBossPower resolved = MiniBossPower.byId(power);
        if (resolved == null) return false;
        MiniBossData.removePower(living, resolved);
        return true;
    }

    public static void clear(Entity entity) {
        if (entity instanceof LivingEntity living) MiniBossData.clearPowers(living);
    }
}

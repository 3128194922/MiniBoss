package com.rainbow.miniboss.api;

import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.logic.MiniBossConversion;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
}

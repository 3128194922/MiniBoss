package com.rainbow.miniboss;

import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.logic.MiniBossConversion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

public final class MiniBossGameTests {
    private MiniBossGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void convertsZombie(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        helper.assertTrue(MiniBossConversion.convert(zombie), "zombie conversion failed");
        helper.assertTrue(MiniBossData.isMiniBoss(zombie), "missing isMiniBoss tag");
        helper.assertTrue(!MiniBossData.getPowers(zombie).isEmpty(), "no powers rolled");
        helper.assertTrue(zombie.getMaxHealth() >= 20.0F, "health was not applied");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void refusesDoubleConversion(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        helper.assertTrue(MiniBossConversion.convert(zombie), "first conversion failed");
        helper.assertTrue(!MiniBossConversion.convert(zombie), "second conversion must be refused");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void writesLegacyKubeJsKeys(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        helper.assertTrue(MiniBossConversion.convert(zombie), "conversion failed");
        helper.assertTrue(zombie.getPersistentData().getBoolean("isMiniBoss"), "isMiniBoss key missing");
        helper.assertTrue(!zombie.getPersistentData().getString("POWER").isEmpty(), "POWER json missing");
        helper.assertTrue(!zombie.getPersistentData().getString("POWER_LEVELS").isEmpty(), "POWER_LEVELS json missing");
        helper.succeed();
    }
}

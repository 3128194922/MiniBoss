package com.rainbow.miniboss;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MiniBoss.MODID)
public final class MiniBoss {
    public static final String MODID = "miniboss";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MiniBoss(FMLJavaModLoadingContext context) {
        GameTestRegistry.register(MiniBossGameTests.class);
        context.registerConfig(ModConfig.Type.COMMON, MiniBossConfig.SPEC);
        LOGGER.info("MiniBoss loaded");
    }
}

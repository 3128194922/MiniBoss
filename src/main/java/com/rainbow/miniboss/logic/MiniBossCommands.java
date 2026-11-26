package com.rainbow.miniboss.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class MiniBossCommands {
    private MiniBossCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("miniboss")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("convert")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> convert(context.getSource(),
                                        EntityArgument.getEntity(context, "target")))))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> info(context.getSource(),
                                        EntityArgument.getEntity(context, "target")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("power", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (MiniBossPower power : MiniBossPower.values()) builder.suggest(power.id());
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> add(context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                StringArgumentType.getString(context, "power"), 1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                                .executes(context -> add(context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        StringArgumentType.getString(context, "power"),
                                                        IntegerArgumentType.getInteger(context, "level")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("power", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (MiniBossPower power : MiniBossPower.values()) builder.suggest(power.id());
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> remove(context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                StringArgumentType.getString(context, "power"))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> clear(context.getSource(),
                                        EntityArgument.getEntity(context, "target"))))));
    }

    private static int convert(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("目标不是 LivingEntity"));
            return 0;
        }
        boolean result = MiniBossConversion.convert(living);
        source.sendSuccess(() -> Component.literal(result ? "已转化为迷你Boss" : "转化失败（已转化/黑名单/非法目标）"), false);
        return result ? 1 : 0;
    }

    private static int info(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living) || !MiniBossData.isMiniBoss(living)) {
            source.sendFailure(Component.literal("目标不是迷你Boss"));
            return 0;
        }
        var levels = MiniBossData.getPowerLevels(living);
        String text = MiniBossData.getPowers(living).stream()
                .map(power -> power.id() + ":" + levels.getOrDefault(power, 1))
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");
        source.sendSuccess(() -> Component.literal("迷你Boss词条: " + text), false);
        return 1;
    }

    private static int add(CommandSourceStack source, Entity target, String powerId, int level) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("目标不是 LivingEntity"));
            return 0;
        }
        MiniBossPower power = MiniBossPower.byId(powerId);
        if (power == null) {
            source.sendFailure(Component.literal("未知词条: " + powerId));
            return 0;
        }
        MiniBossData.addPower(living, power, level);
        if (living instanceof Mob mob) MiniBossGoals.attach(mob);
        source.sendSuccess(() -> Component.literal("已添加 " + power.id() + ":" + level), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, Entity target, String powerId) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("目标不是 LivingEntity"));
            return 0;
        }
        MiniBossPower power = MiniBossPower.byId(powerId);
        if (power == null) {
            source.sendFailure(Component.literal("未知词条: " + powerId));
            return 0;
        }
        MiniBossData.removePower(living, power);
        source.sendSuccess(() -> Component.literal("已移除 " + power.id()), false);
        return 1;
    }

    private static int clear(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("目标不是 LivingEntity"));
            return 0;
        }
        MiniBossData.clearPowers(living);
        source.sendSuccess(() -> Component.literal("已清除迷你Boss标记与词条"), false);
        return 1;
    }
}

package com.rainbow.miniboss.logic;

import com.rainbow.miniboss.data.MiniBossData;
import com.rainbow.miniboss.power.MiniBossPower;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public final class MiniBossGoals {
    private static final int DIG_BASE_TICKS = 12;
    private static final int LEADER_INTERVAL = 40;
    private static final int WITCH_CAST_INTERVAL = 200;
    private static final int SPIDER_WEB_INTERVAL = 300;
    private static final int CREEPER_JUMP_COOLDOWN = 40;
    private static final Potion[] WITCH_POTIONS = {
            Potions.SLOWNESS, Potions.WEAKNESS, Potions.POISON, Potions.STRONG_POISON, Potions.HARMING
    };

    private MiniBossGoals() {
    }

    public static void attach(Mob mob) {
        if (!MiniBossData.isMiniBoss(mob)) return;

        if (MiniBossData.hasPower(mob, MiniBossPower.REGENERATE)) add(mob, 0, new RegenerateGoal(mob));
        if (MiniBossData.hasPower(mob, MiniBossPower.REINFORCE)) add(mob, 1, new ReinforceGoal(mob));
        if (MiniBossData.hasPower(mob, MiniBossPower.LEADER)) add(mob, 2, new LeaderGoal(mob));
        if (MiniBossData.hasPower(mob, MiniBossPower.DIG)) add(mob, 3, new DigGoal(mob));
        if (MiniBossData.hasPower(mob, MiniBossPower.STEALTH)) add(mob, 4, new StealthGoal(mob));
        if (MiniBossData.hasPower(mob, MiniBossPower.EVOLVE)) {
            if (mob instanceof Witch) add(mob, 5, new EvolveWitchGoal(mob));
            if (mob instanceof Spider) add(mob, 5, new EvolveSpiderGoal(mob));
            if (mob instanceof Creeper) add(mob, 5, new EvolveCreeperGoal(mob));
        }
    }

    private static void add(Mob mob, int priority, Goal goal) {
        if (hasGoal(mob, goal.getClass())) return;
        mob.goalSelector.addGoal(priority, goal);
    }

    private static boolean hasGoal(Mob mob, Class<? extends Goal> type) {
        Set<WrappedGoal> goals = mob.goalSelector.getAvailableGoals();
        for (WrappedGoal wrapped : goals) {
            if (type.isInstance(wrapped.getGoal())) return true;
        }
        return false;
    }

    private static final class RegenerateGoal extends Goal {
        private final Mob mob;
        private int timer;

        private RegenerateGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return MiniBossData.hasPower(mob, MiniBossPower.REGENERATE) && mob.getHealth() < mob.getMaxHealth();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            timer = 0;
        }

        @Override
        public void tick() {
            if (++timer < 20) return;
            timer = 0;
            mob.heal(MiniBossData.getPowerLevel(mob, MiniBossPower.REGENERATE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private static final class ReinforceGoal extends Goal {
        private final Mob mob;

        private ReinforceGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!MiniBossData.hasPower(mob, MiniBossPower.REINFORCE)) return false;
            if (MiniBossData.tag(mob).getBoolean("_mb_reinforce_called")) return false;
            if (MiniBossData.tag(mob).getBoolean(MiniBossData.REINFORCE_SPAWNED_KEY)) return false;
            return mob.getTarget() != null && mob.getTarget().isAlive();
        }

        @Override
        public void start() {
            MiniBossData.tag(mob).putBoolean("_mb_reinforce_called", true);
            LivingEntity target = mob.getTarget();
            if (target == null || !(mob.level() instanceof ServerLevel level)) return;

            int count = MiniBossData.getPowerLevel(mob, MiniBossPower.REINFORCE);
            for (int i = 0; i < count; i++) {
                Entity entity = mob.getType().create(level);
                if (entity == null) continue;
                entity.moveTo(mob.getX(), mob.getY() + 1.0D, mob.getZ(), mob.getYRot(), mob.getXRot());
                if (entity instanceof Mob spawned) {
                    spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(spawned.blockPosition()),
                            MobSpawnType.MOB_SUMMONED, null, null);
                    spawned.setTarget(target);
                }
                entity.getPersistentData().putBoolean(MiniBossData.REINFORCE_SPAWNED_KEY, true);
                level.addFreshEntity(entity);
            }
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 1.0F);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    private static final class LeaderGoal extends Goal {
        private final Mob mob;
        private int timer;

        private LeaderGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return MiniBossData.hasPower(mob, MiniBossPower.LEADER);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            timer = 0;
        }

        @Override
        public void tick() {
            if (++timer < LEADER_INTERVAL) return;
            timer = 0;
            AABB area = mob.getBoundingBox().inflate(16.0D);
            MobEffect resilience = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("quark", "resilience"));
            for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area,
                    candidate -> candidate != mob && candidate.getType() == mob.getType())) {
                MobEffect effect = resilience != null ? resilience : MobEffects.DAMAGE_RESISTANCE;
                other.addEffect(new MobEffectInstance(effect, 100, 0, false, true));
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, true));
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private static final class DigGoal extends Goal {
        private final Mob mob;
        private BlockPos digPos;
        private float hardness;
        private int progress;

        private DigGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!MiniBossData.hasPower(mob, MiniBossPower.DIG)) return false;
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            return findBlock(target);
        }

        private boolean findBlock(LivingEntity target) {
            if (!(mob.level() instanceof ServerLevel level)) return false;
            Vec3 from = mob.getEyePosition();
            Vec3 direction = target.getEyePosition().subtract(from);
            if (direction.lengthSqr() < 1.0E-4D) return false;
            Vec3 to = from.add(direction.normalize().scale(8.0D));
            BlockHitResult hit = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            if (hit.getType() != HitResult.Type.BLOCK) return false;

            BlockState state = level.getBlockState(hit.getBlockPos());
            if (state.isAir()) return false;
            float speed = state.getDestroySpeed(level, hit.getBlockPos());
            if (speed < 0.0F) return false;
            digPos = hit.getBlockPos();
            hardness = speed;
            progress = 0;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (digPos == null || !MiniBossData.hasPower(mob, MiniBossPower.DIG)) return false;
            BlockState state = mob.level().getBlockState(digPos);
            return !state.isAir() && progress < breakTime();
        }

        private int breakTime() {
            int base = Math.max(10, (int) Math.ceil(hardness * DIG_BASE_TICKS));
            return Math.max(1, base / MiniBossData.getPowerLevel(mob, MiniBossPower.DIG));
        }

        @Override
        public void start() {
            progress = 0;
        }

        @Override
        public void stop() {
            if (digPos != null) {
                mob.level().destroyBlockProgress(mob.getId(), digPos, -1);
            }
            digPos = null;
            progress = 0;
        }

        @Override
        public void tick() {
            if (digPos == null) return;
            if (mob.level().getBlockState(digPos).isAir()) {
                digPos = null;
                return;
            }

            progress++;
            mob.getLookControl().setLookAt(digPos.getX() + 0.5D, digPos.getY() + 0.5D, digPos.getZ() + 0.5D);
            if (progress % 5 == 0) {
                int stage = Math.min(9, Math.max(0, progress * 10 / breakTime()));
                mob.level().destroyBlockProgress(mob.getId(), digPos, stage);
                mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            if (progress >= breakTime()) {
                BlockPos done = digPos;
                mob.level().destroyBlock(done, true);
                mob.level().destroyBlockProgress(mob.getId(), done, -1);
                digPos = null;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private static final class StealthGoal extends Goal {
        private final Mob mob;

        private StealthGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!MiniBossData.hasPower(mob, MiniBossPower.STEALTH)) return false;
            LivingEntity target = mob.getTarget();
            return target == null || !target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            MiniBossData.tag(mob).putBoolean("_mb_stealth", true);
        }

        @Override
        public void stop() {
            MiniBossData.tag(mob).putBoolean("_mb_stealth", false);
            mob.removeEffect(MobEffects.INVISIBILITY);
        }

        @Override
        public void tick() {
            if (!mob.hasEffect(MobEffects.INVISIBILITY)) {
                mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private static final class EvolveWitchGoal extends Goal {
        private final Mob mob;
        private long nextCastTick;

        private EvolveWitchGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!(mob instanceof Witch) || !MiniBossData.hasPower(mob, MiniBossPower.EVOLVE)) return false;
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && mob.distanceToSqr(target) <= 144.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            if (nextCastTick == 0L) nextCastTick = mob.level().getGameTime();
        }

        @Override
        public void tick() {
            if (mob.level().getGameTime() < nextCastTick) return;
            LivingEntity target = mob.getTarget();
            if (target == null || !(mob.level() instanceof ServerLevel level)) return;
            nextCastTick = level.getGameTime() + WITCH_CAST_INTERVAL;

            ThrownPotion potion = new ThrownPotion(level, mob);
            ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            PotionUtils.setPotion(stack, WITCH_POTIONS[mob.getRandom().nextInt(WITCH_POTIONS.length)]);
            potion.setItem(stack);
            potion.setPos(mob.getX(), mob.getEyeY() - 0.1D, mob.getZ());

            double dx = target.getX() - mob.getX();
            double dy = target.getY(0.3D) - potion.getY();
            double dz = target.getZ() - mob.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.1D) distance = 0.1D;
            potion.setDeltaMovement(dx / distance * 0.7D, dy / distance * 0.7D + distance * 0.045D,
                    dz / distance * 0.7D);
            level.addFreshEntity(potion);
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private static final class EvolveSpiderGoal extends Goal {
        private final Mob mob;
        private long nextWebTick;

        private EvolveSpiderGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!(mob instanceof Spider) || !MiniBossData.hasPower(mob, MiniBossPower.EVOLVE)) return false;
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            if (nextWebTick == 0L) nextWebTick = mob.level().getGameTime() + SPIDER_WEB_INTERVAL;
        }

        @Override
        public void tick() {
            if (mob.level().getGameTime() < nextWebTick) return;
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            nextWebTick = mob.level().getGameTime() + SPIDER_WEB_INTERVAL;

            BlockPos pos = target.blockPosition();
            if (mob.level().getBlockState(pos).isAir()) {
                mob.level().setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
                mob.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
        }
    }

    private static final class EvolveCreeperGoal extends Goal {
        private final Mob mob;
        private long nextJumpTick;

        private EvolveCreeperGoal(Mob mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (!(mob instanceof Creeper) || !MiniBossData.hasPower(mob, MiniBossPower.EVOLVE)) return false;
            if (mob.level().getGameTime() < nextJumpTick) return false;
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || !mob.onGround()) return false;
            double distance = mob.distanceToSqr(target);
            return distance >= 36.0D && distance <= 1600.0D;
        }

        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            double dx = target.getX() - mob.getX();
            double dz = target.getZ() - mob.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 0.1D) return;
            double speed = Math.min(1.4D, 0.6D + distance * 0.04D);
            mob.setDeltaMovement(dx / distance * speed, 0.85D, dz / distance * speed);
            mob.hurtMarked = true;
            nextJumpTick = mob.level().getGameTime() + CREEPER_JUMP_COOLDOWN;
            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 0.8F, 0.7F);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}

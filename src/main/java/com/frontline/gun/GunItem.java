package com.frontline.gun;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Arma de fuego hitscan. El servidor es la autoridad: municion, cadencia, recarga y dano. */
public class GunItem extends Item {
    public static final String TAG_AMMO = "Ammo";
    public static final String TAG_RELOAD_END = "ReloadEnd";
    /** Tick de juego a partir del cual cada jugador puede volver a disparar (solo servidor). */
    public static final Map<UUID, Long> NEXT_SHOT = new ConcurrentHashMap<>();

    private final GunStats stats;

    public GunItem(GunStats stats) {
        super(new Item.Properties().stacksTo(1));
        this.stats = stats;
    }

    public GunStats stats() { return stats; }

    // ---------- estado en NBT ----------
    public static int getAmmo(ItemStack s) { return s.hasTag() ? s.getTag().getInt(TAG_AMMO) : 0; }
    public static void setAmmo(ItemStack s, int a) { s.getOrCreateTag().putInt(TAG_AMMO, a); }
    public static boolean isReloading(ItemStack s) { return s.hasTag() && s.getTag().contains(TAG_RELOAD_END); }

    public int countAmmo(Player p) {
        if (p.getAbilities().instabuild) return 999;
        Item ammo = stats.ammo.get();
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(ammo)) n += s.getCount();
        for (ItemStack s : p.getInventory().offhand) if (s.is(ammo)) n += s.getCount();
        return n;
    }

    private int takeAmmo(Player p, int need) {
        if (p.getAbilities().instabuild) return need;
        Item ammo = stats.ammo.get();
        int taken = 0;
        for (ItemStack s : p.getInventory().items) {
            if (taken >= need) break;
            if (s.is(ammo)) {
                int t = Math.min(need - taken, s.getCount());
                s.shrink(t);
                taken += t;
            }
        }
        return taken;
    }

    // ---------- recarga ----------
    public void startReload(ServerPlayer p, ItemStack gun) {
        if (isReloading(gun) || getAmmo(gun) >= stats.mag || countAmmo(p) <= 0) return;
        gun.getOrCreateTag().putLong(TAG_RELOAD_END, p.level().getGameTime() + stats.reload);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !isReloading(stack)) return;
        // Cambiar de arma cancela la recarga, como en los shooters.
        if (!selected || !(entity instanceof ServerPlayer p)) {
            stack.getTag().remove(TAG_RELOAD_END);
            return;
        }
        if (level.getGameTime() >= stack.getTag().getLong(TAG_RELOAD_END)) {
            int take = takeAmmo(p, stats.mag - getAmmo(stack));
            setAmmo(stack, getAmmo(stack) + take);
            stack.getTag().remove(TAG_RELOAD_END);
            level.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    // ---------- disparo ----------
    public static void tryFire(ServerPlayer p, boolean ads) {
        ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) return;
        GunStats s = gun.stats;
        if (!p.isAlive() || p.isSpectator() || p.isSprinting() || isReloading(stack)) return;

        long now = p.level().getGameTime();
        Long next = NEXT_SHOT.get(p.getUUID());
        if (next != null && now < next) return;
        int ammo = getAmmo(stack);
        if (ammo <= 0) return;

        setAmmo(stack, ammo - 1);
        NEXT_SHOT.put(p.getUUID(), now + s.delay);

        // Modificadores de dispersion: apuntar, moverse, saltar, agacharse.
        float spread = s.spread;
        if (ads) spread *= s.adsSpread;
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        if (dx * dx + dz * dz > 0.0036) spread *= 1.6f;
        if (!p.onGround()) spread *= 2.5f;
        if (p.isCrouching()) spread *= 0.7f;

        ServerLevel level = p.serverLevel();
        for (int i = 0; i < s.pellets; i++) hitscan(p, level, s, spread);

        level.playSound(null, p.getX(), p.getY(), p.getZ(), s.sound.get(), SoundSource.PLAYERS, s.volume, s.pitch);
        Vec3 muzzle = p.getEyePosition().add(p.getLookAngle().scale(0.9));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1, muzzle.z, 2, 0.02, 0.02, 0.02, 0.01);
    }

    private static Vec3 spreadDir(Vec3 look, float spread, RandomSource r) {
        return look.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
    }

    private static void hitscan(ServerPlayer p, ServerLevel level, GunStats s, float spread) {
        Vec3 eye = p.getEyePosition();
        Vec3 dir = spreadDir(p.getLookAngle(), spread, p.getRandom());
        Vec3 end = eye.add(dir.scale(s.range));

        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double maxDist = block.getType() == HitResult.Type.MISS ? s.range : block.getLocation().distanceTo(eye);
        Vec3 clipEnd = eye.add(dir.scale(maxDist));

        AABB box = p.getBoundingBox().expandTowards(dir.scale(maxDist)).inflate(1.0);
        Entity best = null;
        Vec3 bestPos = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : level.getEntities(p, box, en -> !en.isSpectator() && en.isPickable())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(e.getPickRadius()).clip(eye, clipEnd);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestD) { bestD = d; best = e; bestPos = hit.get(); }
            }
        }

        if (best instanceof LivingEntity target) {
            boolean head = bestPos.y >= target.getY() + target.getBbHeight() * 0.8;
            double dist = Math.sqrt(bestD);
            float half = s.range * 0.5f;
            float falloff = dist <= half ? 1f : Mth.clamp(1f - 0.4f * (float) ((dist - half) / half), 0.6f, 1f);
            float dmg = s.damage * falloff * (head ? s.headshot : 1f);
            target.invulnerableTime = 0; // permite cadencias mas rapidas que los i-frames vanilla
            target.hurt(level.damageSources().playerAttack(p), dmg);
            p.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6f, head ? 1.7f : 1.1f);
        } else if (block.getType() != HitResult.Type.MISS) {
            Vec3 l = block.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(block.getBlockPos())),
                    l.x, l.y, l.z, 4, 0.05, 0.05, 0.05, 0.1);
        }
    }

    // ---------- comportamiento de item ----------
    // Evita la animacion de "re-equipar" cada vez que cambia la municion en el NBT.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
                                  net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        String pellets = stats.pellets > 1 ? " x" + stats.pellets : "";
        tips.add(Component.literal("Damage: " + stats.damage + pellets).withStyle(ChatFormatting.GRAY));
        tips.add(Component.literal("RPM: " + (1200 / stats.delay) + (stats.auto ? " (auto)" : " (semi)")).withStyle(ChatFormatting.GRAY));
        tips.add(Component.literal("Mag: " + getAmmo(stack) + "/" + stats.mag).withStyle(ChatFormatting.GRAY));
    }
}

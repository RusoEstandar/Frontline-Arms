package com.frontline.equipment;

import com.frontline.ModEntities;
import com.frontline.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Granada con rebote y mecha. El tipo (frag/flash/humo) sale del item que la representa. */
public class GrenadeEntity extends ThrowableItemProjectile {
    private static final int FRAG_FUSE = 50, FLASH_FUSE = 40, SMOKE_FUSE = 25, SMOKE_DURATION = 300;
    private static final double FLASH_RADIUS = 14.0;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity owner) {
        super(ModEntities.GRENADE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.FRAG.get(); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private GrenadeItem.Kind kind() {
        return getItem().getItem() instanceof GrenadeItem g ? g.kind : GrenadeItem.Kind.FRAG;
    }

    @Override
    protected float getGravity() { return 0.04f; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;

        switch (kind()) {
            case FRAG -> {
                if (tickCount >= FRAG_FUSE) {
                    sl.explode(getOwner(), getX(), getY() + 0.1, getZ(), 3.0f, Level.ExplosionInteraction.NONE);
                    discard();
                }
            }
            case FLASH -> {
                if (tickCount >= FLASH_FUSE) {
                    flash(sl);
                    discard();
                }
            }
            case SMOKE -> {
                if (tickCount == SMOKE_FUSE) {
                    setDeltaMovement(Vec3.ZERO);
                    setNoGravity(true);
                    sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.5f, 0.6f);
                }
                if (tickCount >= SMOKE_FUSE) {
                    if (tickCount % 2 == 0) {
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 14, 1.8, 0.9, 1.8, 0.01);
                    }
                    if (tickCount >= SMOKE_FUSE + SMOKE_DURATION) discard();
                }
            }
        }
    }

    private void flash(ServerLevel sl) {
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 3.0f, 1.8f);
        sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(FLASH_RADIUS))) {
            if (!e.hasLineOfSight(this)) continue;
            double dist = e.distanceTo(this);
            Vec3 toGrenade = position().subtract(e.getEyePosition()).normalize();
            double facing = Math.max(0.0, e.getViewVector(1.0f).dot(toGrenade)); // 1 = mirando a la granada
            int ticks = (int) ((1.0 - dist / FLASH_RADIUS) * (40 + 100 * facing));
            if (ticks > 10) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks / 2, 1));
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        Vec3 v = getDeltaMovement();
        switch (hit.getDirection().getAxis()) {
            case X -> v = new Vec3(-v.x * 0.45, v.y * 0.8, v.z * 0.8);
            case Z -> v = new Vec3(v.x * 0.8, v.y * 0.8, -v.z * 0.45);
            case Y -> v = new Vec3(v.x * 0.7, Math.abs(v.y) < 0.12 ? 0.0 : -v.y * 0.4, v.z * 0.7);
        }
        if (hit.getDirection() == Direction.UP) {
            // Apoyar la granada sobre la superficie para que no se hunda en el suelo.
            setPos(getX(), hit.getLocation().y + 0.125, getZ());
        }
        setDeltaMovement(v);
        if (!level().isClientSide && v.lengthSqr() > 0.02) {
            playSound(SoundEvents.CHAIN_HIT, 0.7f, 1.4f);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // Atraviesa entidades sin detonar: la mecha decide cuando explota.
    }
}

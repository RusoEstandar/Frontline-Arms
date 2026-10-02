package com.frontline.gun;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;

/** Parametros de balance de un arma. Edita los valores en ModItems para ajustar. */
public final class GunStats {
    public final Supplier<Item> ammo;
    public float damage = 5f, headshot = 2f, range = 60f;
    public float spread = 0.02f, adsSpread = 0.3f;          // dispersion cadera / multiplicador al apuntar
    public float recoilPitch = 1f, recoilYaw = 0.3f;         // grados por disparo
    public float adsZoom = 1.2f, volume = 1f, pitch = 1f;
    public int delay = 3, mag = 30, reload = 50, pellets = 1, adsTicks = 4;
    public boolean auto = true, scope = false;
    public Supplier<SoundEvent> sound = () -> SoundEvents.FIREWORK_ROCKET_BLAST;

    public GunStats(Supplier<Item> ammo) { this.ammo = ammo; }

    public GunStats dmg(float d, float hs) { damage = d; headshot = hs; return this; }
    public GunStats rate(int delayTicks, boolean automatic) { delay = delayTicks; auto = automatic; return this; }
    public GunStats mag(int size, int reloadTicks) { mag = size; reload = reloadTicks; return this; }
    public GunStats acc(float hip, float adsMult) { spread = hip; adsSpread = adsMult; return this; }
    public GunStats recoil(float p, float y) { recoilPitch = p; recoilYaw = y; return this; }
    public GunStats ads(float zoom, int ticks, boolean hasScope) { adsZoom = zoom; adsTicks = ticks; scope = hasScope; return this; }
    public GunStats range(float r) { range = r; return this; }
    public GunStats pellets(int n) { pellets = n; return this; }
    public GunStats sfx(Supplier<SoundEvent> s, float vol, float p) { sound = s; volume = vol; pitch = p; return this; }
}

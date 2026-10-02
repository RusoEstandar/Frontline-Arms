package com.frontline.client.anim;

import com.frontline.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Estado de animacion del arma en primera persona (solo jugador local). */
public final class GunAnim {
    private static String current;
    private static double start;
    private static float speed = 1f;
    private static Item lastItem;
    private static boolean wasReloading;

    public static float sprint, sprintPrev;
    public static float swayPitch, swayPitchO, swayYaw, swayYawO;
    private static boolean swayInit;

    public static String current() { return current; }

    private static void play(String name, float spd, Level level) {
        current = name;
        speed = spd;
        start = level.getGameTime();
    }

    public static void reset() {
        current = null;
        lastItem = null;
        wasReloading = false;
        sprint = sprintPrev = 0f;
        swayInit = false;
    }

    /** Se llama cada tick de cliente mientras se sostiene un arma. */
    public static void tick(LocalPlayer p, ItemStack held, GunItem gun, GunModelData m) {
        Level level = p.level();
        Item item = held.getItem();
        if (item != lastItem) { // arma nueva en mano: animacion de sacar
            lastItem = item;
            wasReloading = false;
            if (m != null && m.anims.containsKey("draw")) play("draw", 1f, level); else current = null;
        }

        boolean rel = GunItem.isReloading(held);
        if (m != null && rel && !wasReloading) {
            String n = (GunItem.getAmmo(held) == 0 && m.anims.containsKey("reload_empty")) ? "reload_empty" : "reload";
            GunModelData.Anim a = m.anims.get(n);
            if (a != null) play(n, a.length / Math.max(1, gun.stats().reload), level); // ajusta la duracion a la del servidor
        }
        if (!rel && wasReloading && current != null && current.startsWith("reload")) current = null;
        wasReloading = rel;

        if (m != null && current != null) {
            GunModelData.Anim a = m.anims.get(current);
            if (a == null || (!a.loop && (level.getGameTime() - start) * speed >= a.length)) current = null;
        }

        sprintPrev = sprint;
        sprint = Mth.clamp(sprint + (p.isSprinting() && !rel ? 0.2f : -0.2f), 0f, 1f);

        // Inercia del arma al girar la camara.
        if (!swayInit) {
            swayPitch = swayPitchO = p.getXRot();
            swayYaw = swayYawO = p.getYRot();
            swayInit = true;
        }
        swayPitchO = swayPitch;
        swayYawO = swayYaw;
        swayPitch += (p.getXRot() - swayPitch) * 0.35f;
        swayYaw += Mth.wrapDegrees(p.getYRot() - swayYaw) * 0.35f;
    }

    public static void onShoot(GunModelData m) {
        Level level = Minecraft.getInstance().level;
        if (m != null && level != null && m.anims.containsKey("shoot")) play("shoot", 1f, level);
    }

    public static void inspect(GunModelData m, ItemStack held, LocalPlayer p) {
        if (m == null || current != null || GunItem.isReloading(held) || !m.anims.containsKey("inspect")) return;
        play("inspect", 1f, p.level());
    }

    /** Tiempo transcurrido de la animacion actual (en unidades de la animacion), o -1 si no hay. */
    public static float elapsed(float partial) {
        Level level = Minecraft.getInstance().level;
        if (current == null || level == null) return -1f;
        return (float) ((level.getGameTime() + partial - start) * speed);
    }

    public static float[] pos(GunModelData m, String track, float t) {
        GunModelData.Track tr = track(m, track);
        return tr == null ? GunModelData.ZERO3 : GunModelData.sample(tr.pos, clampT(m, t));
    }

    public static float[] rot(GunModelData m, String track, float t) {
        GunModelData.Track tr = track(m, track);
        return tr == null ? GunModelData.ZERO3 : GunModelData.sample(tr.rot, clampT(m, t));
    }

    private static GunModelData.Track track(GunModelData m, String name) {
        if (current == null) return null;
        GunModelData.Anim a = m.anims.get(current);
        return a == null ? null : a.tracks.get(name);
    }

    private static float clampT(GunModelData m, float t) {
        GunModelData.Anim a = m.anims.get(current);
        if (a.loop && a.length > 0f) return t % a.length;
        return Math.min(t, a.length);
    }

    private GunAnim() {}
}

package com.frontline.client;

import com.frontline.Frontline;
import com.frontline.client.anim.GunAnim;
import com.frontline.client.anim.GunModelData;
import com.frontline.client.anim.GunModels;
import com.frontline.gun.GunItem;
import com.frontline.gun.GunStats;
import com.frontline.net.FirePacket;
import com.frontline.net.Network;
import com.frontline.net.ReloadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Controles estilo shooter: clic izq = disparar (mantener para automaticas),
 * clic der (mantener) = apuntar, R = recargar.
 */
@Mod.EventBusSubscriber(modid = Frontline.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    /** Progreso de apuntado 0..1 (actual y del tick anterior, para interpolar). */
    public static float ads, adsPrev;
    private static boolean wasAttack;
    private static long nextShot;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        adsPrev = ads;
        if (p == null) { ads = 0; return; }

        ItemStack held = p.getMainHandItem();
        boolean attackDown = mc.options.keyAttack.isDown();
        if (!(held.getItem() instanceof GunItem gun)) {
            ads = 0;
            adsPrev = 0;
            wasAttack = attackDown;
            GunAnim.reset();
            return;
        }
        if (mc.screen != null) {
            ads = 0;
            adsPrev = 0;
            wasAttack = attackDown;
            return;
        }

        GunStats s = gun.stats();
        boolean reloading = GunItem.isReloading(held);
        boolean aiming = mc.options.keyUse.isDown() && !p.isSprinting() && !reloading;
        float step = 1f / s.adsTicks;
        ads = Mth.clamp(ads + (aiming ? step : -step), 0f, 1f);

        GunModelData md = GunModels.get(held.getItem());
        GunAnim.tick(p, held, gun, md);
        while (ModKeys.INSPECT.consumeClick()) GunAnim.inspect(md, held, p);

        boolean pull = attackDown && (s.auto || !wasAttack);
        wasAttack = attackDown;
        if (pull) tryFire(p, held, s, aiming);

        while (ModKeys.RELOAD.consumeClick()) Network.CHANNEL.sendToServer(new ReloadPacket());
    }

    private static void tryFire(LocalPlayer p, ItemStack held, GunStats s, boolean aiming) {
        long now = p.level().getGameTime();
        if (now < nextShot || p.isSprinting() || GunItem.isReloading(held)) return;
        if (GunItem.getAmmo(held) <= 0) {
            p.playSound(SoundEvents.DISPENSER_FAIL, 0.6f, 1.6f); // "click" de cargador vacio
            nextShot = now + 8;
            return;
        }
        nextShot = now + s.delay;
        Network.CHANNEL.sendToServer(new FirePacket(aiming));
        GunAnim.onShoot(GunModels.get(held.getItem()));

        // Retroceso: patada hacia arriba con algo de deriva lateral; menor al apuntar.
        float k = aiming ? 0.6f : 1f;
        float r = p.getRandom().nextFloat();
        p.setXRot(p.getXRot() - s.recoilPitch * k * (0.8f + 0.4f * r));
        p.setYRot(p.getYRot() + (p.getRandom().nextFloat() - 0.5f) * 2f * s.recoilYaw * k);
    }

    /** Con arma en mano, ni atacar ni usar item vanilla (evita golpes/colocar bloques). */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.getMainHandItem().getItem() instanceof GunItem && (e.isAttack() || e.isUseItem())) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent e) {
        if (!(e.getPlayer().getMainHandItem().getItem() instanceof GunItem gun)) return;
        float t = Mth.lerp(Minecraft.getInstance().getPartialTick(), adsPrev, ads);
        if (t <= 0f) return;
        e.setNewFovModifier(e.getNewFovModifier() * Mth.lerp(t, 1f, 1f / gun.stats().adsZoom));
    }

    /** Apuntar reduce la velocidad de movimiento. */
    @SubscribeEvent
    public static void onMove(MovementInputUpdateEvent e) {
        if (e.getEntity().getMainHandItem().getItem() instanceof GunItem && ads > 0) {
            float m = 1f - 0.4f * ads;
            e.getInput().leftImpulse *= m;
            e.getInput().forwardImpulse *= m;
        }
    }

    /** Oculta la mira vanilla al apuntar. */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre e) {
        if (ads > 0.5f && e.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) e.setCanceled(true);
    }

    private ClientEvents() {}
}

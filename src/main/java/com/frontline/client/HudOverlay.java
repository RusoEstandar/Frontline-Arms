package com.frontline.client;

import com.frontline.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** HUD: municion (cargador / reserva) y mira telescopica del francotirador. */
public final class HudOverlay implements IGuiOverlay {
    public static final HudOverlay INSTANCE = new HudOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        ItemStack held = p.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gun)) return;

        if (gun.stats().scope && Mth.lerp(partialTick, ClientEvents.adsPrev, ClientEvents.ads) > ClientEvents.SCOPE_OVERLAY_AT) drawScope(g, w, h);

        Font font = mc.font;
        int ammo = GunItem.getAmmo(held);
        boolean reloading = GunItem.isReloading(held);
        int reserve = gun.countAmmo(p);

        String main = reloading ? Component.translatable("hud.frontline.reloading").getString() : String.valueOf(ammo);
        int color = reloading ? 0xFFFFAA00 : (ammo == 0 ? 0xFFFF4444 : (ammo <= gun.stats().mag / 4 ? 0xFFFFCC44 : 0xFFFFFFFF));

        String res = "/ " + (p.getAbilities().instabuild ? "\u221E" : String.valueOf(reserve));
        int resW = font.width(res);
        int mainW = font.width(main) * 2;
        g.drawString(font, res, w - 10 - resW, h - 38, 0xFFBBBBBB, true);
        g.pose().pushPose();
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, main, (int) ((w - 14 - resW - mainW) / 2f), (int) ((h - 46) / 2f), color, true);
        g.pose().popPose();
    }

    private static void drawScope(GuiGraphics g, int w, int h) {
        int left = (w - h) / 2;
        g.fill(0, 0, left, h, 0xFF000000);
        g.fill(w - left, 0, w, h, 0xFF000000);
        g.fill(left, h / 2, w - left, h / 2 + 1, 0xFF000000);
        g.fill(w / 2, 0, w / 2 + 1, h, 0xFF000000);
    }

    private HudOverlay() {}
}

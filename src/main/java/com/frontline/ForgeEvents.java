package com.frontline;

import com.frontline.gun.GunItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos comunes (ambos lados). */
@Mod.EventBusSubscriber(modid = Frontline.MODID)
public final class ForgeEvents {
    /** Con un arma en mano, el clic izquierdo dispara: no debe romper bloques. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getItemStack().getItem() instanceof GunItem) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        GunItem.NEXT_SHOT.remove(e.getEntity().getUUID());
    }

    private ForgeEvents() {}
}

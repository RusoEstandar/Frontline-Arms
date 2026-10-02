package com.frontline.client;

import com.frontline.Frontline;
import com.frontline.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registros que ocurren en el bus del mod, solo en cliente. */
@Mod.EventBusSubscriber(modid = Frontline.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) { e.register(ModKeys.RELOAD); }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) { e.registerAboveAll("ammo", HudOverlay.INSTANCE); }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.GRENADE.get(), ThrownItemRenderer::new);
    }

    private ClientModEvents() {}
}

package com.frontline.client;

import com.frontline.Frontline;
import com.frontline.client.anim.GunAnim;
import com.frontline.client.anim.GunModelData;
import com.frontline.client.anim.GunModels;
import com.frontline.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dibuja el arma en primera persona pieza a pieza (cuerpo, corredera, cargador, manos...)
 * aplicando la animacion activa, el apuntado, el sprint, el balanceo y la inercia.
 */
@Mod.EventBusSubscriber(modid = Frontline.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GunRenderer {
    private static final ResourceLocation PALETTE = new ResourceLocation(Frontline.MODID, "textures/item/palette.png");
    private static final int FULL_BRIGHT = 15728880;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = e.getItemStack();
        if (!(stack.getItem() instanceof GunItem)) return;
        GunModelData m = GunModels.get(stack.getItem());
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (m == null || p == null || mc.level == null) return; // sin datos: render vanilla
        e.setCanceled(true);

        float pt = e.getPartialTick();
        float adsRaw = Mth.lerp(pt, ClientEvents.adsPrev, ClientEvents.ads);
        float adsE = adsRaw * adsRaw * (3f - 2f * adsRaw);
        float spr = Mth.lerp(pt, GunAnim.sprintPrev, GunAnim.sprint);
        spr = spr * spr * (3f - 2f * spr) * (1f - adsE);

        // Pose base: cadera -> sprint -> apuntado.
        float[] pose = new float[6];
        for (int i = 0; i < 6; i++) {
            float base = Mth.lerp(spr, m.hip[i], m.sprint[i]);
            pose[i] = Mth.lerp(adsE, base, m.ads[i]);
        }

        // Balanceo al andar, respiracion e inercia al girar (menos al apuntar).
        float calm = 1f - adsE * 0.8f;
        float time = mc.level.getGameTime() + pt;
        float walkDist = Mth.lerp(pt, p.walkDistO, p.walkDist);
        float bob = Mth.lerp(pt, p.oBob, p.bob);
        pose[0] += Mth.sin(walkDist * Mth.PI) * bob * 0.03f * calm;
        pose[1] += (-Math.abs(Mth.cos(walkDist * Mth.PI)) * bob * 0.04f + Mth.sin(time * 0.09f) * 0.002f) * calm;
        pose[3] += Mth.sin(time * 0.07f) * 0.25f * calm;
        float dyaw = Mth.clamp(Mth.wrapDegrees(p.getYRot() - Mth.rotLerp(pt, GunAnim.swayYawO, GunAnim.swayYaw)), -8f, 8f);
        float dpitch = Mth.clamp(p.getXRot() - Mth.lerp(pt, GunAnim.swayPitchO, GunAnim.swayPitch), -8f, 8f);
        pose[4] += dyaw * 0.18f * calm;
        pose[3] += dpitch * 0.18f * calm;

        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(pose[0], pose[1], pose[2]);
        rotate(ps, pose[3], pose[4], pose[5]);

        float t = GunAnim.elapsed(pt);
        float[] rp = GunAnim.pos(m, "root", t);
        float[] rr = GunAnim.rot(m, "root", t);
        ps.translate(rp[0] / 16f, rp[1] / 16f, rp[2] / 16f);
        rotate(ps, rr[0], rr[1], rr[2]);

        ps.mulPose(Axis.YP.rotationDegrees(90f)); // el canon (+X del modelo) apunta hacia delante
        float s = 0.0625f * m.scale;
        ps.scale(s, s, s);
        ps.translate(-m.pivot[0], -m.pivot[1], -m.pivot[2]);

        boolean reloading = GunItem.isReloading(stack);
        boolean empty = GunItem.getAmmo(stack) == 0 && !reloading;
        boolean shooting = "shoot".equals(GunAnim.current()) && t >= 0f && t < 1.6f;
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(PALETTE));

        for (GunModelData.Part part : m.parts.values()) {
            boolean flash = part.name.equals("flash");
            if (flash && !shooting) continue;
            float[] pp = GunAnim.pos(m, part.name, t);
            float[] pr = GunAnim.rot(m, part.name, t);
            if (empty && part.lock != null) pp = part.lock; // corredera retenida atras

            ps.pushPose();
            ps.translate(pp[0], pp[1], pp[2]);
            float rx = part.rot[0] + pr[0], ry = part.rot[1] + pr[1], rz = part.rot[2] + pr[2];
            if (rx != 0f || ry != 0f || rz != 0f) {
                ps.translate(part.pivot[0], part.pivot[1], part.pivot[2]);
                rotate(ps, rx, ry, rz);
                ps.translate(-part.pivot[0], -part.pivot[1], -part.pivot[2]);
            }
            int light = flash ? FULL_BRIGHT : e.getPackedLight();
            for (GunModelData.Box b : part.boxes) addBox(vc, ps.last(), b, light);
            ps.popPose();
        }
        ps.popPose();
    }

    private static void rotate(PoseStack ps, float rx, float ry, float rz) {
        if (rx != 0f) ps.mulPose(Axis.XP.rotationDegrees(rx));
        if (ry != 0f) ps.mulPose(Axis.YP.rotationDegrees(ry));
        if (rz != 0f) ps.mulPose(Axis.ZP.rotationDegrees(rz));
    }

    /** Cubo de color plano: toma el color de una celda 2x2 de la paleta. */
    private static void addBox(VertexConsumer vc, PoseStack.Pose pose, GunModelData.Box b, int light) {
        float u = (2 * (b.color % 8) + 1) / 16f;
        float v = (2 * (b.color / 8) + 1) / 16f;
        float x0 = b.x0, y0 = b.y0, z0 = b.z0, x1 = b.x1, y1 = b.y1, z1 = b.z1;
        quad(vc, pose, 0, 0, -1, u, v, light, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0); // norte
        quad(vc, pose, 0, 0, 1, u, v, light, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);   // sur
        quad(vc, pose, -1, 0, 0, u, v, light, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // oeste
        quad(vc, pose, 1, 0, 0, u, v, light, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);  // este
        quad(vc, pose, 0, -1, 0, u, v, light, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // abajo
        quad(vc, pose, 0, 1, 0, u, v, light, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);  // arriba
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float nx, float ny, float nz, float u, float v, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vert(vc, pose, ax, ay, az, u, v, light, nx, ny, nz);
        vert(vc, pose, bx, by, bz, u, v, light, nx, ny, nz);
        vert(vc, pose, cx, cy, cz, u, v, light, nx, ny, nz);
        vert(vc, pose, dx, dy, dz, u, v, light, nx, ny, nz);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                             int light, float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    private GunRenderer() {}
}

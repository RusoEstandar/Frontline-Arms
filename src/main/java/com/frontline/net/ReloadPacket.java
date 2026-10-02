package com.frontline.net;

import com.frontline.gun.GunItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: "recarga el arma en mano". */
public record ReloadPacket() {
    public static void encode(ReloadPacket m, FriendlyByteBuf b) {}
    public static ReloadPacket decode(FriendlyByteBuf b) { return new ReloadPacket(); }

    public static void handle(ReloadPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context c = sup.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p == null) return;
            ItemStack stack = p.getMainHandItem();
            if (stack.getItem() instanceof GunItem gun) gun.startReload(p, stack);
        });
        c.setPacketHandled(true);
    }
}

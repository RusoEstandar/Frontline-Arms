package com.frontline.net;

import com.frontline.gun.GunItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: "dispare". Lleva si el jugador estaba apuntando (ADS). */
public record FirePacket(boolean ads) {
    public static void encode(FirePacket m, FriendlyByteBuf b) { b.writeBoolean(m.ads); }
    public static FirePacket decode(FriendlyByteBuf b) { return new FirePacket(b.readBoolean()); }

    public static void handle(FirePacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context c = sup.get();
        c.enqueueWork(() -> {
            if (c.getSender() != null) GunItem.tryFire(c.getSender(), m.ads);
        });
        c.setPacketHandled(true);
    }
}

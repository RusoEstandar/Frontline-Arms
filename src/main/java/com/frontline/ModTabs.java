package com.frontline;

import com.frontline.gun.GunItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Frontline.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.frontline"))
            .icon(() -> new ItemStack(ModItems.AR14.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<? extends Item> o : ModItems.ALL) {
                    ItemStack s = new ItemStack(o.get());
                    if (o.get() instanceof GunItem g) GunItem.setAmmo(s, g.stats().mag); // armas con cargador lleno
                    out.accept(s);
                }
            })
            .build());

    private ModTabs() {}
}

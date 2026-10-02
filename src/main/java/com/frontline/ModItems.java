package com.frontline;

import com.frontline.equipment.GrenadeItem;
import com.frontline.gun.GunItem;
import com.frontline.gun.GunStats;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Frontline.MODID);
    /** Todos los items del mod, en orden, para la pestana creativa. */
    public static final List<RegistryObject<? extends Item>> ALL = new ArrayList<>();

    private static <T extends Item> RegistryObject<T> reg(String name, Supplier<T> sup) {
        RegistryObject<T> o = ITEMS.register(name, sup);
        ALL.add(o);
        return o;
    }

    // ---- Municion ----
    public static final RegistryObject<Item> AMMO_9MM = reg("ammo_9mm", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AMMO_556 = reg("ammo_556", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AMMO_12G = reg("ammo_12g", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AMMO_50CAL = reg("ammo_50cal", () -> new Item(new Item.Properties()));

    // ---- Armas (balance: ajusta aqui) ----
    public static final RegistryObject<GunItem> P9 = reg("p9_pistol", () -> new GunItem(
            new GunStats(AMMO_9MM::get).dmg(5f, 1.8f).rate(4, false).mag(12, 40).acc(0.015f, 0.35f)
                    .recoil(1.3f, 0.3f).ads(1.15f, 3, false).range(50f)
                    .sfx(() -> SoundEvents.FIREWORK_ROCKET_SHOOT, 0.9f, 1.4f)));

    public static final RegistryObject<GunItem> VX9 = reg("vx9_smg", () -> new GunItem(
            new GunStats(AMMO_9MM::get).dmg(3.5f, 1.6f).rate(2, true).mag(30, 45).acc(0.028f, 0.4f)
                    .recoil(0.8f, 0.35f).ads(1.25f, 3, false).range(40f)
                    .sfx(() -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.5f)));

    public static final RegistryObject<GunItem> AR14 = reg("ar14_rifle", () -> new GunItem(
            new GunStats(AMMO_556::get).dmg(5f, 2f).rate(3, true).mag(30, 55).acc(0.02f, 0.25f)
                    .recoil(1.1f, 0.4f).ads(1.4f, 4, false).range(80f)
                    .sfx(() -> SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.0f, 1.0f)));

    public static final RegistryObject<GunItem> M12 = reg("m12_shotgun", () -> new GunItem(
            new GunStats(AMMO_12G::get).dmg(3.5f, 1.5f).rate(18, false).mag(6, 70).acc(0.07f, 0.6f)
                    .recoil(4.5f, 0.5f).ads(1.15f, 4, false).range(24f).pellets(8)
                    .sfx(() -> SoundEvents.GENERIC_EXPLODE, 0.6f, 1.7f)));

    public static final RegistryObject<GunItem> KR50 = reg("kr50_sniper", () -> new GunItem(
            new GunStats(AMMO_50CAL::get).dmg(18f, 2f).rate(25, false).mag(5, 65).acc(0.09f, 0.01f)
                    .recoil(3.5f, 0.3f).ads(4.0f, 7, true).range(150f)
                    .sfx(() -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.7f)));

    // ---- Equipamiento ----
    public static final RegistryObject<GrenadeItem> FRAG = reg("frag_grenade", () -> new GrenadeItem(GrenadeItem.Kind.FRAG));
    public static final RegistryObject<GrenadeItem> FLASH = reg("flashbang", () -> new GrenadeItem(GrenadeItem.Kind.FLASH));
    public static final RegistryObject<GrenadeItem> SMOKE = reg("smoke_grenade", () -> new GrenadeItem(GrenadeItem.Kind.SMOKE));

    private ModItems() {}
}

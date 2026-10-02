package com.frontline.client.anim;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/** Carga assets/frontline/guns/*.json al arrancar y con F3+T. Se indexa por el nombre del item. */
public final class GunModels extends SimpleJsonResourceReloadListener {
    private static final Map<String, GunModelData> MODELS = new HashMap<>();

    public GunModels() {
        super(new Gson(), "guns");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        MODELS.clear();
        for (Map.Entry<ResourceLocation, JsonElement> en : map.entrySet()) {
            try {
                MODELS.put(en.getKey().getPath(), GunModelData.fromJson(en.getValue().getAsJsonObject()));
            } catch (Exception ex) {
                System.err.println("[frontline] Modelo de arma invalido " + en.getKey() + ": " + ex);
            }
        }
    }

    public static GunModelData get(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? null : MODELS.get(key.getPath());
    }
}

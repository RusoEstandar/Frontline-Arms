package com.frontline.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de arma definido por datos (assets/frontline/guns/*.json).
 * Ejes del modelo: +X = hacia el canon, +Y = arriba, +Z = derecha. Unidades de 1/16 de bloque.
 */
public final class GunModelData {
    public static final float[] ZERO3 = {0f, 0f, 0f};

    public static final class Box {
        public final float x0, y0, z0, x1, y1, z1;
        public final int color; // indice en la paleta (textures/item/palette.png)

        Box(float[] a) {
            x0 = a[0]; y0 = a[1]; z0 = a[2]; x1 = a[3]; y1 = a[4]; z1 = a[5];
            color = (int) a[6];
        }
    }

    public static final class Part {
        public final String name;
        public float[] pivot = new float[3];
        public float[] rot = new float[3];
        public float[] lock = null; // desplazamiento fijo con el cargador vacio (corredera atras)
        public final List<Box> boxes = new ArrayList<>();

        Part(String name) { this.name = name; }
    }

    public static final class Track {
        public final List<float[]> pos = new ArrayList<>(); // {t, x, y, z}
        public final List<float[]> rot = new ArrayList<>(); // {t, rx, ry, rz} en grados
    }

    public static final class Anim {
        public float length = 1f;
        public boolean loop;
        public final Map<String, Track> tracks = new HashMap<>();
    }

    public final Map<String, Part> parts = new LinkedHashMap<>();
    public final Map<String, Anim> anims = new HashMap<>();
    public float[] pivot = new float[3];
    public float scale = 0.45f;
    /** Poses de la vista: {x, y, z (metros, espacio camara), rx, ry, rz (grados)}. */
    public float[] hip = new float[6], ads = new float[6], sprint = new float[6];

    public static GunModelData fromJson(JsonObject o) {
        GunModelData d = new GunModelData();
        d.pivot = floats(o.get("pivot"));
        if (o.has("scale")) d.scale = o.get("scale").getAsFloat();
        d.hip = floats(o.get("hip"));
        d.ads = floats(o.get("ads"));
        d.sprint = floats(o.get("sprint"));

        for (Map.Entry<String, JsonElement> en : o.getAsJsonObject("parts").entrySet()) {
            JsonObject po = en.getValue().getAsJsonObject();
            Part part = new Part(en.getKey());
            if (po.has("pivot")) part.pivot = floats(po.get("pivot"));
            if (po.has("rot")) part.rot = floats(po.get("rot"));
            if (po.has("lock")) part.lock = floats(po.get("lock"));
            for (JsonElement b : po.getAsJsonArray("boxes")) part.boxes.add(new Box(floats(b)));
            d.parts.put(en.getKey(), part);
        }

        if (o.has("anims")) {
            for (Map.Entry<String, JsonElement> en : o.getAsJsonObject("anims").entrySet()) {
                JsonObject ao = en.getValue().getAsJsonObject();
                Anim a = new Anim();
                a.length = ao.get("length").getAsFloat();
                a.loop = ao.has("loop") && ao.get("loop").getAsBoolean();
                for (Map.Entry<String, JsonElement> te : ao.getAsJsonObject("tracks").entrySet()) {
                    JsonObject to = te.getValue().getAsJsonObject();
                    Track t = new Track();
                    if (to.has("pos")) for (JsonElement k : to.getAsJsonArray("pos")) t.pos.add(floats(k));
                    if (to.has("rot")) for (JsonElement k : to.getAsJsonArray("rot")) t.rot.add(floats(k));
                    a.tracks.put(te.getKey(), t);
                }
                d.anims.put(en.getKey(), a);
            }
        }
        return d;
    }

    private static float[] floats(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    /** Interpolacion suave (smoothstep) entre fotogramas {t, x, y, z}. */
    public static float[] sample(List<float[]> keys, float t) {
        if (keys == null || keys.isEmpty()) return ZERO3;
        float[] first = keys.get(0);
        if (t <= first[0]) return new float[] {first[1], first[2], first[3]};
        for (int i = 1; i < keys.size(); i++) {
            float[] b = keys.get(i);
            if (t <= b[0]) {
                float[] a = keys.get(i - 1);
                float f = (t - a[0]) / Math.max(1e-4f, b[0] - a[0]);
                f = f * f * (3f - 2f * f);
                return new float[] {
                        a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f, a[3] + (b[3] - a[3]) * f};
            }
        }
        float[] last = keys.get(keys.size() - 1);
        return new float[] {last[1], last[2], last[3]};
    }
}

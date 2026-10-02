package com.frontline.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    public static final KeyMapping RELOAD = new KeyMapping("key.frontline.reload",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.frontline");

    public static final KeyMapping INSPECT = new KeyMapping("key.frontline.inspect",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.frontline");

    private ModKeys() {}
}

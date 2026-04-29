package com.elthisboy.structscript.input;

import com.elthisboy.structscript.network.RotatePacket;
import com.elthisboy.structscript.registry.ModItems;
import com.elthisboy.structscript.util.RotationState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class BlueprintKeyHandler {

    public static KeyBinding rotateKey;

    public static void register() {
        rotateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.structscript.rotate",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.structscript"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ItemStack stack = client.player.getMainHandStack();
            if (!stack.isOf(ModItems.STRUCTURE_BLUEPRINT)) return;
            while (rotateKey.wasPressed()) {
                RotationState.rotate(stack);
                ClientPlayNetworking.send(new RotatePacket(RotationState.get(stack)));
            }
        });
    }
}

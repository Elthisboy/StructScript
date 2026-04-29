package com.elthisboy.structscript.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class CooldownHelper {

    public static boolean isOnCooldown(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        return client.player.getItemCooldownManager().isCoolingDown(stack.getItem());
    }

    public static float getCooldownProgress(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0f;
        return client.player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0f);
    }
}

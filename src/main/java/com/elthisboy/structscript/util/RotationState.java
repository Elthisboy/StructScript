package com.elthisboy.structscript.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class RotationState {

    public static final String ROTATION_KEY = "manualRotation";

    public static int get(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return 0;
        return comp.copyNbt().getInt(ROTATION_KEY);
    }

    public static void set(ItemStack stack, int value) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = comp != null ? comp.copyNbt() : new NbtCompound();
        nbt.putInt(ROTATION_KEY, ((value % 4) + 4) % 4);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void rotate(ItemStack stack) {
        set(stack, get(stack) + 1);
    }
}

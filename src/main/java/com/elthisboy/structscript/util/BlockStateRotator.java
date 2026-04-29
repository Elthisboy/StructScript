package com.elthisboy.structscript.util;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;

public class BlockStateRotator {

    /**
     * Rota un BlockState según la dirección que mira el jugador.
     * NORTH es la dirección base (sin rotación).
     */
    public static BlockState rotate(BlockState state, Direction playerFacing) {
        BlockRotation rotation = switch (playerFacing) {
            case SOUTH -> BlockRotation.CLOCKWISE_180;
            case WEST  -> BlockRotation.CLOCKWISE_90;
            case EAST  -> BlockRotation.COUNTERCLOCKWISE_90;
            default    -> BlockRotation.NONE; // NORTH
        };
        if (rotation == BlockRotation.NONE) return state;
        return state.rotate(rotation);
    }
}

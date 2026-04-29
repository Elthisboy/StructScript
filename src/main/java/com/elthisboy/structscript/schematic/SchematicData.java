package com.elthisboy.structscript.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class SchematicData {

    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    public final BlockState[] blocks;

    public SchematicData(int sizeX, int sizeY, int sizeZ, BlockState[] blocks) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = blocks;
    }

    public BlockState getBlock(int x, int y, int z) {
        int index = (y * sizeZ + z) * sizeX + x;
        if (index >= 0 && index < blocks.length) {
            BlockState state = blocks[index];
            return state != null ? state : net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        return net.minecraft.block.Blocks.AIR.getDefaultState();
    }

    public Map<BlockPos, BlockState> getBlockMap() {
        Map<BlockPos, BlockState> map = new HashMap<>();
        if (this.blocks == null || this.blocks.length == 0) return map;
        for (int y = 0; y < sizeY; y++)
            for (int z = 0; z < sizeZ; z++)
                for (int x = 0; x < sizeX; x++) {
                    BlockState state = getBlock(x, y, z);
                    if (state != null && !state.isAir())
                        map.put(new BlockPos(x, y, z), state);
                }
        return map;
    }
}

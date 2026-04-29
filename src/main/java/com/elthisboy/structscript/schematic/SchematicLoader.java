package com.elthisboy.structscript.schematic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SchematicLoader {

    public static SchematicData load(InputStream stream) {

        try {

            NbtCompound nbt = NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());

            int width = nbt.getShort("Width");
            int height = nbt.getShort("Height");
            int length = nbt.getShort("Length");

            NbtCompound paletteTag = nbt.getCompound("Palette");

            Map<Integer, BlockState> palette = new HashMap<>();

            for (String key : paletteTag.getKeys()) {

                int id = paletteTag.getInt(key);

                Identifier blockId = Identifier.tryParse(key.split("\\[")[0]);

                Block block = Registries.BLOCK.get(blockId);

                palette.put(id, block.getDefaultState());
            }

            byte[] blockData = nbt.getByteArray("BlockData");

            BlockState[] blocks = new BlockState[width * height * length];

            for (int i = 0; i < blockData.length; i++) {

                int paletteIndex = blockData[i] & 0xFF;

                blocks[i] = palette.get(paletteIndex);
            }

            return new SchematicData(width, height, length, blocks);

        } catch (Exception e) {

            e.printStackTrace();

            return new SchematicData(0,0,0,new BlockState[0]);
        }
    }
}
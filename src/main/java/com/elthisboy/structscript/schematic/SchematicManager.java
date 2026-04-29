package com.elthisboy.structscript.schematic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SchematicManager {

    private static final Map<String, SchematicData> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    public static SchematicData get(String name) {
        if (name == null || name.isEmpty()) return null;
        if (CACHE.containsKey(name)) return CACHE.get(name);

        Path base = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("structscript/structures/");

        // Intentar .schem primero, luego .litematic
        Path schemPath     = base.resolve(name + ".schem");
        Path litematicPath = base.resolve(name + ".litematic");

        try {
            if (Files.exists(schemPath)) {
                SchematicData data = loadSchem(schemPath);
                if (data != null) CACHE.put(name, data);
                return data;
            } else if (Files.exists(litematicPath)) {
                SchematicData data = loadLitematic(litematicPath);
                if (data != null) CACHE.put(name, data);
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ── .schem (Sponge v2 / v3) ──────────────────────────────────────────

    private static SchematicData loadSchem(Path path) throws Exception {
        NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        NbtCompound schem = root.contains("Schematic") ? root.getCompound("Schematic") : root;

        int width  = schem.getShort("Width");
        int height = schem.getShort("Height");
        int length = schem.getShort("Length");

        NbtCompound blocksTag = schem.contains("Blocks") ? schem.getCompound("Blocks") : schem;
        NbtCompound paletteTag = blocksTag.getCompound("Palette");

        Map<Integer, BlockState> palette = new HashMap<>();
        for (String key : paletteTag.getKeys()) {
            palette.put(paletteTag.getInt(key), parseBlockState(key));
        }

        byte[] blockData = blocksTag.contains("Data")
                ? blocksTag.getByteArray("Data")
                : blocksTag.getByteArray("BlockData");

        BlockState[] blocks = new BlockState[width * height * length];
        int[] indices = decodeVarIntArray(blockData, blocks.length);
        for (int i = 0; i < blocks.length; i++) {
            int idx = i < indices.length ? indices[i] : 0;
            blocks[i] = palette.getOrDefault(idx, net.minecraft.block.Blocks.AIR.getDefaultState());
        }

        System.out.println("[StructScript] .schem cargado: " + path.getFileName()
                + " (" + width + "x" + height + "x" + length + ")");
        return new SchematicData(width, height, length, blocks);
    }

    // ── .litematic ────────────────────────────────────────────────────────
    // Estructura NBT:
    // root → Regions → <NombreRegion> → BlockStatePalette (NbtList), BlockStates (long[])
    //                                 → Size {x, y, z}

    private static SchematicData loadLitematic(Path path) throws Exception {
        NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());

        NbtCompound regions = root.getCompound("Regions");
        if (regions.isEmpty()) return null;

        // Tomar la primera región (la mayoría de litematics tiene una sola)
        String regionName = regions.getKeys().iterator().next();
        NbtCompound region = regions.getCompound(regionName);

        NbtCompound sizeTag = region.getCompound("Size");
        // En litematic el tamaño puede ser negativo según la dirección de selección
        int sizeX = Math.abs(sizeTag.getInt("x"));
        int sizeY = Math.abs(sizeTag.getInt("y"));
        int sizeZ = Math.abs(sizeTag.getInt("z"));

        // Paleta: NbtList de NbtCompound con "Name" y "Properties"
        NbtList paletteList = region.getList("BlockStatePalette", 10); // 10 = NbtCompound
        BlockState[] palette = new BlockState[paletteList.size()];
        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound entry = paletteList.getCompound(i);
            String name = entry.getString("Name");
            BlockState state = parseBlockState(name);

            if (entry.contains("Properties")) {
                NbtCompound props = entry.getCompound("Properties");
                for (String key : props.getKeys()) {
                    state = applyPropertyByName(state, key, props.getString(key));
                }
            }
            palette[i] = state;
        }

        // BlockStates: array de long[] empaquetado en bits
        long[] blockStates = region.getLongArray("BlockStates");
        int totalBlocks = sizeX * sizeY * sizeZ;
        int bitsPerBlock = Math.max(4, ceilLog2(paletteList.size()));

        BlockState[] blocks = new BlockState[totalBlocks];
        long mask = (1L << bitsPerBlock) - 1L;

        for (int i = 0; i < totalBlocks; i++) {
            int bitIndex  = i * bitsPerBlock;
            int longIndex = bitIndex / 64;
            int bitOffset = bitIndex % 64;

            long value;
            if (bitOffset + bitsPerBlock <= 64) {
                value = (blockStates[longIndex] >> bitOffset) & mask;
            } else {
                // El índice cruza dos longs
                long lo = blockStates[longIndex] >>> bitOffset;
                long hi = (longIndex + 1 < blockStates.length)
                        ? blockStates[longIndex + 1] << (64 - bitOffset)
                        : 0L;
                value = (lo | hi) & mask;
            }

            int paletteIdx = (int) value;
            blocks[i] = (paletteIdx < palette.length)
                    ? palette[paletteIdx]
                    : net.minecraft.block.Blocks.AIR.getDefaultState();
        }

        System.out.println("[StructScript] .litematic cargado: " + path.getFileName()
                + " (" + sizeX + "x" + sizeY + "x" + sizeZ + ") región: " + regionName);
        return new SchematicData(sizeX, sizeY, sizeZ, blocks);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static BlockState parseBlockState(String entry) {
        String blockName = entry.contains("[") ? entry.substring(0, entry.indexOf('[')) : entry;
        Identifier blockId = Identifier.tryParse(blockName);
        if (blockId == null || !Registries.BLOCK.containsId(blockId))
            return net.minecraft.block.Blocks.AIR.getDefaultState();

        Block block = Registries.BLOCK.get(blockId);
        BlockState state = block.getDefaultState();

        if (entry.contains("[")) {
            String propsStr = entry.substring(entry.indexOf('[') + 1, entry.lastIndexOf(']'));
            for (String prop : propsStr.split(",")) {
                String[] kv = prop.split("=");
                if (kv.length != 2) continue;
                state = applyPropertyByName(state, kv[0].trim(), kv[1].trim());
            }
        }
        return state;
    }

    private static BlockState applyPropertyByName(BlockState state, String propName, String propValue) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propName)) {
                Optional<?> value = property.parse(propValue);
                if (value.isPresent())
                    return applyProperty(state, property, value.get());
                break;
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(
            BlockState state, Property<T> property, Object value) {
        return state.with(property, (T) value);
    }

    private static int[] decodeVarIntArray(byte[] data, int expectedCount) {
        int[] result = new int[expectedCount];
        int ri = 0, bi = 0;
        while (bi < data.length && ri < expectedCount) {
            int value = 0, shift = 0;
            byte b;
            do {
                if (bi >= data.length) break;
                b = data[bi++];
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);
            result[ri++] = value;
        }
        return result;
    }

    private static int ceilLog2(int value) {
        if (value <= 1) return 1;
        return 32 - Integer.numberOfLeadingZeros(value - 1);
    }
}
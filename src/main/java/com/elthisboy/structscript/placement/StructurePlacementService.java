package com.elthisboy.structscript.placement;

import com.elthisboy.structscript.schematic.SchematicData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class StructurePlacementService {

    private static final int TICKS_PER_BLOCK = 2;
    private static final int MAX_BLOCKS_PER_TICK = 3;

    public static void place(ServerWorld world, BlockPos hitPos, SchematicData data, Direction facing, int manualRotationOffset) {
        BlockRotation rotation = getRotation(facing, manualRotationOffset);

        boolean is90 = (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90);
        int effSizeX = is90 ? data.sizeZ : data.sizeX;
        int effSizeZ = is90 ? data.sizeX : data.sizeZ;

        int cx = hitPos.getX() - effSizeX / 2;
        int cz = hitPos.getZ() - effSizeZ / 2;
        int fwdX = facing.getOffsetX();
        int fwdZ = facing.getOffsetZ();

        BlockPos origin = new BlockPos(
            cx + fwdX * (effSizeZ / 2),
            hitPos.getY(),
            cz + fwdZ * (effSizeZ / 2)
        );

        List<PlacementEntry> entries = new ArrayList<>();
        for (int y = 0; y < data.sizeY; y++)
            for (int z = 0; z < data.sizeZ; z++)
                for (int x = 0; x < data.sizeX; x++) {
                    BlockState state = data.getBlock(x, y, z);
                    if (state == null || state.isAir()) continue;
                    BlockPos rotatedPos = rotatePos(x, y, z, data.sizeX, data.sizeZ, rotation);
                    entries.add(new PlacementEntry(origin.add(rotatedPos), state.rotate(rotation)));
                }

        world.playSound(null, hitPos,
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.BLOCKS, 0.6f, 0.8f);

        int nextEnd = Math.min(MAX_BLOCKS_PER_TICK, entries.size());
        scheduleBatchDelayed(world, entries, 0, nextEnd, 0);
    }

    private static void scheduleBatchDelayed(
        ServerWorld world,
        List<PlacementEntry> entries,
        int index, int end,
        int delayTicks
    ) {
        final long targetTick = world.getTime() + delayTicks;
        world.getServer().execute(new Runnable() {
            @Override
            public void run() {
                if (world.getTime() < targetTick) {
                    world.getServer().execute(this);
                    return;
                }
                for (int i = index; i < end && i < entries.size(); i++) {
                    PlacementEntry entry = entries.get(i);
                    world.setBlockState(entry.pos(), entry.state(), 3);
                    float pitch = 0.9f + (i % 4) * 0.1f;
                    world.playSound(null, entry.pos(),
                        SoundEvents.BLOCK_STONE_PLACE,
                        SoundCategory.BLOCKS, 0.3f, pitch);
                }
                int nextIndex = end;
                if (nextIndex >= entries.size()) {
                    world.playSound(null, entries.get(0).pos(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.BLOCKS, 0.5f, 1.2f);
                    return;
                }
                int nextEnd = Math.min(nextIndex + MAX_BLOCKS_PER_TICK, entries.size());
                scheduleBatchDelayed(world, entries, nextIndex, nextEnd, TICKS_PER_BLOCK);
            }
        });
    }

    private record PlacementEntry(BlockPos pos, BlockState state) {}

    // público para que StructureBlueprintItem pueda usarlo
    public static BlockPos rotatePos(int x, int y, int z, int sizeX, int sizeZ, BlockRotation rotation) {
        return switch (rotation) {
            case NONE                -> new BlockPos(x, y, z);
            case CLOCKWISE_90        -> new BlockPos(sizeZ - 1 - z, y, x);
            case CLOCKWISE_180       -> new BlockPos(sizeX - 1 - x, y, sizeZ - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, sizeX - 1 - x);
        };
    }

    // público para que StructureBlueprintItem pueda usarlo
    public static BlockRotation getRotation(Direction facing, int offset) {
        return addRotation(toRotation(facing), offset);
    }

    private static BlockRotation addRotation(BlockRotation base, int offset) {
        BlockRotation[] cycle = {
            BlockRotation.NONE,
            BlockRotation.CLOCKWISE_90,
            BlockRotation.CLOCKWISE_180,
            BlockRotation.COUNTERCLOCKWISE_90
        };
        int baseIdx = switch (base) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
        return cycle[(baseIdx + offset) % 4];
    }

    private static BlockRotation toRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> BlockRotation.CLOCKWISE_90;
            case WEST  -> BlockRotation.NONE;
            case SOUTH -> BlockRotation.COUNTERCLOCKWISE_90;
            case EAST  -> BlockRotation.CLOCKWISE_180;
            default    -> BlockRotation.NONE;
        };
    }
}

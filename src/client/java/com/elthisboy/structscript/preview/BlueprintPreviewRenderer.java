package com.elthisboy.structscript.preview;

import com.elthisboy.structscript.item.StructureBlueprintItem;
import com.elthisboy.structscript.registry.ModItems;
import com.elthisboy.structscript.schematic.SchematicData;
import com.elthisboy.structscript.schematic.SchematicManager;
import com.elthisboy.structscript.util.RotationState;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class BlueprintPreviewRenderer {

    private static final BufferAllocator ALLOCATOR = new BufferAllocator(2 * 1024 * 1024);

    private static float getPulse() {
        double time = System.currentTimeMillis() / 1000.0;
        return (float)(0.60 + Math.sin(time * 2.0) * 0.15);
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            ItemStack stack = client.player.getMainHandStack();
            if (!stack.isOf(ModItems.STRUCTURE_BLUEPRINT)) return;

            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (comp == null) return;

            NbtCompound nbt = comp.copyNbt();
            if (!nbt.contains(StructureBlueprintItem.FILE_KEY)) return;

            String structureName = nbt.getString(StructureBlueprintItem.FILE_KEY);
            SchematicData data = SchematicManager.get(structureName);
            if (data == null) return;

            Direction facing = client.player.getHorizontalFacing();
            BlockRotation base = toRotation(facing);
            int manualOffset = RotationState.get(stack);
            BlockRotation rotation = addRotation(base, manualOffset);

            Map<BlockPos, BlockState> rotatedMap = buildRotatedMap(data, rotation);
            if (rotatedMap.isEmpty()) return;

            boolean is90 = (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90);
            int effSizeX = is90 ? data.sizeZ : data.sizeX;
            int effSizeZ = is90 ? data.sizeX : data.sizeZ;

            BlockPos anchorPos;
            if (client.crosshairTarget instanceof BlockHitResult hit) {
                anchorPos = hit.getBlockPos().offset(hit.getSide());
            } else {
                Vec3d pos = client.player.getPos();
                anchorPos = BlockPos.ofFloored(
                    pos.x + facing.getOffsetX() * 8,
                    pos.y,
                    pos.z + facing.getOffsetZ() * 8
                );
            }

            int cx = anchorPos.getX() - effSizeX / 2;
            int cz = anchorPos.getZ() - effSizeZ / 2;
            int fwdX = facing.getOffsetX();
            int fwdZ = facing.getOffsetZ();

            BlockPos originPos = new BlockPos(
                cx + fwdX * (effSizeZ / 2),
                anchorPos.getY(),
                cz + fwdZ * (effSizeZ / 2)
            );

            boolean hasCollision = false;
            for (BlockPos rel : rotatedMap.keySet()) {
                BlockPos worldPos = originPos.add(rel);
                BlockState existing = client.world.getBlockState(worldPos);
                if (!existing.isAir() && !existing.isReplaceable()) {
                    hasCollision = true;
                    break;
                }
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;

            double camX = context.camera().getPos().x;
            double camY = context.camera().getPos().y;
            double camZ = context.camera().getPos().z;

            float pulse = getPulse();

            VertexConsumerProvider.Immediate ownConsumers =
                    VertexConsumerProvider.immediate(ALLOCATOR);

            matrices.push();
            matrices.translate(
                originPos.getX() - camX,
                originPos.getY() - camY,
                originPos.getZ() - camZ
            );

            for (Map.Entry<BlockPos, BlockState> entry : rotatedMap.entrySet()) {
                BlockPos rel = entry.getKey();
                BlockState state = entry.getValue();
                matrices.push();
                matrices.translate(rel.getX(), rel.getY(), rel.getZ());

                VertexConsumer holoConsumer = hasCollision
                    ? new TintedVertexConsumer(
                        ownConsumers.getBuffer(RenderLayer.getTranslucent()),
                        pulse, 1.00f, 0.15f, 0.15f)
                    : new TintedVertexConsumer(
                        ownConsumers.getBuffer(RenderLayer.getTranslucent()),
                        pulse, 0.20f, 0.55f, 1.00f);

                client.getBlockRenderManager().renderBlockAsEntity(
                    state, matrices,
                    layer -> holoConsumer,
                    0xF000F0, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }

            ownConsumers.draw();
            matrices.pop();
        });
    }

    private static class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;
        private final float tR, tG, tB;
        private static final float MIX = 0.25f;

        TintedVertexConsumer(VertexConsumer delegate, float alpha, float tR, float tG, float tB) {
            this.delegate = delegate;
            this.alpha = alpha;
            this.tR = tR; this.tG = tG; this.tB = tB;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            int nr = (int)(r * MIX + 255 * tR * (1 - MIX));
            int ng = (int)(g * MIX + 255 * tG * (1 - MIX));
            int nb = (int)(b * MIX + 255 * tB * (1 - MIX));
            int na = (int)(alpha * 200);
            return delegate.color(nr, ng, nb, na);
        }

        @Override public VertexConsumer vertex(float x, float y, float z)  { return delegate.vertex(x, y, z); }
        @Override public VertexConsumer texture(float u, float v)           { return delegate.texture(u, v); }
        @Override public VertexConsumer overlay(int u, int v)               { return delegate.overlay(u, v); }
        @Override public VertexConsumer light(int u, int v)                 { return delegate.light(u, v); }
        @Override public VertexConsumer normal(float x, float y, float z)   { return delegate.normal(x, y, z); }
    }

    public static BlockRotation addRotation(BlockRotation base, int offset) {
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

    public static Map<BlockPos, BlockState> buildRotatedMap(SchematicData data, BlockRotation rotation) {
        Map<BlockPos, BlockState> map = new HashMap<>();
        for (int y = 0; y < data.sizeY; y++)
            for (int z = 0; z < data.sizeZ; z++)
                for (int x = 0; x < data.sizeX; x++) {
                    BlockState state = data.getBlock(x, y, z);
                    if (state == null || state.isAir()) continue;
                    map.put(rotatePos(x, y, z, data.sizeX, data.sizeZ, rotation),
                            state.rotate(rotation));
                }
        return map;
    }

    private static BlockPos rotatePos(int x, int y, int z, int sizeX, int sizeZ, BlockRotation rotation) {
        return switch (rotation) {
            case NONE                -> new BlockPos(x, y, z);
            case CLOCKWISE_90        -> new BlockPos(sizeZ - 1 - z, y, x);
            case CLOCKWISE_180       -> new BlockPos(sizeX - 1 - x, y, sizeZ - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, sizeX - 1 - x);
        };
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

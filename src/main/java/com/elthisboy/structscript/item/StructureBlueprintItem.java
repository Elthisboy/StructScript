package com.elthisboy.structscript.item;

import com.elthisboy.structscript.placement.StructurePlacementService;
import com.elthisboy.structscript.schematic.SchematicData;
import com.elthisboy.structscript.schematic.SchematicManager;
import com.elthisboy.structscript.util.RotationState;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureBlueprintItem extends Item {

    public static final String FILE_KEY = "file";
    private static final int COOLDOWN_TICKS = 60;
    private static final long CONFIRM_WINDOW_MS = 2000;

    private static final Map<UUID, Long> pendingConfirm = new HashMap<>();

    public StructureBlueprintItem(Settings settings) {
        super(settings);
    }

    public static void setStructure(ItemStack stack, String file) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(FILE_KEY, file);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.translatable("item.structscript.structure_blueprint.named", file));
    }

    public static String getFile(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return null;
        return data.copyNbt().getString(FILE_KEY);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;

        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.FAIL;

        if (player.getItemCooldownManager().isCoolingDown(context.getStack().getItem()))
            return ActionResult.PASS;

        ServerWorld world = (ServerWorld) context.getWorld();
        String file = getFile(context.getStack());
        if (file == null) return ActionResult.FAIL;

        SchematicData data = SchematicManager.get(file);
        if (data == null) {
            ((ServerPlayerEntity) player).sendMessage(
                Text.translatable("message.structscript.file_not_found", file), true);
            return ActionResult.FAIL;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        int rotationOffset = RotationState.get(context.getStack());

        boolean hasCollision = checkCollision(world, pos, data,
            player.getHorizontalFacing(), rotationOffset);

        UUID playerId = player.getUuid();

        if (hasCollision) {
            long now = System.currentTimeMillis();
            Long firstClick = pendingConfirm.get(playerId);

            if (firstClick == null || (now - firstClick) > CONFIRM_WINDOW_MS) {
                pendingConfirm.put(playerId, now);
                ((ServerPlayerEntity) player).sendMessage(
                    Text.translatable("message.structscript.collision_warning"), true);
                return ActionResult.PASS;
            } else {
                pendingConfirm.remove(playerId);
            }
        } else {
            pendingConfirm.remove(playerId);
        }

        StructurePlacementService.place(world, pos, data,
            player.getHorizontalFacing(), rotationOffset);

        player.getItemCooldownManager().set(context.getStack().getItem(), COOLDOWN_TICKS);

        boolean isSurvival = !player.isCreative() && !player.isSpectator();
        if (isSurvival) context.getStack().decrement(1);

        return ActionResult.SUCCESS;
    }

    private boolean checkCollision(ServerWorld world, BlockPos hitPos, SchematicData data,
                                   Direction facing, int rotationOffset) {
        BlockRotation rotation = StructurePlacementService.getRotation(facing, rotationOffset);

        boolean is90 = (rotation == BlockRotation.CLOCKWISE_90
                     || rotation == BlockRotation.COUNTERCLOCKWISE_90);
        int effSizeX = is90 ? data.sizeZ : data.sizeX;
        int effSizeZ = is90 ? data.sizeX : data.sizeZ;

        int cx   = hitPos.getX() - effSizeX / 2;
        int cz   = hitPos.getZ() - effSizeZ / 2;
        int fwdX = facing.getOffsetX();
        int fwdZ = facing.getOffsetZ();

        BlockPos origin = new BlockPos(
            cx + fwdX * (effSizeZ / 2),
            hitPos.getY(),
            cz + fwdZ * (effSizeZ / 2)
        );

        for (int y = 0; y < data.sizeY; y++)
            for (int z = 0; z < data.sizeZ; z++)
                for (int x = 0; x < data.sizeX; x++) {
                    BlockState block = data.getBlock(x, y, z);
                    if (block == null || block.isAir()) continue;
                    BlockPos rotated = StructurePlacementService.rotatePos(
                        x, y, z, data.sizeX, data.sizeZ, rotation);
                    BlockPos worldPos = origin.add(rotated);
                    BlockState existing = world.getBlockState(worldPos);
                    if (!existing.isAir() && !existing.isReplaceable()) return true;
                }
        return false;
    }
}

package com.elthisboy.structscript.network;

import com.elthisboy.structscript.util.RotationState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RotatePacket(int rotation) implements CustomPayload {

    public static final CustomPayload.Id<RotatePacket> ID =
        new CustomPayload.Id<>(Identifier.of("structscript", "rotate"));

    public static final PacketCodec<PacketByteBuf, RotatePacket> CODEC =
        PacketCodec.of(
            (value, buf) -> buf.writeInt(value.rotation()),
            buf -> new RotatePacket(buf.readInt())
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                RotationState.set(stack, payload.rotation());
            });
        });
    }
}

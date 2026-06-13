package com.mcvcofflinewhitelist.auth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom payload for the {@code mcvcofflinewhitelist:auth} plugin message channel.
 *
 * <p>The payload carries a simple pipe-delimited text protocol:
 * {@code type|uuid|username|...}</p>
 */
public record AuthPayload(String text) implements CustomPayload {

    public static final CustomPayload.Id<AuthPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mcvcofflinewhitelist", "auth"));

    /** Codec using Minecraft's length-prefixed string serialisation. */
    public static final PacketCodec<ByteBuf, AuthPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> ((PacketByteBuf) buf).writeString(payload.text()),
                    (buf) -> new AuthPayload(((PacketByteBuf) buf).readString(32767))
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

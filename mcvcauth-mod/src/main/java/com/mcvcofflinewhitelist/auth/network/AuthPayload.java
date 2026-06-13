package com.mcvcofflinewhitelist.auth.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
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

    /** Simple byte-array codec (reads/writes UTF-8 bytes). */
    public static final PacketCodec<ByteBuf, AuthPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBytes(payload.text().getBytes(StandardCharsets.UTF_8)),
                    (buf) -> {
                        String text = buf.toString(StandardCharsets.UTF_8);
                        return new AuthPayload(text);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

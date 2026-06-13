package com.mcvcofflinewhitelist.auth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.nio.charset.StandardCharsets;

/**
 * Custom payload for the {@code mcvcofflinewhitelist:auth} plugin message channel.
 *
 * <p>The payload carries a simple pipe-delimited text protocol:
 * {@code type|uuid|username|...}</p>
 *
 * <p>IMPORTANT: We encode as raw UTF-8 bytes (without a VarInt length prefix)
 * so that Velocity's {@code PluginMessageEvent.getData()} can read the text
 * directly without needing to strip a Minecraft-style length prefix.</p>
 */
public record AuthPayload(String text) implements CustomPayload {

    public static final CustomPayload.Id<AuthPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mcvcofflinewhitelist", "auth"));

    /** Codec using raw UTF-8 bytes (no VarInt length prefix). */
    public static final PacketCodec<ByteBuf, AuthPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBytes(payload.text().getBytes(StandardCharsets.UTF_8)),
                    (buf) -> {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return new AuthPayload(new String(bytes, StandardCharsets.UTF_8));
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

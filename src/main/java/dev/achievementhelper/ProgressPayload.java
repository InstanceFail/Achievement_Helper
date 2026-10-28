package dev.achievementhelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One advancement per packet keeps catalogue synchronization bounded. */
public record ProgressPayload(String json) implements CustomPacketPayload {
    public static final Type<ProgressPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AchievementHelper.ID, "progress"));
    public static final StreamCodec<FriendlyByteBuf, ProgressPayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUtf(value.json(), 262144),
            buf -> new ProgressPayload(buf.readUtf(262144)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

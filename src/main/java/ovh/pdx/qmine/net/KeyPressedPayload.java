package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

public record KeyPressedPayload(boolean pressed) implements CustomPacketPayload {
    public static final Type<KeyPressedPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "key_pressed")
    );

    public static final StreamCodec<FriendlyByteBuf, KeyPressedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KeyPressedPayload::pressed,
            KeyPressedPayload::new
    );

    @Override
    public Type<KeyPressedPayload> type() {
        return TYPE;
    }
}

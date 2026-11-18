package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

public record ModeChangedPayload(boolean next) implements CustomPacketPayload {
    public static final Type<ModeChangedPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "mode_changed")
    );

    public static final StreamCodec<FriendlyByteBuf, ModeChangedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ModeChangedPayload::next,
            ModeChangedPayload::new
    );

    @Override
    public Type<ModeChangedPayload> type() {
        return TYPE;
    }
}

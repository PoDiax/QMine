package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

public record ShapeResizePayload(boolean increase) implements CustomPacketPayload {
    public static final Type<ShapeResizePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "shape_resize")
    );

    public static final StreamCodec<FriendlyByteBuf, ShapeResizePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShapeResizePayload::increase,
            ShapeResizePayload::new
    );

    @Override
    public Type<ShapeResizePayload> type() {
        return TYPE;
    }
}

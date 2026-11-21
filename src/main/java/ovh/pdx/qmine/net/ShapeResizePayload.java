package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ovh.pdx.qmine.compat.IdCompat;

public record ShapeResizePayload(boolean increase) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = IdCompat.of("shape_resize");
    public static final Type<ShapeResizePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ShapeResizePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShapeResizePayload::increase,
            ShapeResizePayload::new
    );

    @Override
    public Type<ShapeResizePayload> type() {
        return TYPE;
    }
}


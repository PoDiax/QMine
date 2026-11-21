package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ovh.pdx.qmine.compat.IdCompat;

public record ModeChangedPayload(boolean next) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = IdCompat.of("mode_changed");
    public static final Type<ModeChangedPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ModeChangedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ModeChangedPayload::next,
            ModeChangedPayload::new
    );

    @Override
    public Type<ModeChangedPayload> type() {
        return TYPE;
    }
}


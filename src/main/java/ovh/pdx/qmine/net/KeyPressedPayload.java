package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ovh.pdx.qmine.compat.IdCompat;

public record KeyPressedPayload(boolean pressed) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = IdCompat.of("key_pressed");
    public static final Type<KeyPressedPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, KeyPressedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KeyPressedPayload::pressed,
            KeyPressedPayload::new
    );

    @Override
    public Type<KeyPressedPayload> type() {
        return TYPE;
    }
}


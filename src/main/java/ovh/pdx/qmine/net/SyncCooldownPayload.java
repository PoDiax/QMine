package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ovh.pdx.qmine.compat.IdCompat;

public record SyncCooldownPayload(long timestamp, CooldownType cooldownType) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = IdCompat.of("sync_cooldown");
    public static final Type<SyncCooldownPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SyncCooldownPayload::timestamp,
            ByteBufCodecs.fromCodec(net.minecraft.util.StringRepresentable.fromEnum(CooldownType::values)), SyncCooldownPayload::cooldownType,
            SyncCooldownPayload::new
    );

    @Override
    public Type<SyncCooldownPayload> type() {
        return TYPE;
    }

    public enum CooldownType implements net.minecraft.util.StringRepresentable {
        COOLDOWN("cooldown"),
        LAST_USED("last_used");

        private final String name;

        CooldownType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}


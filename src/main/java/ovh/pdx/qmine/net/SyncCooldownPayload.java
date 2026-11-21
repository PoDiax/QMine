package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import ovh.pdx.qmine.compat.IdCompat;


public record SyncCooldownPayload(long timestamp, CooldownType cooldownType) {
    public static final ResourceLocation ID = IdCompat.of("sync_cooldown");

    public static SyncCooldownPayload read(FriendlyByteBuf buf) {
        long timestamp = buf.readVarLong();
        CooldownType type = buf.readEnum(CooldownType.class);
        return new SyncCooldownPayload(timestamp, type);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(timestamp);
        buf.writeEnum(cooldownType);
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

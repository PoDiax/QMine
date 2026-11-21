package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import ovh.pdx.qmine.compat.IdCompat;


public record ModeChangedPayload(boolean next) {
    public static final ResourceLocation ID = IdCompat.of("mode_changed");

    public static ModeChangedPayload read(FriendlyByteBuf buf) {
        return new ModeChangedPayload(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(next);
    }
}

package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import ovh.pdx.qmine.compat.IdCompat;


public record KeyPressedPayload(boolean pressed) {
    public static final ResourceLocation ID = IdCompat.of("key_pressed");

    public static KeyPressedPayload read(FriendlyByteBuf buf) {
        return new KeyPressedPayload(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(pressed);
    }
}

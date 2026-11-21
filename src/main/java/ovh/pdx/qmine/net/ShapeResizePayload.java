package ovh.pdx.qmine.net;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import ovh.pdx.qmine.compat.IdCompat;


public record ShapeResizePayload(boolean increase) {
    public static final ResourceLocation ID = IdCompat.of("shape_resize");

    public static ShapeResizePayload read(FriendlyByteBuf buf) {
        return new ShapeResizePayload(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(increase);
    }
}

package ovh.pdx.qmine.shape;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.resources.ResourceLocation;
import ovh.pdx.qmine.compat.IdCompat;
import ovh.pdx.qmine.tag.QMineTags;

public class EscapeTunnelShape extends DiagonalTunnelShape {
    
    public static final ResourceLocation ID = IdCompat.of("escape_tunnel");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.escape_tunnel");
    }

    @Override
    protected int yDirection() {
        return 1;
    }
}

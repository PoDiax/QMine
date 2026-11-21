package ovh.pdx.qmine.shape;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.resources.ResourceLocation;
import ovh.pdx.qmine.compat.IdCompat;
import ovh.pdx.qmine.tag.QMineTags;

public class MiningTunnelShape extends DiagonalTunnelShape {
    
    public static final ResourceLocation ID = IdCompat.of("mining_tunnel");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.translatable("qmine.shape.mining_tunnel");
    }

    @Override
    protected int yDirection() {
        return -1;
    }
}

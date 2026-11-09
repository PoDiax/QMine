package ovh.pdx.qmine.shape;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

public class MiningTunnelShape extends DiagonalTunnelShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "mining_tunnel");

    @Override
    public Identifier getId() {
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

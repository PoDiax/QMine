package ovh.pdx.qmine.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ovh.pdx.qmine.tag.QMineTags;

import java.util.List;
import java.util.Optional;

public record SendShapePayload(int shapeIndex, int shapeRadius, Optional<List<BlockPos>> positions) implements CustomPacketPayload {
    public static final Type<SendShapePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(QMineTags.MOD_ID, "send_shape")
    );

    public static final StreamCodec<FriendlyByteBuf, SendShapePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SendShapePayload::shapeIndex,
            ByteBufCodecs.VAR_INT, SendShapePayload::shapeRadius,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())), SendShapePayload::positions,
            SendShapePayload::new
    );

    public static SendShapePayload ofShapeOnly(int shapeIndex, int shapeRadius) {
        return new SendShapePayload(shapeIndex, shapeRadius, Optional.empty());
    }

    public static SendShapePayload ofShapeAndBlocks(int shapeIndex, int shapeRadius, List<BlockPos> positions) {
        return new SendShapePayload(shapeIndex, shapeRadius, Optional.of(positions));
    }

    @Override
    public Type<SendShapePayload> type() {
        return TYPE;
    }
}

package ovh.pdx.qmine.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.resources.ResourceLocation;

import ovh.pdx.qmine.compat.IdCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public record SendShapePayload(int shapeIndex, int shapeRadius, Optional<List<BlockPos>> positions) {
    public static final ResourceLocation ID = IdCompat.of("send_shape");

    public static SendShapePayload ofShapeOnly(int shapeIndex, int shapeRadius) {
        return new SendShapePayload(shapeIndex, shapeRadius, Optional.empty());
    }

    public static SendShapePayload ofShapeAndBlocks(int shapeIndex, int shapeRadius, List<BlockPos> positions) {
        return new SendShapePayload(shapeIndex, shapeRadius, Optional.of(positions));
    }

    public static SendShapePayload read(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        int radius = buf.readVarInt();
        boolean hasBlocks = buf.readBoolean();
        Optional<List<BlockPos>> positions = Optional.empty();
        if (hasBlocks) {
            int count = buf.readVarInt();
            List<BlockPos> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(buf.readBlockPos());
            }
            positions = Optional.of(list);
        }
        return new SendShapePayload(index, radius, positions);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(shapeIndex);
        buf.writeVarInt(shapeRadius);
        buf.writeBoolean(positions.isPresent());
        if (positions.isPresent()) {
            List<BlockPos> list = positions.get();
            buf.writeVarInt(list.size());
            for (BlockPos p : list) {
                buf.writeBlockPos(p);
            }
        }
    }
}

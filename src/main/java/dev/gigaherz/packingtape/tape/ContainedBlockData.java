package dev.gigaherz.packingtape.tape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record ContainedBlockData(BlockState state, CompoundTag blockEntityTag, Optional<Direction> preferredAll, Optional<Direction> preferredHorizontal)
{
    public static final Codec<ContainedBlockData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BlockState.CODEC.fieldOf("state").forGetter(ContainedBlockData::state),
            CompoundTag.CODEC.fieldOf("blockEntityTag").forGetter(ContainedBlockData::blockEntityTag),
            Direction.CODEC.optionalFieldOf("preferredDirection").forGetter(ContainedBlockData::preferredAll),
            Direction.CODEC.optionalFieldOf("preferredHorizontal").forGetter(ContainedBlockData::preferredAll)
    ).apply(inst, ContainedBlockData::new));

    private static final StreamCodec<ByteBuf, BlockState> BLOCKSTATE_STREAM_CODEC = ByteBufCodecs.fromCodec(BlockState.CODEC);

    public static final StreamCodec<FriendlyByteBuf, ContainedBlockData> STREAM_CODEC = StreamCodec.composite(
            BLOCKSTATE_STREAM_CODEC, ContainedBlockData::state,
            ByteBufCodecs.COMPOUND_TAG, ContainedBlockData::blockEntityTag,
            ByteBufCodecs.optional(Direction.STREAM_CODEC), ContainedBlockData::preferredAll,
            ByteBufCodecs.optional(Direction.STREAM_CODEC), ContainedBlockData::preferredHorizontal,
            ContainedBlockData::new
    );

    public Block getBlock()
    {
        return state.getBlock();
    }
}

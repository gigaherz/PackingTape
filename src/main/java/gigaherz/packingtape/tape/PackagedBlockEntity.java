package gigaherz.packingtape.tape;

import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class PackagedBlockEntity extends BlockEntity
{
    private static Logger LOGGER = LogManager.getLogger();

    private BlockState containedBlockState;
    private CompoundTag containedTile;
    private Direction preferredDirection;

    public PackagedBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public PackagedBlockEntity(BlockPos pos, BlockState state)
    {
        super(PackingTapeMod.PACKAGED_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void saveAdditional(CompoundTag compound)
    {
        super.saveAdditional(compound);

        if (containedBlockState != null)
        {
            CompoundTag blockData = NbtUtils.writeBlockState(containedBlockState);
            compound.put("Block", blockData);
            compound.put("BlockEntity", containedTile.copy());
            if (preferredDirection != null)
            {
                compound.putString("PreferredDirection", preferredDirection.getSerializedName());
            }
        }
    }

    @Override
    public void load(CompoundTag compound)
    {
        super.load(compound);

        HolderGetter<Block> holdergetter = this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup();

        CompoundTag blockTag = compound.getCompound("Block");
        containedBlockState = NbtUtils.readBlockState(holdergetter, blockTag);
        containedTile = compound.getCompound("BlockEntity").copy();
        if (compound.contains("PreferredDirection", Tag.TAG_STRING))
        {
            preferredDirection = Direction.byName(compound.getString("PreferredDirection"));
        }
        else if (compound.contains("PreferredDirection", Tag.TAG_ANY_NUMERIC))
        {
            var values = Direction.values();
            var ordinal = compound.getInt("PreferredDirection");
            if (ordinal >= 0 && ordinal < values.length)
                preferredDirection = values[ordinal];
        }
    }

    public BlockState getContainedBlockState()
    {
        return containedBlockState;
    }

    public CompoundTag getContainedTile()
    {
        return containedTile;
    }

    public void setContents(BlockState state, CompoundTag tag)
    {
        containedBlockState = state;
        containedTile = tag;
    }

    @Nullable
    public Direction getPreferredDirection()
    {
        return preferredDirection;
    }

    public void setPreferredDirection(Direction preferredDirection)
    {
        this.preferredDirection = preferredDirection;
    }

    public ItemStack getPackedStack()
    {
        ItemStack stack = new ItemStack(PackingTapeMod.PACKAGED_BLOCK.get());

        CompoundTag tileEntityData = saveWithoutMetadata();

        CompoundTag stackTag = new CompoundTag();
        stackTag.put("BlockEntityTag", tileEntityData);
        stack.setTag(stackTag);

        LOGGER.debug(String.format("Created Packed stack with %s", containedBlockState.toString()));

        return stack;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        handleUpdateTag(pkt.getTag());
    }

    public boolean isEmpty()
    {
        return containedBlockState == null || containedBlockState.isAir();
    }
}
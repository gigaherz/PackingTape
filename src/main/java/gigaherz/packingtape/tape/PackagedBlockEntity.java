package gigaherz.packingtape.tape;

import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;

public class PackagedBlockEntity extends BlockEntity
{
    @ObjectHolder("packingtape:packaged_block")
    public static BlockEntityType<PackagedBlockEntity> TYPE;

    private BlockState containedBlockState;
    private CompoundTag containedTile;
    private Direction preferredDirection;

    public PackagedBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public PackagedBlockEntity(BlockPos pos, BlockState state)
    {
        super(TYPE, pos, state);
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
                compound.putInt("PreferredDirection", preferredDirection.ordinal());
            }
        }
    }

    @Override
    public void load(CompoundTag compound)
    {
        super.load(compound);

        // Old way.
        if (compound.contains("containedBlock", Tag.TAG_STRING))
        {
            CompoundTag tempTag = new CompoundTag();
            tempTag.putString("Name", compound.getString("containedBlock"));
            tempTag.put("Properties", compound.get("containedBlockState"));
            containedBlockState = NbtUtils.readBlockState(tempTag);
            containedTile = compound.getCompound("containedTile").copy();
            if (compound.contains("preferredDirection"))
            {
                preferredDirection = Direction.values()[compound.getInt("preferredDirection")];
            }
        }
        else
        {
            CompoundTag blockTag = compound.getCompound("Block");
            containedBlockState = NbtUtils.readBlockState(blockTag);
            containedTile = compound.getCompound("BlockEntity").copy();
            if (compound.contains("PreferredDirection"))
            {
                preferredDirection = Direction.byName(compound.getString("PreferredDirection"));
            }
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

        CompoundTag tileEntityData = new CompoundTag();
        save(tileEntityData);
        tileEntityData.remove("x");
        tileEntityData.remove("y");
        tileEntityData.remove("z");

        CompoundTag stackTag = new CompoundTag();
        stackTag.put("BlockEntityTag", tileEntityData);
        stack.setTag(stackTag);

        PackingTapeMod.logger.debug(String.format("Created Packed stack with %s", containedBlockState.toString()));

        return stack;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return save(new CompoundTag());
    }

    //@Nullable
    //@Override
    //public SPacketUpdateTileEntity getUpdatePacket()
    //{
    //    return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    //}

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
package gigaherz.packingtape.tape;

import gigaherz.packingtape.BlockStateNBT;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class TilePackaged extends TileEntity
{
    private IBlockState containedBlockState;
    private NBTTagCompound containedTile;
    private EnumFacing preferredDirection;

    public TilePackaged(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
    }

    public TilePackaged()
    {
        super(ModPackingTape.packaged_block_tile);
    }

    @Override
    public NBTTagCompound write(NBTTagCompound compound)
    {
        compound = super.write(compound);

        if (containedBlockState != null)
        {
            NBTTagCompound blockData = NBTUtil.writeBlockState(containedBlockState);
            compound.put("Block", blockData);
            compound.put("BlockEntity", containedTile.copy());
            if (preferredDirection != null)
            {
                compound.putInt("PreferredDirection", preferredDirection.ordinal());
            }
        }

        return compound;
    }

    @Override
    public void read(NBTTagCompound compound)
    {
        super.read(compound);

        // Old way.
        if (compound.contains("containedBlock", Constants.NBT.TAG_STRING))
        {
            NBTTagCompound tempTag = new NBTTagCompound();
            tempTag.putString("Name", compound.getString("containedBlock"));
            tempTag.put("Properties", compound.get("containedBlockState"));
            containedBlockState = NBTUtil.readBlockState(tempTag);
            containedTile = compound.getCompound("containedTile").copy();
            if (compound.contains("preferredDirection"))
            {
                preferredDirection = EnumFacing.values()[compound.getInt("preferredDirection")];
            }
        }
        else
        {
            NBTTagCompound blockTag = compound.getCompound("Block");
            containedBlockState = NBTUtil.readBlockState(blockTag);
            containedTile = compound.getCompound("BlockEntity").copy();
            if (compound.contains("PreferredDirection"))
            {
                preferredDirection = EnumFacing.byName(compound.getString("PreferredDirection"));
            }
        }
    }

    public IBlockState getContainedBlockState()
    {
        return containedBlockState;
    }

    public NBTTagCompound getContainedTile()
    {
        return containedTile;
    }

    public void setContents(IBlockState state, NBTTagCompound tag)
    {
        containedBlockState = state;
        containedTile = tag;
    }

    @Nullable
    public EnumFacing getPreferredDirection()
    {
        return preferredDirection;
    }

    public void setPreferredDirection(EnumFacing preferredDirection)
    {
        this.preferredDirection = preferredDirection;
    }

    public ItemStack getPackedStack()
    {
        ItemStack stack = new ItemStack(ModPackingTape.packagedBlock);

        NBTTagCompound tileEntityData = new NBTTagCompound();
        write(tileEntityData);
        tileEntityData.remove("x");
        tileEntityData.remove("y");
        tileEntityData.remove("z");

        NBTTagCompound stackTag = new NBTTagCompound();
        stackTag.put("BlockEntityTag", tileEntityData);
        stack.setTag(stackTag);

        ModPackingTape.logger.debug(String.format("Created Packed stack with %s", containedBlockState.toString()));

        return stack;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        return write(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        read(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        handleUpdateTag(pkt.getNbtCompound());
    }
}
package gigaherz.packingtape.tape;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class TilePackaged extends TileEntity
{
    String containedBlock;
    int    containedBlockMetadata;
    NBTTagCompound containedTile;
    EnumFacing preferredDirection;

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
    {
        return (oldState.getBlock() != newSate.getBlock());
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        if(containedBlock != null && containedBlock.length() > 0)
        {
            compound.setString("containedBlock", containedBlock);
            compound.setInteger("containedBlockMetadata", containedBlockMetadata);
            compound.setTag("containedTile", containedTile.copy());
            if(preferredDirection != null)
            {
                compound.setInteger("preferredDirection", preferredDirection.ordinal());
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        containedBlock = compound.getString("containedBlock");
        containedBlockMetadata = compound.getInteger("containedBlockMetadata");
        containedTile = (NBTTagCompound)compound.getCompoundTag("containedTile").copy();
        if(compound.hasKey("preferredDirection"))
        {
            preferredDirection = EnumFacing.values()[compound.getInteger("preferredDirection")];
        }
    }

    public void setContainedBlock(String blockName, int meta, NBTTagCompound tag)
    {
        containedBlock = blockName;
        containedBlockMetadata = meta;
        containedTile = tag;
    }


    public void setPreferredDirection(EnumFacing preferredDirection)
    {
        this.preferredDirection = preferredDirection;
    }

    public EnumFacing getPreferredDirection()
    {
        return preferredDirection;
    }
}
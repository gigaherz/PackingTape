package gigaherz.packingtape.tape;

import gigaherz.packingtape.updatable.IPackedTickHandler;
import gigaherz.packingtape.updatable.PackedUpdateHandlerRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TilePackaged extends TileEntity /* implements ITickable */
{
    ResourceLocation containedBlock;
    int containedBlockMetadata;
    NBTTagCompound containedTile;
    EnumFacing preferredDirection;
    boolean continueUpdating = true;

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
    {
        return (oldState.getBlock() != newSate.getBlock());
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        if (containedBlock != null)
        {
            compound.setString("containedBlock", containedBlock.toString());
            compound.setInteger("containedBlockMetadata", containedBlockMetadata);
            compound.setTag("containedTile", containedTile.copy());
            if (preferredDirection != null)
            {
                compound.setInteger("preferredDirection", preferredDirection.ordinal());
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        containedBlock = new ResourceLocation(compound.getString("containedBlock"));
        containedBlockMetadata = compound.getInteger("containedBlockMetadata");
        containedTile = (NBTTagCompound) compound.getCompoundTag("containedTile").copy();
        if (compound.hasKey("preferredDirection"))
        {
            preferredDirection = EnumFacing.values()[compound.getInteger("preferredDirection")];
        }
    }

    public void setContainedBlock(ResourceLocation blockName, int meta, NBTTagCompound tag)
    {
        containedBlock = blockName;
        containedBlockMetadata = meta;
        containedTile = tag;
    }

    public ResourceLocation getContainedBlock()
    {
        return containedBlock;
    }

    public void setPreferredDirection(EnumFacing preferredDirection)
    {
        this.preferredDirection = preferredDirection;
    }

    public EnumFacing getPreferredDirection()
    {
        return preferredDirection;
    }

    //@Override
    public void update()
    {
        if(!continueUpdating)
            return;

        IPackedTickHandler update = PackedUpdateHandlerRegistry.find(this);
        if(update == null)
        {
            continueUpdating = false;
            return;
        }

        continueUpdating = update.tickPlaced(this);
    }
}
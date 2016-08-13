package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TilePackaged extends TileEntity
{
    private ResourceLocation containedBlock;
    private int containedBlockMetadata;
    private NBTTagCompound containedTile;
    private EnumFacing preferredDirection;

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
    {
        return (oldState.getBlock() != newSate.getBlock());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        compound = super.writeToNBT(compound);

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

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        containedBlock = new ResourceLocation(compound.getString("containedBlock"));
        containedBlockMetadata = compound.getInteger("containedBlockMetadata");
        containedTile = compound.getCompoundTag("containedTile").copy();
        if (compound.hasKey("preferredDirection"))
        {
            preferredDirection = EnumFacing.values()[compound.getInteger("preferredDirection")];
        }
    }

    @Nullable
    public ResourceLocation getContainedBlock()
    {
        return containedBlock;
    }

    public int getContainedMetadata()
    {
        return containedBlockMetadata;
    }

    public NBTTagCompound getContainedTile()
    {
        return containedTile;
    }

    public void setContents(ResourceLocation blockName, int meta, NBTTagCompound tag)
    {
        containedBlock = blockName;
        containedBlockMetadata = meta;
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
        writeToNBT(tileEntityData);
        tileEntityData.removeTag("x");
        tileEntityData.removeTag("y");
        tileEntityData.removeTag("z");

        NBTTagCompound stackTag = new NBTTagCompound();
        stackTag.setTag("BlockEntityTag", tileEntityData);
        stack.setTagCompound(stackTag);

        ModPackingTape.logger.debug("Created Packed stack with " + containedBlock + "[" + containedBlockMetadata + "]");

        return stack;
    }
}
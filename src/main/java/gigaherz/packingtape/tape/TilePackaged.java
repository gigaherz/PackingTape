package gigaherz.packingtape.tape;

import gigaherz.packingtape.BlockStateNBT;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TilePackaged extends TileEntity
{
    private ResourceLocation containedBlock;
    private INBTBase containedBlockState;
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

    //@Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
    {
        return (oldState.getBlock() != newSate.getBlock());
    }

    @Override
    public NBTTagCompound write(NBTTagCompound compound)
    {
        compound = super.write(compound);

        if (containedBlock != null)
        {
            compound.putString("containedBlock", containedBlock.toString());
            compound.put("containedBlockState", containedBlockState);
            compound.put("containedTile", containedTile.copy());
            if (preferredDirection != null)
            {
                compound.putInt("preferredDirection", preferredDirection.ordinal());
            }
        }

        return compound;
    }

    @Override
    public void read(NBTTagCompound compound)
    {
        super.read(compound);

        containedBlock = new ResourceLocation(compound.getString("containedBlock"));
        containedBlockState = compound.get("containedBlockState");
        containedTile = compound.getCompound("containedTile").copy();
        if (compound.contains("preferredDirection"))
        {
            preferredDirection = EnumFacing.values()[compound.getInt("preferredDirection")];
        }
    }

    @Nullable
    public ResourceLocation getContainedBlock()
    {
        return containedBlock;
    }

    public INBTBase getContainedBlockState()
    {
        return containedBlockState;
    }

    public NBTTagCompound getContainedTile()
    {
        return containedTile;
    }

    public void setContents(ResourceLocation blockName, INBTBase propertyData, NBTTagCompound tag)
    {
        containedBlock = blockName;
        containedBlockState = propertyData;
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

        ModPackingTape.logger.debug("Created Packed stack with " + containedBlock + "[" + containedBlockState + "]");

        return stack;
    }

    public IBlockState getParsedBlockState(Block b)
    {
        return BlockStateNBT.decodeBlockState(b, getContainedBlockState());
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
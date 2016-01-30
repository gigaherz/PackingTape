package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class ItemTape extends Item
{
    public ItemTape()
    {
        this.maxStackSize = 16;
        this.setUnlocalizedName(ModPackingTape.MODID + ".packingTape");
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (stack.stackSize == 0)
        {
            return false;
        }

        TileEntity te = worldIn.getTileEntity(pos);

        if (te == null)
        {
            return false;
        }

        if (worldIn.isRemote)
        {
            return true;
        }

        if (!ModPackingTape.isTileEntityAllowed(te))
        {
            return false;
        }

        worldIn.restoringBlockSnapshots = true;

        NBTTagCompound tag = new NBTTagCompound();

        IBlockState state = worldIn.getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation blockName = Block.blockRegistry.getNameForObject(block);
        int meta = block.getMetaFromState(state);

        te.writeToNBT(tag);

        worldIn.setBlockState(pos, ModPackingTape.packagedBlock.getDefaultState());
        TilePackaged packaged = (TilePackaged) worldIn.getTileEntity(pos);
        packaged.setContainedBlock(blockName, meta, tag);

        worldIn.restoringBlockSnapshots = false;

        stack.stackSize--;

        return true;
    }
}
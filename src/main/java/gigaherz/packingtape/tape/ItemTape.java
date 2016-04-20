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
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemTape extends Item
{
    public ItemTape(String name)
    {
        maxStackSize = 16;
        setRegistryName(name);
        setUnlocalizedName(ModPackingTape.MODID + "." + name);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (stack.stackSize == 0)
        {
            return EnumActionResult.PASS;
        }

        TileEntity te = worldIn.getTileEntity(pos);

        if (te == null)
        {
            return EnumActionResult.PASS;
        }

        if (worldIn.isRemote)
        {
            return EnumActionResult.SUCCESS;
        }

        if (!ModPackingTape.isTileEntityAllowed(te))
        {
            return EnumActionResult.PASS;
        }

        worldIn.restoringBlockSnapshots = true;

        NBTTagCompound tag = new NBTTagCompound();

        IBlockState state = worldIn.getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation blockName = Block.REGISTRY.getNameForObject(block);
        int meta = block.getMetaFromState(state);

        te.writeToNBT(tag);

        worldIn.setBlockState(pos, ModPackingTape.packagedBlock.getDefaultState());
        TilePackaged packaged = (TilePackaged) worldIn.getTileEntity(pos);
        packaged.setContents(blockName, meta, tag);

        worldIn.restoringBlockSnapshots = false;

        stack.stackSize--;

        return EnumActionResult.SUCCESS;
    }
}
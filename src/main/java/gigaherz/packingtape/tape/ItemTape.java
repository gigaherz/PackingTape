package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
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
        setMaxDamage(8);
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        return stack.getItemDamage() == 0 ? super.getItemStackLimit(stack) : 1;
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

        NBTTagCompound tag = new NBTTagCompound();

        IBlockState state = worldIn.getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation blockName = Block.REGISTRY.getNameForObject(block);
        int meta = block.getMetaFromState(state);

        te.writeToNBT(tag);

        worldIn.restoringBlockSnapshots = true;
        worldIn.setBlockState(pos, ModPackingTape.packagedBlock.getDefaultState());
        worldIn.restoringBlockSnapshots = false;

        TileEntity te2 = worldIn.getTileEntity(pos);
        if (te2 instanceof TilePackaged)
        {
            TilePackaged packaged = (TilePackaged) te2;

            packaged.setContents(blockName, meta, tag);
        }

        if (!playerIn.capabilities.isCreativeMode)
        {
            if (stack.stackSize > 1)
            {
                ItemStack split = stack.copy();
                split.stackSize = 1;
                split.damageItem(1, playerIn);
                if (split.stackSize > 0)
                {
                    EntityItem ei = new EntityItem(worldIn, playerIn.posX, playerIn.posY, playerIn.posZ, split);
                    worldIn.spawnEntityInWorld(ei);
                }
                stack.stackSize--;
            }
            else
            {
                stack.damageItem(1, playerIn);
            }
        }

        return EnumActionResult.SUCCESS;
    }
}
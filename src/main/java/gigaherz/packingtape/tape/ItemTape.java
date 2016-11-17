package gigaherz.packingtape.tape;

import gigaherz.common.ItemRegistered;
import gigaherz.packingtape.Config;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
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

public class ItemTape extends ItemRegistered
{
    public ItemTape(String name)
    {
        super(name);
        maxStackSize = 16;
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public int getMaxDamage()
    {
        return Config.tapeRollUses;
    }

    @Override
    public boolean isDamageable()
    {
        return true;
    }

    @Override
    public Item setMaxDamage(int maxDamageIn)
    {
        return this;
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        return stack.getItemDamage() == 0 ? super.getItemStackLimit(stack) : 1;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        ItemStack stack = playerIn.getHeldItem(hand);
        if (stack.func_190916_E() <= 0)
        {
            return EnumActionResult.PASS;
        }

        TileEntity te = worldIn.getTileEntity(pos);

        if (te == null)
        {
            return EnumActionResult.PASS;
        }

        if (!playerIn.capabilities.isCreativeMode && !hasPaper(playerIn))
        {
            ModPackingTape.proxy.showPaperMessage();
            return EnumActionResult.FAIL;
        }

        if (worldIn.isRemote)
        {
            return EnumActionResult.SUCCESS;
        }

        if (!Config.isTileEntityAllowed(te))
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
            usePaper(playerIn);

            if (stack.func_190916_E() > 1)
            {
                ItemStack split = stack.copy();
                split.func_190920_e(1);
                split.damageItem(1, playerIn);
                if (split.func_190916_E() > 0)
                {
                    EntityItem ei = new EntityItem(worldIn, playerIn.posX, playerIn.posY, playerIn.posZ, split);
                    worldIn.spawnEntityInWorld(ei);
                }
                stack.func_190917_f(-1);
            }
            else
            {
                stack.damageItem(1, playerIn);
            }
        }

        return EnumActionResult.SUCCESS;
    }

    private boolean hasPaper(EntityPlayer playerIn)
    {
        ItemStack stack = playerIn.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if (stack != null && stack.getItem() == Items.PAPER)
        {
            return true;
        }
        InventoryPlayer inv = playerIn.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.PAPER)
            {
                return true;
            }
        }
        return false;
    }

    private void usePaper(EntityPlayer playerIn)
    {
        ItemStack stack = playerIn.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if (stack != null && stack.getItem() == Items.PAPER)
        {
            stack.func_190917_f(-1);
            if (stack.func_190916_E() <= 0)
                playerIn.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, null);
        }
        InventoryPlayer inv = playerIn.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.PAPER)
            {
                stack.func_190917_f(-1);
                if (stack.func_190916_E() <= 0)
                    inv.setInventorySlotContents(i, null);
                return;
            }
        }
    }
}
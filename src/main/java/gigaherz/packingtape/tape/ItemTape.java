package gigaherz.packingtape.tape;

import gigaherz.packingtape.BlockStateNBT;
import gigaherz.packingtape.Config;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemTape extends Item
{
    public ItemTape(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        return Config.tapeRollUses;
    }

    @Override
    public boolean isDamageable()
    {
        return true;
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        return stack.getDamage() == 0 ? super.getItemStackLimit(stack) : 1;
    }

    @Override
    public EnumActionResult onItemUse(ItemUseContext context)
    {
        EntityPlayer playerIn = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getPos();

        ItemStack stack = context.getItem();
        if (stack.getCount() <= 0)
        {
            return EnumActionResult.PASS;
        }

        IBlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);

        if (te == null)
        {
            return EnumActionResult.PASS;
        }

        if (!playerIn.abilities.isCreativeMode && Config.consumesPaper && !hasPaper(playerIn))
        {
            TextComponentTranslation textComponent = new TextComponentTranslation("text.packingtape.tape.requires_paper");
            playerIn.sendStatusMessage(textComponent, true);
            return EnumActionResult.FAIL;
        }

        if (world.isRemote)
        {
            return EnumActionResult.SUCCESS;
        }

        if (!Config.isTileEntityAllowed(te))
        {
            return EnumActionResult.PASS;
        }

        NBTTagCompound tag = te.write(new NBTTagCompound());

        world.removeTileEntity(pos);
        world.setBlockState(pos, ModPackingTape.packagedBlock.getDefaultState());

        TileEntity te2 = world.getTileEntity(pos);
        if (te2 instanceof TilePackaged)
        {
            TilePackaged packaged = (TilePackaged) te2;

            packaged.setContents(state, tag);
        }

        if (!playerIn.abilities.isCreativeMode)
        {
            if (Config.consumesPaper)
                usePaper(playerIn);

            if (stack.getCount() > 1)
            {
                ItemStack split = stack.copy();
                split.setCount(1);
                split.damageItem(1, playerIn);
                if (split.getCount() > 0)
                {
                    EntityItem ei = new EntityItem(world, playerIn.posX, playerIn.posY, playerIn.posZ, split);
                    world.spawnEntity(ei);
                }
                stack.grow(-1);
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
        if (stack.getItem() == Items.PAPER)
        {
            return true;
        }
        InventoryPlayer inv = playerIn.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            stack = inv.getStackInSlot(i);
            if (stack.getItem() == Items.PAPER)
            {
                return true;
            }
        }
        return false;
    }

    private void usePaper(EntityPlayer playerIn)
    {
        ItemStack stack = playerIn.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        if (stack.getItem() == Items.PAPER)
        {
            stack.grow(-1);
            if (stack.getCount() <= 0)
                playerIn.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        InventoryPlayer inv = playerIn.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            stack = inv.getStackInSlot(i);
            if (stack.getItem() == Items.PAPER)
            {
                stack.grow(-1);
                if (stack.getCount() <= 0)
                    inv.setInventorySlotContents(i, ItemStack.EMPTY);
                return;
            }
        }
    }
}
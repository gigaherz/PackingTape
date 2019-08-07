package gigaherz.packingtape.tape;

import gigaherz.packingtape.Config;
import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

public class TapeItem extends Item
{
    public TapeItem(Properties properties)
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
    public ActionResultType onItemUse(ItemUseContext context)
    {
        PlayerEntity playerIn = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getPos();

        ItemStack stack = context.getItem();
        if (stack.getCount() <= 0)
        {
            return ActionResultType.PASS;
        }

        BlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);

        if (te == null)
        {
            return ActionResultType.PASS;
        }

        if (!playerIn.abilities.isCreativeMode && Config.consumesPaper && !hasPaper(playerIn))
        {
            TranslationTextComponent textComponent = new TranslationTextComponent("text.packingtape.tape.requires_paper");
            playerIn.sendStatusMessage(textComponent, true);
            return ActionResultType.FAIL;
        }

        if (world.isRemote)
        {
            return ActionResultType.SUCCESS;
        }

        if (!Config.isTileEntityAllowed(te))
        {
            return ActionResultType.PASS;
        }

        CompoundNBT tag = te.write(new CompoundNBT());

        world.removeTileEntity(pos);
        world.setBlockState(pos, PackingTapeMod.Blocks.PACKAGED_BLOCK.getDefaultState());

        TileEntity te2 = world.getTileEntity(pos);
        if (te2 instanceof PackagedBlockEntity)
        {
            PackagedBlockEntity packaged = (PackagedBlockEntity) te2;

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
                split.setDamage(split.getDamage()+1);
                if (stack.getDamage() < stack.getMaxDamage())
                {
                    ItemHandlerHelper.giveItemToPlayer(playerIn, split);
                }
                stack.shrink(1);
            }
            else
            {
                stack.setDamage(stack.getDamage()+1);
                if (stack.getDamage() >= stack.getMaxDamage()) {
                    stack.shrink(1);
                }
            }
        }

        return ActionResultType.SUCCESS;
    }

    private boolean hasPaper(PlayerEntity playerIn)
    {
        ItemStack stack = playerIn.getItemStackFromSlot(EquipmentSlotType.OFFHAND);
        if (stack.getItem() == Items.PAPER)
        {
            return true;
        }
        PlayerInventory inv = playerIn.inventory;
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

    private void usePaper(PlayerEntity playerIn)
    {
        ItemStack stack = playerIn.getItemStackFromSlot(EquipmentSlotType.OFFHAND);
        if (stack.getItem() == Items.PAPER)
        {
            stack.grow(-1);
            if (stack.getCount() <= 0)
                playerIn.setItemStackToSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
        }
        PlayerInventory inv = playerIn.inventory;
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
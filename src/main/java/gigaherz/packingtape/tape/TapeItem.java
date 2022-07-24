package gigaherz.packingtape.tape;

import gigaherz.packingtape.ConfigValues;
import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
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
        return ConfigValues.tapeRollUses;
    }

    @Override
    public boolean canBeDepleted()
    {
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack stack)
    {
        return stack.getDamageValue() == 0 ? super.getMaxStackSize(stack) : 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player playerIn = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        ItemStack stack = context.getItemInHand();
        if (stack.getCount() <= 0)
        {
            return InteractionResult.PASS;
        }

        BlockState state = world.getBlockState(pos);
        BlockEntity te = world.getBlockEntity(pos);

        if (te == null)
        {
            return InteractionResult.PASS;
        }

        if (!playerIn.getAbilities().instabuild && ConfigValues.consumesPaper && !hasPaper(playerIn))
        {
            var textComponent = Component.translatable("text.packingtape.tape.requires_paper");
            playerIn.displayClientMessage(textComponent, true);
            return InteractionResult.FAIL;
        }

        if (world.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        if (ConfigValues.isTileEntityBlocked(te))
        {
            return InteractionResult.PASS;
        }

        if (state.getBlock() instanceof ChestBlock)
        {
            if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
            {
                state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
            }
        }

        CompoundTag tag = te.saveWithoutMetadata();

        world.removeBlockEntity(pos);
        world.setBlockAndUpdate(pos, PackingTapeMod.PACKAGED_BLOCK.get().defaultBlockState());

        BlockEntity te2 = world.getBlockEntity(pos);
        if (te2 instanceof PackagedBlockEntity packaged)
        {
            packaged.setContents(state, tag);
        }

        if (!playerIn.getAbilities().instabuild)
        {
            if (ConfigValues.consumesPaper)
                usePaper(playerIn);

            if (stack.getCount() > 1)
            {
                ItemStack split = stack.copy();
                split.setCount(1);
                split.setDamageValue(split.getDamageValue() + 1);
                if (stack.getDamageValue() < stack.getMaxDamage())
                {
                    ItemHandlerHelper.giveItemToPlayer(playerIn, split);
                }
                stack.shrink(1);
            }
            else
            {
                stack.setDamageValue(stack.getDamageValue() + 1);
                if (stack.getDamageValue() >= stack.getMaxDamage())
                {
                    stack.shrink(1);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private boolean hasPaper(Player playerIn)
    {
        ItemStack stack = playerIn.getItemBySlot(EquipmentSlot.OFFHAND);
        if (stack.getItem() == Items.PAPER)
        {
            return true;
        }
        Inventory inv = playerIn.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            stack = inv.getItem(i);
            if (stack.getItem() == Items.PAPER)
            {
                return true;
            }
        }
        return false;
    }

    private void usePaper(Player playerIn)
    {
        ItemStack stack = playerIn.getItemBySlot(EquipmentSlot.OFFHAND);
        if (stack.getItem() == Items.PAPER)
        {
            stack.grow(-1);
            if (stack.getCount() <= 0)
                playerIn.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        Inventory inv = playerIn.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            stack = inv.getItem(i);
            if (stack.getItem() == Items.PAPER)
            {
                stack.grow(-1);
                if (stack.getCount() <= 0)
                    inv.setItem(i, ItemStack.EMPTY);
                return;
            }
        }
    }
}
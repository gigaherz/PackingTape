package dev.gigaherz.packingtape.tape;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class PackagedBlockItem extends BlockItem
{

    public PackagedBlockItem(PackagedBlock block, Properties properties)
    {
        super(block, properties);
    }

    @Deprecated
    @Override
    public void appendHoverText(ItemStack p_41421_, TooltipContext p_339594_, TooltipDisplay p_399753_, Consumer<Component> p_399884_, TooltipFlag p_41424_)
    {
        ((PackagedBlock) getBlock()).appendHoverText(p_41421_, p_399884_);
    }
}

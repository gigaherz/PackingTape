package gigaherz.packingtape.updatable;

import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PackedTickHelper implements IPackedTickHandler
{
    @Override
    public boolean tickPlaced(TilePackaged container)
    {
        return false;
    }

    @Override
    public boolean tickAsEntity(EntityItem entityItem)
    {
        return false;
    }

    @Override
    public void tickInInventory(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {

    }
}

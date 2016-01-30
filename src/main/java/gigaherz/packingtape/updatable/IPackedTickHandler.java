package gigaherz.packingtape.updatable;

import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IPackedTickHandler
{
    boolean tickPlaced(TilePackaged container);
    boolean tickAsEntity(EntityItem entityItem);
    void tickInInventory(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected);
}

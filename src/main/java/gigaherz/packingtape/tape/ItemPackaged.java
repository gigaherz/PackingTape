package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.List;

public class ItemPackaged extends ItemBlock
{
    public ItemPackaged(Block b)
    {
        super(b);
        this.setUnlocalizedName(ModPackingTape.MODID + ".packedBlock");
    }

    @Override
    public int getMetadata(int damage)
    {
        return damage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced)
    {
        super.addInformation(stack, playerIn, tooltip, advanced);

        List<String> tooltips = (List<String>)tooltip;

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound info = (NBTTagCompound) tag.getTag("BlockEntityTag");

        String blockname = info.getString("containedBlock");
        int meta = info.getInteger("containedBlockMetadata");

        tooltips.add("Contains: " + blockname + "[" + meta + "]");

    }
}

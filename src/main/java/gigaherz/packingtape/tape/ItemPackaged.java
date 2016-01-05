package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

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

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced)
    {
        super.addInformation(stack, playerIn, tooltip, advanced);

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound info = (NBTTagCompound) tag.getTag("BlockEntityTag");

        String blockname = info.getString("containedBlock");
        int meta = info.getInteger("containedBlockMetadata");

        tooltip.add("Contains:");

        Block block = Block.blockRegistry.getObject(new ResourceLocation(blockname));
        ItemStack stack1 = new ItemStack(Item.getItemFromBlock(block), 1, meta);

        for (String s : stack1.getTooltip(playerIn, advanced))
        {
            tooltip.add("  " + s);
        }
    }
}

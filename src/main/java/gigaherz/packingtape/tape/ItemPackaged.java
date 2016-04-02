package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.List;

public class ItemPackaged extends ItemBlock
{
    public ItemPackaged(Block b)
    {
        super(b);
        setRegistryName(b.getRegistryName());
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
        if(tag == null)
        {
            tooltip.add("Missing data (no nbt)!");
            return;
        }

        NBTTagCompound info = (NBTTagCompound) tag.getTag("BlockEntityTag");
        if(info == null)
        {
            tooltip.add("Missing data (no tag)!");
            return;
        }


        if (!info.hasKey("containedBlock", Constants.NBT.TAG_STRING) ||
            !info.hasKey("containedBlockMetadata", Constants.NBT.TAG_INT) ||
            !info.hasKey("containedTile", Constants.NBT.TAG_COMPOUND))
        {
            tooltip.add("Missing data (no block info)!");
            return;
        }

        String blockname = info.getString("containedBlock");
        int meta = info.getInteger("containedBlockMetadata");

        Block block = Block.blockRegistry.getObject(new ResourceLocation(blockname));
        if(block == null)
        {
            tooltip.add("Unknown block:");
            tooltip.add("  " + blockname);
            return;
        }

        Item item = Item.getItemFromBlock(block);
        if(item == null)
        {
            tooltip.add("No ItemBlock:");
            tooltip.add("  " + blockname);
            return;
        }

        tooltip.add("Contains:");
        ItemStack stack1 = new ItemStack(item, 1, meta);
        for (String s : stack1.getTooltip(playerIn, advanced))
        {
            tooltip.add("  " + s);
        }
    }
}

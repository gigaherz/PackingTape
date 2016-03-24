package gigaherz.packingtape.client;

import gigaherz.packingtape.ISideProxy;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;

public class ClientProxy implements ISideProxy
{

    @Override
    public void preInit()
    {
        registerModels();
    }

    @Override
    public void init()
    {
    }

    private void registerModels()
    {
        registerItemModel(ModPackingTape.itemTape, "packing_tape");
        registerBlockModelAsItem(ModPackingTape.packagedBlock, "packagedBlock");
    }

    public void registerBlockModelAsItem(final Block block, final String blockName)
    {
        registerBlockModelAsItem(block, 0, blockName);
    }

    public void registerBlockModelAsItem(final Block block, int meta, final String blockName)
    {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), meta, new ModelResourceLocation(ModPackingTape.MODID + ":" + blockName, "inventory"));
    }

    public void registerItemModel(final Item item, final String itemName)
    {
        registerItemModel(item, 0, itemName);
    }

    public void registerItemModel(final Item item, int meta, final String itemName)
    {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(ModPackingTape.MODID + ":" + itemName, "inventory"));
    }
}

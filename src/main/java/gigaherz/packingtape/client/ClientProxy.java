package gigaherz.packingtape.client;

import gigaherz.packingtape.ISideProxy;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
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

    @Override
    public void showPaperMessage()
    {
        Minecraft.getMinecraft().ingameGUI.setRecordPlaying(I18n.format("text." + ModPackingTape.MODID + ".itemTape.requiresPaper"), false);
    }

    private void registerModels()
    {
        registerItemModel(ModPackingTape.itemTape);
        registerItemModel(Item.getItemFromBlock(ModPackingTape.packagedBlock));
    }

    public void registerItemModel(final Item item)
    {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}

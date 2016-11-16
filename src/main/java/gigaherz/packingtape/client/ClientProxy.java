package gigaherz.packingtape.client;

import gigaherz.packingtape.ISideProxy;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ClientProxy implements ISideProxy
{
    @Override
    public void showPaperMessage()
    {
        Minecraft.getMinecraft().ingameGUI.setRecordPlaying(I18n.format("text." + ModPackingTape.MODID + ".itemTape.requiresPaper"), false);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerItemModel(ModPackingTape.itemTape);
        registerItemModel(Item.getItemFromBlock(ModPackingTape.packagedBlock));
    }

    private static void registerItemModel(final Item item)
    {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}

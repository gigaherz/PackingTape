package gigaherz.packingtape.client;

import gigaherz.packingtape.ISideProxy;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import static gigaherz.common.client.ModelHelpers.registerBlockModelAsItem;
import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy implements ISideProxy
{
    @Override
    public void showPaperMessage()
    {
        Minecraft.getMinecraft().ingameGUI.setOverlayMessage(I18n.format("text." + ModPackingTape.MODID + ".tape.requires_paper"), false);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerItemModel(ModPackingTape.itemTape);
        registerBlockModelAsItem(ModPackingTape.packagedBlock);
    }
}

package gigaherz.packingtape.client;

import gigaherz.packingtape.ModPackingTape;
import gigaherz.packingtape.tape.BlockPackaged;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import static gigaherz.common.client.ModelHelpers.registerBlockModelAsItem;
import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ModelsEventHandler
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerItemModel(ModPackingTape.itemTape);
        registerBlockModelAsItem(ModPackingTape.packagedBlock);
        ModelLoader.setCustomStateMapper(ModPackingTape.packagedBlock, (new StateMap.Builder()).ignore(BlockPackaged.UNPACKING).build());
    }
}

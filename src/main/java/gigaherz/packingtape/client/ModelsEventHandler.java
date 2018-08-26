package gigaherz.packingtape.client;

import gigaherz.packingtape.ModPackingTape;
import gigaherz.packingtape.tape.BlockPackaged;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value=Side.CLIENT, modid=ModPackingTape.MODID)
public class ModelsEventHandler
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        ModelLoader.setCustomModelResourceLocation(ModPackingTape.itemTape, 0,
                new ModelResourceLocation(ModPackingTape.itemTape.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(ModPackingTape.packagedBlock), 0,
                new ModelResourceLocation(ModPackingTape.packagedBlock.getRegistryName(), "inventory"));
        ModelLoader.setCustomStateMapper(ModPackingTape.packagedBlock, (new StateMap.Builder()).ignore(BlockPackaged.UNPACKING).build());
    }
}

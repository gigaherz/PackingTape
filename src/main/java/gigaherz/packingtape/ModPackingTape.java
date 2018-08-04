package gigaherz.packingtape;

import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod.EventBusSubscriber
@Mod(modid = ModPackingTape.MODID,
        acceptedMinecraftVersions = "[1.12.0,1.13.0)")
public class ModPackingTape
{
    public static final String MODID = "packingtape";

    @GameRegistry.ObjectHolder(MODID + ":packaged_block")
    public static BlockPackaged packagedBlock;

    @GameRegistry.ObjectHolder(MODID + ":tape")
    public static Item itemTape;

    @Mod.Instance(value = ModPackingTape.MODID)
    public static ModPackingTape instance;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        File configurationFile = event.getSuggestedConfigurationFile();
        Configuration config = new Configuration(configurationFile);
        Config.loadConfig(config);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                new BlockPackaged("packaged_block")
        );

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        GameRegistry.registerTileEntity(TilePackaged.class, packagedBlock.getRegistryName());

        event.getRegistry().registerAll(
                packagedBlock.createItemBlock(),

                new ItemTape("tape")
        );
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}

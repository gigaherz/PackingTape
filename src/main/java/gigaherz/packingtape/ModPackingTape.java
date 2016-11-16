package gigaherz.packingtape;

import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod.EventBusSubscriber
@Mod(modid = ModPackingTape.MODID,
        version = ModPackingTape.VERSION,
        acceptedMinecraftVersions = "[1.9.4,1.11.0)",
        dependencies = "required-after:Forge@[12.16.0.1825,)")
public class ModPackingTape
{
    public static final String MODID = "packingtape";
    public static final String VERSION = "@VERSION@";

    public static BlockPackaged packagedBlock;
    public static Item itemTape;

    @Mod.Instance(value = ModPackingTape.MODID)
    public static ModPackingTape instance;

    @SidedProxy(clientSide = "gigaherz.packingtape.client.ClientProxy", serverSide = "gigaherz.packingtape.server.ServerProxy")
    public static ISideProxy proxy;

    public static Logger logger;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                packagedBlock = new BlockPackaged("packagedBlock")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                packagedBlock.createItemBlock(),

                itemTape = new ItemTape("itemTape")
        );
    }

    public static void registerTileEntities()
    {
        GameRegistry.registerTileEntity(TilePackaged.class, "tilePackagedBlock");
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        registerTileEntities();

        File configurationFile = event.getSuggestedConfigurationFile();
        Configuration config = new Configuration(configurationFile);
        Config.loadConfig(config);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        GameRegistry.addShapelessRecipe(new ItemStack(itemTape, 1), Items.SLIME_BALL, Items.STRING, Items.PAPER);
    }
}

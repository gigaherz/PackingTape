package gigaherz.packingtape;

import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod.EventBusSubscriber
@Mod(modid = ModPackingTape.MODID,
        acceptedMinecraftVersions = "[1.12.0,1.13.0)")
public class ModPackingTape
{
    public static final String MODID = "packingtape";

    @ObjectHolder(MODID + ":packaged_block")
    public static Block packagedBlock;

    @ObjectHolder(MODID + ":tape")
    public static Item itemTape;

    @Mod.Instance(value = ModPackingTape.MODID)
    public static ModPackingTape instance;

    public static Logger logger = LogManager.getLogger(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        File configurationFile = event.getSuggestedConfigurationFile();
        Configuration config = new Configuration(configurationFile);
        Config.loadConfig(config);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                withName(new BlockPackaged(), "packaged_block")
        );

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        GameRegistry.registerTileEntity(TilePackaged.class, packagedBlock.getRegistryName());

        event.getRegistry().registerAll(
                forBlock(packagedBlock),

                withName(new ItemTape(), "tape").setMaxStackSize(16).setCreativeTab(CreativeTabs.MISC)
        );
    }

    private static Item withName(Item item, String name)
    {
        return item.setRegistryName(name).setTranslationKey(MODID + "." + name);
    }

    private static Block withName(Block block, String name)
    {
        return block.setRegistryName(name).setTranslationKey(MODID + "." + name);
    }

    private static Item forBlock(Block block)
    {
        return new ItemBlock(block).setRegistryName(block.getRegistryName());
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}

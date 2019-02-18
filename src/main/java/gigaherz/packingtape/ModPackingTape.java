package gigaherz.packingtape;

import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModPackingTape.MODID)
public class ModPackingTape
{
    public static final String MODID = "packingtape";

    @ObjectHolder(MODID + ":packaged_block")
    public static Block packagedBlock;

    @ObjectHolder(MODID + ":tape")
    public static Item itemTape;

    @ObjectHolder(MODID + ":packaged_block")
    public static Item packagedBlockItem;

    @ObjectHolder(MODID + ":packaged_block")
    public static TileEntityType<TilePackaged> packaged_block_tile;

    public static ModPackingTape instance;

    public static Logger logger = LogManager.getLogger(MODID);

    public ModPackingTape()
    {
        // no @Instance anymore
        instance = this;

        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Block.class, this::registerBlocks);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, this::registerItems);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(TileEntityType.class, this::registerTileEntities);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverConfig);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    public void serverConfig(ModConfig.ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == Config.SERVER_SPEC)
            Config.load();
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                packagedBlock = new BlockPackaged().setRegistryName(location("packaged_block"))
        );
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                packagedBlockItem = new ItemBlock(packagedBlock, new Item.Properties()).setRegistryName(packagedBlock.getRegistryName()),
                itemTape = new ItemTape(new Item.Properties().maxStackSize(16).group(ItemGroup.MISC)).setRegistryName(location("tape"))
        );

        Item.BLOCK_TO_ITEM.put(packagedBlock, packagedBlockItem);
    }

    @SubscribeEvent
    public void registerTileEntities(RegistryEvent.Register<TileEntityType<?>> event)
    {
        packaged_block_tile = TileEntityType.register(packagedBlock.getRegistryName().toString(), TileEntityType.Builder.create(TilePackaged::new));
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}

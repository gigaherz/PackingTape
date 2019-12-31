package gigaherz.packingtape;

import gigaherz.packingtape.tape.PackagedBlock;
import gigaherz.packingtape.tape.TapeItem;
import gigaherz.packingtape.tape.PackagedBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod(PackingTapeMod.MODID)
public class PackingTapeMod
{
    public static final String MODID = "packingtape";

    @SuppressWarnings("ConstantConditions")
    @Nonnull
    private static <T> T willBeInitializedAtRuntime() {
        return null;
    }

    @ObjectHolder(MODID)
    public static class Blocks
    {
        public static final Block PACKAGED_BLOCK = willBeInitializedAtRuntime();
    }

    @ObjectHolder(MODID)
    public static class Items
    {
        public static final Item TAPE = willBeInitializedAtRuntime();
    }

    public static Logger logger = LogManager.getLogger(MODID);

    public PackingTapeMod()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Block.class, this::registerBlocks);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, this::registerItems);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(TileEntityType.class, this::registerTileEntities);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverConfig);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    public void serverConfig(ModConfig.ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == Config.SERVER_SPEC)
            Config.bake();
    }

    public void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                new PackagedBlock(Block.Properties.create(Material.WOOL).hardnessAndResistance(0.5f, 0.5f).sound(SoundType.WOOD)).setRegistryName(location("packaged_block"))
        );
    }

    public void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                new BlockItem(Blocks.PACKAGED_BLOCK, new Item.Properties()).setRegistryName(Blocks.PACKAGED_BLOCK.getRegistryName()),
                new TapeItem(new Item.Properties().maxStackSize(16).group(ItemGroup.MISC)).setRegistryName("tape")
        );
    }

    public void registerTileEntities(RegistryEvent.Register<TileEntityType<?>> event)
    {
        event.getRegistry().registerAll(
            TileEntityType.Builder.create(PackagedBlockEntity::new, Blocks.PACKAGED_BLOCK).build(null).setRegistryName(Blocks.PACKAGED_BLOCK.getRegistryName())
        );
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}

package gigaherz.packingtape;

import gigaherz.packingtape.tape.PackagedBlock;
import gigaherz.packingtape.tape.PackagedBlockEntity;
import gigaherz.packingtape.tape.TapeItem;
import gigaherz.packingtape.util.MiniReg;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(PackingTapeMod.MODID)
public class PackingTapeMod
{
    public static final String MODID = "packingtape";

    private static final MiniReg HELPER = new MiniReg(MODID);

    public static final RegistryObject<PackagedBlock> PACKAGED_BLOCK = HELPER.block("packaged_block", () ->
                new PackagedBlock(Block.Properties.create(Material.WOOL).hardnessAndResistance(0.5f, 0.5f).sound(SoundType.WOOD)))
            .withItem()
            .withTileEntity((Supplier<PackagedBlockEntity>) PackagedBlockEntity::new)
            .defer();

    public static final RegistryObject<TapeItem> TAPE = HELPER.item("tape", () -> new TapeItem(new Item.Properties().maxStackSize(16).group(ItemGroup.MISC))).defer();

    public static Logger logger = LogManager.getLogger(MODID);

    public PackingTapeMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        HELPER.subscribeEvents(modEventBus);

        modEventBus.addListener(this::serverConfig);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    public void serverConfig(ModConfig.ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == Config.SERVER_SPEC)
            Config.bake();
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}

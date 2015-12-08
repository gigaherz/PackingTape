package gigaherz.packingtape;

import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Mod(modid = ModPackingTape.MODID, name = ModPackingTape.MODNAME, version = ModPackingTape.VERSION)
public class ModPackingTape
{
    public static final String MODID = "packingtape";
    public static final String MODNAME = "Packing Tape";
    public static final String VERSION = "1.0";

    public static Block packagedBlock;
    public static Item itemTape;

    @Mod.Instance(value = ModPackingTape.MODID)
    public static ModPackingTape instance;

    @SidedProxy(clientSide = "gigaherz.packingtape.client.ClientProxy", serverSide = "gigaherz.packingtape.server.ServerProxy")
    public static ISideProxy proxy;

    public static boolean blacklistMode = true;
    public static Set<String> whiteBlackList = new HashSet<String>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        blacklistMode = !config.get("blacklist", "whitelistMode", false).getBoolean();
        Collections.addAll(whiteBlackList, config.get("blacklist", "tileEntityNames", new String[0]).getStringList());
        config.save();

        itemTape = new ItemTape();
        GameRegistry.registerItem(itemTape, "itemTape");

        packagedBlock = new BlockPackaged().setHardness(0.5F).setStepSound(Block.soundTypeWood);
        GameRegistry.registerBlock(packagedBlock, ItemPackaged.class, "packagedBlock");

        GameRegistry.registerTileEntity(TilePackaged.class, "tilePackagedBlock");

        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();

        GameRegistry.addShapelessRecipe(new ItemStack(itemTape, 1), Items.slime_ball, Items.string, Items.paper);
    }

    public boolean checkWhitelist(TileEntity te)
    {
        boolean present = whiteBlackList.contains(te.getClass().getName());
        return (blacklistMode) ? !present : present;
    }
}

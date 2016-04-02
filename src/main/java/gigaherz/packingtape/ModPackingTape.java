package gigaherz.packingtape;

import com.google.common.collect.Sets;
import gigaherz.packingtape.tape.BlockPackaged;
import gigaherz.packingtape.tape.ItemPackaged;
import gigaherz.packingtape.tape.ItemTape;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Mod(modid = ModPackingTape.MODID, version = ModPackingTape.VERSION)
public class ModPackingTape
{
    public static final String MODID = "packingtape";
    public static final String VERSION = "@VERSION@";

    public static final Set<String> blackList = Sets.newHashSet();
    public static final Set<String> whiteList = Sets.newHashSet();

    public static Block packagedBlock;
    public static Item itemTape;

    @Mod.Instance(value = ModPackingTape.MODID)
    public static ModPackingTape instance;

    @SidedProxy(clientSide = "gigaherz.packingtape.client.ClientProxy", serverSide = "gigaherz.packingtape.server.ServerProxy")
    public static ISideProxy proxy;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        Property bl = config.get("tileEntities", "blacklist", new String[0]);
        Property wl = config.get("tileEntities", "whitelist", new String[0]);

        config.load();
        Collections.addAll(blackList, bl.getStringList());
        Collections.addAll(whiteList, wl.getStringList());
        config.save();

        itemTape = new ItemTape("itemTape");
        GameRegistry.registerItem(itemTape);

        packagedBlock = new BlockPackaged("packagedBlock").setHardness(0.5F);
        GameRegistry.registerBlock(packagedBlock, ItemPackaged.class);

        GameRegistry.registerTileEntity(TilePackaged.class, "tilePackagedBlock");

        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();

        GameRegistry.addShapelessRecipe(new ItemStack(itemTape, 1), Items.slime_ball, Items.string, Items.paper);
    }

    public static boolean isTileEntityAllowed(TileEntity te)
    {
        String cn = te.getClass().getCanonicalName();

        if (whiteList.contains(cn))
            return true;

        if (blackList.contains(cn))
            return false;

        // Security concern: moving command blocks may allow things to happen that shouldn't happen.
        if(te.getClass().equals(net.minecraft.tileentity.TileEntityCommandBlock.class))
            return false;

        // Security/gameplay concern: Moving end portal blocks could cause issues.
        if(te.getClass().equals(net.minecraft.tileentity.TileEntityEndPortal.class))
            return false;

        // Balance concern: moving block spawners can be cheaty and should be reserved to hard-to-obtain methods.
        if(te.getClass().equals(net.minecraft.tileentity.TileEntityMobSpawner.class))
            return false;

        // Placed skulls don't have an ItemBlock form, and can be moved away easily regardless.
        if(te.getClass().equals(net.minecraft.tileentity.TileEntitySkull.class))
            return false;

        // Was this also a security concern?
        if(te.getClass().equals(net.minecraft.tileentity.TileEntitySign.class))
            return false;

        // The rest: There's no point to packing them.
        if(te.getClass().equals(net.minecraft.tileentity.TileEntityBanner.class))
            return false;

        if(te.getClass().equals(net.minecraft.tileentity.TileEntityComparator.class))
            return false;

        if(te.getClass().equals(net.minecraft.tileentity.TileEntityDaylightDetector.class))
            return false;

        if(te.getClass().equals(net.minecraft.tileentity.TileEntityPiston.class))
            return false;

        if(te.getClass().equals(net.minecraft.tileentity.TileEntityNote.class))
            return false;


        // TODO: Blacklist more Vanilla stuffs.

        return true;
    }
}

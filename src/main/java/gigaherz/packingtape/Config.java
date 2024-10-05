package gigaherz.packingtape;

import com.google.common.collect.Sets;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.Collections;
import java.util.Set;

public class Config
{
    private static final Set<String> blackList = Sets.newHashSet();
    private static final Set<String> whiteList = Sets.newHashSet();

    public static int tapeRollUses;
    public static boolean consumesPaper;

    static void loadConfig(Configuration config)
    {
        Property bl = config.get("tileEntities", "blacklist", new String[0]);
        Property wl = config.get("tileEntities", "whitelist", new String[0]);
        Property ru = config.get("tapeRoll", "numberOfUses", 8);
        Property cp = config.get("tapeRoll", "consumesPaper", true);

        Collections.addAll(blackList, bl.getStringList());
        Collections.addAll(whiteList, wl.getStringList());
        tapeRollUses = ru.getInt();
        consumesPaper = cp.getBoolean();
        if (!bl.wasRead() || !wl.wasRead() || !ru.wasRead() || !cp.wasRead())
            config.save();
    }

    public static boolean isTileEntityAllowed(TileEntity te)
    {
        Class<? extends TileEntity> teClass = te.getClass();

        String cn = teClass.getCanonicalName();

        if (whiteList.contains(cn))
            return true;

        if (blackList.contains(cn))
            return false;

        // Security concern: moving command blocks may allow things to happen that shouldn't happen.
        if (net.minecraft.tileentity.TileEntityCommandBlock.class.isAssignableFrom(teClass))
            return false;

        // Security/gameplay concern: Moving end portal blocks could cause issues.
        if (net.minecraft.tileentity.TileEntityEndPortal.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityEndGateway.class.isAssignableFrom(teClass))
            return false;

        // Balance concern: moving block spawners can be cheaty and should be reserved to hard-to-obtain methods.
        if (net.minecraft.tileentity.TileEntityMobSpawner.class.isAssignableFrom(teClass))
            return false;

        // Placed skulls don't have an ItemBlock form, and can be moved away easily regardless.
        if (net.minecraft.tileentity.TileEntitySkull.class.isAssignableFrom(teClass))
            return false;

        // Was this also a security concern?
        if (net.minecraft.tileentity.TileEntitySign.class.isAssignableFrom(teClass))
            return false;

        // The rest: There's no point to packing them.
        if (net.minecraft.tileentity.TileEntityBanner.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityComparator.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityDaylightDetector.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityPiston.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityNote.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityEnchantmentTable.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityBed.class.isAssignableFrom(teClass))
            return false;

        //blacklist IC2 machines; they turn into invalid tiles upon unpacking
        if (te.getWorld().getBlockState(te.getPos()).getBlock().getRegistryName().toString().equals("ic2:te"))
            return false;


        // TODO: Blacklist more Vanilla stuffs.

        return true;
    }
}

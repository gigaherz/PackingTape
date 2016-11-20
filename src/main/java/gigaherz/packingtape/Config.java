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

    static void loadConfig(Configuration config)
    {
        Property bl = config.get("tileEntities", "blacklist", new String[0]);
        Property wl = config.get("tileEntities", "whitelist", new String[0]);
        Property ru = config.get("tapeRoll", "numberOfUses", 8);

        Collections.addAll(blackList, bl.getStringList());
        Collections.addAll(whiteList, wl.getStringList());
        tapeRollUses = ru.getInt();
        if (!bl.wasRead() || !wl.wasRead() || !ru.wasRead())
            config.save();
    }

    public static boolean isTileEntityAllowed(TileEntity te)
    {
        String cn = te.getClass().getCanonicalName();

        if (whiteList.contains(cn))
            return true;

        if (blackList.contains(cn))
            return false;

        // Security concern: moving command blocks may allow things to happen that shouldn't happen.
        if (te.getClass().equals(net.minecraft.tileentity.TileEntityCommandBlock.class))
            return false;

        // Security/gameplay concern: Moving end portal blocks could cause issues.
        if (te.getClass().equals(net.minecraft.tileentity.TileEntityEndPortal.class))
            return false;

        // Balance concern: moving block spawners can be cheaty and should be reserved to hard-to-obtain methods.
        if (te.getClass().equals(net.minecraft.tileentity.TileEntityMobSpawner.class))
            return false;

        // Placed skulls don't have an ItemBlock form, and can be moved away easily regardless.
        if (te.getClass().equals(net.minecraft.tileentity.TileEntitySkull.class))
            return false;

        // Was this also a security concern?
        if (te.getClass().equals(net.minecraft.tileentity.TileEntitySign.class))
            return false;

        // The rest: There's no point to packing them.
        if (te.getClass().equals(net.minecraft.tileentity.TileEntityBanner.class))
            return false;

        if (te.getClass().equals(net.minecraft.tileentity.TileEntityComparator.class))
            return false;

        if (te.getClass().equals(net.minecraft.tileentity.TileEntityDaylightDetector.class))
            return false;

        if (te.getClass().equals(net.minecraft.tileentity.TileEntityPiston.class))
            return false;

        if (te.getClass().equals(net.minecraft.tileentity.TileEntityNote.class))
            return false;

        // TODO: Blacklist more Vanilla stuffs.

        return true;
    }
}

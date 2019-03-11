package gigaherz.packingtape;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

public class Config
{
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static Set<String> whiteList = Sets.newHashSet();
    public static Set<String> blackList = Sets.newHashSet();
    public static int tapeRollUses = 8;
    public static boolean consumesPaper = true;

    public static void bake()
    {
        whiteList = Sets.newHashSet(SERVER.whitelist.get());
        blackList = Sets.newHashSet(SERVER.blacklist.get());
        tapeRollUses = SERVER.tapeRollUses.get();
        consumesPaper = SERVER.consumesPaper.get();
    }

    public static class ServerConfig
    {
        public ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
        public ForgeConfigSpec.ConfigValue<List<? extends String>> blacklist;
        public ForgeConfigSpec.IntValue tapeRollUses;
        public ForgeConfigSpec.BooleanValue consumesPaper;

        ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            whitelist = builder
                    .comment("TileEntities to allow regardless of the blacklist")
                    .translation("text.packingtape.config.whitelist")
                    .defineList("whitelist", Lists.newArrayList(), o -> o instanceof String);
            blacklist = builder
                    .comment("TileEntities to disallow (whitelist takes precedence)")
                    .translation("text.packingtape.config.blacklist")
                    .defineList("whitelist", Lists.newArrayList(), o -> o instanceof String);
            tapeRollUses = builder
                    .comment("How many times the tape roll can be used before it breaks")
                    .translation("text.packingtape.config.tape_roll_uses")
                    .defineInRange("tape_roll_uses", 8, 0, Integer.MAX_VALUE - 1);
            consumesPaper = builder
                    .comment("Whether the tape roll consumes paper when used")
                    .translation("text.packingtape.config.consume_paper")
                    .define("sound", true);
            builder.pop();
        }
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

        if (net.minecraft.tileentity.TileEntityEnchantmentTable.class.isAssignableFrom(teClass))
            return false;

        if (net.minecraft.tileentity.TileEntityBed.class.isAssignableFrom(teClass))
            return false;

        // TODO: Blacklist more Vanilla stuffs.

        return true;
    }
}

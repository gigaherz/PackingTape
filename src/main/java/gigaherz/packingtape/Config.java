package gigaherz.packingtape;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.tileentity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Config
{
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static
    {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static Set<TileEntityType<?>> whiteList = Sets.newHashSet();
    public static Set<TileEntityType<?>> blackList = Sets.newHashSet();
    public static int tapeRollUses = 8;
    public static boolean consumesPaper = true;

    public static void bake()
    {
        whiteList = SERVER.whitelist.get().stream().map(n -> ForgeRegistries.TILE_ENTITIES.getValue(new ResourceLocation(n))).collect(Collectors.toSet());
        blackList = SERVER.blacklist.get().stream().map(n -> ForgeRegistries.TILE_ENTITIES.getValue(new ResourceLocation(n))).collect(Collectors.toSet());
        blackList.addAll(getDefaultVanillaDisabled());
        tapeRollUses = SERVER.tapeRollUses.get();
        consumesPaper = SERVER.consumesPaper.get();
    }

    public static class ServerConfig
    {
        public ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
        public ForgeConfigSpec.ConfigValue<List<? extends String>> blacklist;
        public ForgeConfigSpec.IntValue tapeRollUses;
        public ForgeConfigSpec.BooleanValue consumesPaper;

        ServerConfig(ForgeConfigSpec.Builder builder)
        {
            builder.push("general");
            whitelist = builder
                    .comment("TileEntities to allow regardless of the blacklist and vanilla restrictions.",
                             "WARNING: This whitelist bypasses the 'only ops can copy' limitation of Minecraft, which can result in security issues, don't whitelist things unless you know what the side-effects will be.",
                             "NOTE: This list now uses 'Block Entity Type' IDs. Eg. the spawner is 'minecraft:mob_spawner'.")
                    .translation("text.packingtape.config.whitelist")
                    .defineList("whitelist", Lists.newArrayList(), o -> o instanceof String);
            blacklist = builder
                    .comment("TileEntities to disallow (whitelist takes precedence)",
                             "NOTE: This list now uses 'Block Entity Type' IDs. Eg. the shulker boxes are 'minecraft:shulker_box'.")
                    .translation("text.packingtape.config.blacklist")
                    .defineList("blacklist", Lists.newArrayList(), o -> o instanceof String);
            tapeRollUses = builder
                    .comment("How many times the tape roll can be used before it breaks")
                    .translation("text.packingtape.config.tape_roll_uses")
                    .defineInRange("tape_roll_uses", 8, 0, Integer.MAX_VALUE - 1);
            consumesPaper = builder
                    .comment("Whether the tape roll consumes paper when used")
                    .translation("text.packingtape.config.consume_paper")
                    .define("consume_paper", true);
            builder.pop();
        }
    }

    public static Set<TileEntityType<?>> getDefaultVanillaDisabled()
    {
        return Sets.newHashSet(
                // Security concern: moving command blocks may allow things to happen that shouldn't happen.
                TileEntityType.COMMAND_BLOCK,
                TileEntityType.STRUCTURE_BLOCK,
                TileEntityType.JIGSAW,

                // Was this also a security concern?
                TileEntityType.SIGN,

                // Security/gameplay concern: Moving end portal blocks could cause issues.
                TileEntityType.END_PORTAL,
                TileEntityType.END_GATEWAY,

                // Placed skulls don't have an ItemBlock form, and can be moved away easily regardless.
                TileEntityType.SKULL,

                // Conduit TEs store a list of surrounding blocks, and have no items stored.
                TileEntityType.CONDUIT,

                // The rest: There's no point to packing them.
                TileEntityType.BANNER,
                TileEntityType.COMPARATOR,
                TileEntityType.DAYLIGHT_DETECTOR,
                TileEntityType.PISTON,
                TileEntityType.ENCHANTING_TABLE,
                TileEntityType.BED,
                TileEntityType.BELL
        );
    }

    public static boolean isTileEntityAllowed(TileEntity te)
    {
        TileEntityType<?> type = te.getType();

        if (whiteList.contains(type))
            return true;

        if (te.onlyOpsCanSetNbt())
            return false;

        return !blackList.contains(type);
    }
}

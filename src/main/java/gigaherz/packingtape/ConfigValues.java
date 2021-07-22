package gigaherz.packingtape;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigValues
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
                             "DEPRECATED: Use the tileentity tag 'packingtape:te_whitelist' instead. This config will be removed in the future.",
                             "WARNING: This whitelist bypasses the 'only ops can copy' limitation of Minecraft, which can result in security issues, don't whitelist things unless you know what the side-effects will be.",
                             "NOTE: This list now uses 'Block Entity Type' IDs. Eg. the spawner is 'minecraft:mob_spawner'.")
                    .translation("text.packingtape.config.whitelist")
                    .defineList("whitelist", Lists.newArrayList(), o -> o instanceof String);
            blacklist = builder
                    .comment("TileEntities to disallow (whitelist takes precedence)",
                            "DEPRECATED: Use the tileentity tag 'packingtape:te_blacklist' instead. This config will be removed in the future.",
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

    public static ITag<TileEntityType<?>> TE_WHITELIST = ForgeTagHandler.createOptionalTag(ForgeRegistries.TILE_ENTITIES, PackingTapeMod.location("te_whitelist"));
    public static ITag<TileEntityType<?>> TE_BLACKLIST = ForgeTagHandler.createOptionalTag(ForgeRegistries.TILE_ENTITIES, PackingTapeMod.location("te_blacklist"));

    public static boolean isTileEntityBlocked(TileEntity te)
    {
        TileEntityType<?> type = te.getType();

        if (whiteList.contains(type))
            return false;

        if (type.isIn(TE_WHITELIST))
            return false;

        if (te.onlyOpsCanSetNbt())
            return true;

        if (blackList.contains(type))
            return true;

        return type.isIn(TE_BLACKLIST);
    }
}

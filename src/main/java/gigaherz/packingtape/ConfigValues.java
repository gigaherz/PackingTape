package gigaherz.packingtape;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.tags.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

    public static int tapeRollUses = 8;
    public static boolean consumesPaper = true;

    public static void bake()
    {
        tapeRollUses = SERVER.tapeRollUses.get();
        consumesPaper = SERVER.consumesPaper.get();
    }

    public static class ServerConfig
    {
        public ForgeConfigSpec.IntValue tapeRollUses;
        public ForgeConfigSpec.BooleanValue consumesPaper;

        ServerConfig(ForgeConfigSpec.Builder builder)
        {
            builder.push("general");
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

    public static Tag<BlockEntityType<?>> TE_WHITELIST = ForgeTagHandler.createOptionalTag(ForgeRegistries.BLOCK_ENTITIES, PackingTapeMod.location("te_whitelist"));
    public static Tag<BlockEntityType<?>> TE_BLACKLIST = ForgeTagHandler.createOptionalTag(ForgeRegistries.BLOCK_ENTITIES, PackingTapeMod.location("te_blacklist"));

    public static boolean isTileEntityBlocked(BlockEntity te)
    {
        BlockEntityType<?> type = te.getType();

        if (type.isIn(TE_WHITELIST))
            return false;

        if (te.onlyOpCanSetNbt())
            return true;

        return type.isIn(TE_BLACKLIST);
    }
}

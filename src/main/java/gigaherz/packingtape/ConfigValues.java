package gigaherz.packingtape;

import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

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

    public static TagKey<BlockEntityType<?>> TE_WHITELIST = TagKey.create(Registry.BLOCK_ENTITY_TYPE_REGISTRY, PackingTapeMod.location("te_whitelist"));
    public static TagKey<BlockEntityType<?>> TE_BLACKLIST = TagKey.create(Registry.BLOCK_ENTITY_TYPE_REGISTRY, PackingTapeMod.location("te_blacklist"));

    public static boolean isTileEntityBlocked(BlockEntity te)
    {
        BlockEntityType<?> type = te.getType();

        var rk = Registry.BLOCK_ENTITY_TYPE.getResourceKey(type).orElseThrow();
        var holder = Registry.BLOCK_ENTITY_TYPE.getHolderOrThrow(rk);

        if (holder.is(TE_WHITELIST))
            return false;

        if (te.onlyOpCanSetNbt())
            return true;

        return holder.is(TE_BLACKLIST);
    }
}

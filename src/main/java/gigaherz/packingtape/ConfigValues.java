package gigaherz.packingtape;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigValues
{
    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static
    {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static int tapeRollUses = 8;
    public static boolean consumesPaper = true;
    public static int maxStorageSize = 1000000;

    public static void bake()
    {
        tapeRollUses = SERVER.tapeRollUses.get();
        consumesPaper = SERVER.consumesPaper.get();
        maxStorageSize = SERVER.maxStorageSize.get();
    }

    public static class ServerConfig
    {
        public ModConfigSpec.IntValue tapeRollUses;
        public ModConfigSpec.BooleanValue consumesPaper;
        public ModConfigSpec.IntValue maxStorageSize;

        ServerConfig(ModConfigSpec.Builder builder)
        {
            builder.push("general");
            tapeRollUses = builder
                    .comment("How many times the tape roll can be used before it breaks")
                    .defineInRange("tape_roll_uses", 8, 0, Integer.MAX_VALUE - 1);
            consumesPaper = builder
                    .comment("Whether the tape roll consumes paper when used")
                    .define("consume_paper", true);
            maxStorageSize = builder
                    .comment("How much space (in bytes) the packaged block contents can take up.")
                    .defineInRange("max_storage_size", 1000000, 0, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    public static TagKey<BlockEntityType<?>> TE_WHITELIST = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("te_whitelist"));
    public static TagKey<BlockEntityType<?>> TE_BLACKLIST = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("te_blacklist"));

    public static TagKey<Block> BLOCK_WHITELIST = TagKey.create(Registries.BLOCK, PackingTapeMod.location("te_whitelist"));
    public static TagKey<Block> BLOCK_BLACKLIST = TagKey.create(Registries.BLOCK, PackingTapeMod.location("te_blacklist"));

    public static boolean isBlockEntityWhitelisted(BlockEntity be)
    {
        var block = be.getBlockState().getBlock();

        var rk0 = BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
        var holder0 = BuiltInRegistries.BLOCK.getHolder(rk0).orElseThrow();

        if (holder0.is(BLOCK_WHITELIST))
            return true;

        var type = be.getType();

        var rk1 = BuiltInRegistries.BLOCK_ENTITY_TYPE.getResourceKey(type).orElseThrow();
        var holder1 = BuiltInRegistries.BLOCK_ENTITY_TYPE.getHolder(rk1).orElseThrow();

        return holder1.is(TE_WHITELIST);
    }

    public static boolean isBlockEntityBlocked(BlockEntity be)
    {
        var block = be.getBlockState().getBlock();

        var rk0 = BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
        var holder0 = BuiltInRegistries.BLOCK.getHolder(rk0).orElseThrow();

        if (holder0.is(BLOCK_WHITELIST))
            return false;

        if (holder0.is(BLOCK_BLACKLIST))
            return true;

        var type = be.getType();

        var rk1 = BuiltInRegistries.BLOCK_ENTITY_TYPE.getResourceKey(type).orElseThrow();
        var holder1 = BuiltInRegistries.BLOCK_ENTITY_TYPE.getHolder(rk1).orElseThrow();

        if (holder1.is(TE_WHITELIST))
            return false;

        if (be.onlyOpCanSetNbt())
            return true;

        return holder1.is(TE_BLACKLIST);
    }
}

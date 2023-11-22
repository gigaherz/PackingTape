package gigaherz.packingtape;

import gigaherz.packingtape.tape.PackagedBlock;
import gigaherz.packingtape.tape.PackagedBlockEntity;
import gigaherz.packingtape.tape.TapeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod(PackingTapeMod.MODID)
public class PackingTapeMod
{
    public static final String MODID = "packingtape";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<PackagedBlock> PACKAGED_BLOCK = BLOCKS.register("packaged_block", () ->
            new PackagedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.5f, 0.5f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> PACKAGED_BLOCK_ITEM = ITEMS.register(PACKAGED_BLOCK.getId().getPath(), () ->
        new BlockItem(PACKAGED_BLOCK.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackagedBlockEntity>> PACKAGED_BLOCK_ENTITY = BLOCK_ENTITIES.register(PACKAGED_BLOCK.getId().getPath(), () ->
        BlockEntityType.Builder.of(PackagedBlockEntity::new, PACKAGED_BLOCK.get()).build(null));
    public static final DeferredItem<TapeItem> TAPE = ITEMS.register("tape", () ->
            new TapeItem(new Item.Properties().stacksTo(16)));

    public PackingTapeMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::serverConfig);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::addItemsToTabs);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
    }

    private void addItemsToTabs(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
        {
            event.accept(TAPE);
            event.accept(PACKAGED_BLOCK_ITEM, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    private void gatherData(GatherDataEvent event)
    {
        DataGen.gatherData(event);
    }

    public void serverConfig(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == ConfigValues.SERVER_SPEC)
            ConfigValues.bake();
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }

    public static class DataGen
    {
        public static void gatherData(GatherDataEvent event)
        {
            DataGenerator gen = event.getGenerator();

            gen.addProvider(event.includeServer(), Loot.create(gen.getPackOutput()));
            gen.addProvider(event.includeServer(), new Recipes(gen, event.getLookupProvider()));
        }

        private static class Recipes extends RecipeProvider implements DataProvider, IConditionBuilder
        {
            public Recipes(DataGenerator gen, CompletableFuture<HolderLookup.Provider> lookupProvider)
            {
                super(gen.getPackOutput(), lookupProvider);
            }

            @Override
            protected void buildRecipes(RecipeOutput consumer)
            {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, PackingTapeMod.TAPE.get())
                        .requires(Items.SLIME_BALL)
                        .requires(Items.STRING)
                        .requires(Items.PAPER)
                        .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                        .save(consumer);
            }
        }

        private static class Loot
        {
            public static LootTableProvider create(PackOutput gen)
            {
                return new LootTableProvider(gen, Set.of(), List.of(
                        new LootTableProvider.SubProviderEntry(Loot.BlockTables::new, LootContextParamSets.BLOCK)
                ));
            }


            public static class BlockTables extends BlockLootSubProvider
            {
                public BlockTables()
                {
                    super(Set.of(), FeatureFlags.REGISTRY.allFlags());
                }

                @Override
                protected void generate()
                {
                    this.add(PackingTapeMod.PACKAGED_BLOCK.get(), this::dropWithPackagedContents);
                }

                protected LootTable.Builder dropWithPackagedContents(Block p_218544_0_)
                {
                    return LootTable.lootTable()
                            .withPool(applyExplosionCondition(p_218544_0_, LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1))
                                    .add(LootItem.lootTableItem(p_218544_0_)
                                            .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                                            .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                                    .copy("Block", "BlockEntityTag.Block")
                                                    .copy("BlockEntity", "BlockEntityTag.BlockEntity")
                                                    .copy("PreferredDirection", "BlockEntityTag.PreferredDirection")))));
                }

                @Override
                protected Iterable<Block> getKnownBlocks()
                {
                    return BuiltInRegistries.BLOCK.entrySet().stream()
                            .filter(e -> e.getKey().location().getNamespace().equals(PackingTapeMod.MODID))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList());
                }
            }
        }
    }
}

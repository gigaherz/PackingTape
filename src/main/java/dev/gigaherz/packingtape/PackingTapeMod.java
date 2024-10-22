package dev.gigaherz.packingtape;

import dev.gigaherz.packingtape.tape.ContainedBlockData;
import dev.gigaherz.packingtape.tape.PackagedBlock;
import dev.gigaherz.packingtape.tape.PackagedBlockEntity;
import dev.gigaherz.packingtape.tape.TapeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
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
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.Tags;
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
import java.util.stream.Collectors;

@Mod(PackingTapeMod.MODID)
public class PackingTapeMod
{
    public static final String MODID = "packingtape";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredBlock<PackagedBlock>
            PACKAGED_BLOCK = BLOCKS.registerBlock("packaged_block", props ->
                    new PackagedBlock(props
                            .mapColor(MapColor.WOOL).strength(0.5f, 0.5f).sound(SoundType.WOOD)),
            BlockBehaviour.Properties.of());

    public static final DeferredItem<BlockItem>
            PACKAGED_BLOCK_ITEM = ITEMS.registerItem(PACKAGED_BLOCK.getId().getPath(), props ->
                    new BlockItem(PACKAGED_BLOCK.get(), props.stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackagedBlockEntity>>
            PACKAGED_BLOCK_ENTITY = BLOCK_ENTITIES.register(PACKAGED_BLOCK.getId().getPath(), () ->
                    new BlockEntityType<>(PackagedBlockEntity::new, PACKAGED_BLOCK.get()));

    public static final DeferredItem<TapeItem>
            TAPE = ITEMS.registerItem("tape", props -> new TapeItem(props.stacksTo(16)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ContainedBlockData>>
            CONTAINED_BLOCK = DATA_COMPONENTS.register("contained_block", () -> DataComponentType.<ContainedBlockData>builder()
            .persistent(ContainedBlockData.CODEC)
            .networkSynchronized(ContainedBlockData.STREAM_CODEC)
            .build());

    public PackingTapeMod(ModContainer thisContainer, IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(this::modConfigLoad);
        modEventBus.addListener(this::modConfigReload);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::addItemsToTabs);

        thisContainer.registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
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

    public void modConfigLoad(ModConfigEvent.Loading event)
    {
        refreshConfig(event);
    }

    public void modConfigReload(ModConfigEvent.Reloading event)
    {
        refreshConfig(event);
    }

    private void refreshConfig(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == ConfigValues.SERVER_SPEC)
            ConfigValues.bake();
    }

    public static ResourceLocation location(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static class DataGen
    {
        public static void gatherData(GatherDataEvent event)
        {
            DataGenerator gen = event.getGenerator();
            PackOutput output = gen.getPackOutput();

            gen.addProvider(event.includeServer(), Loot.create(output, event.getLookupProvider()));
            gen.addProvider(event.includeServer(), new Recipes(output, event.getLookupProvider()));
        }

        private static class Recipes extends RecipeProvider.Runner
        {
            public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup)
            {
                super(output, lookup);
            }

            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput output)
            {
                return new VanillaRecipeProvider(lookup, output)
                {
                    @Override
                    protected void buildRecipes()
                    {
                        shapeless(RecipeCategory.TOOLS, PackingTapeMod.TAPE.get())
                                .requires(Tags.Items.SLIME_BALLS)
                                .requires(Tags.Items.STRINGS)
                                .requires(Items.PAPER)
                                .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                                .save(output);
                    }
                };
            }

            @Override
            public String getName()
            {
                return "Packing Tape Recipes";
            }
        }

        private static class Loot
        {
            public static LootTableProvider create(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookup)
            {
                return new LootTableProvider(packOutput, Set.of(), List.of(
                        new LootTableProvider.SubProviderEntry(BlockTables::new, LootContextParamSets.BLOCK)
                ), lookup);
            }


            public static class BlockTables extends BlockLootSubProvider
            {
                public BlockTables(HolderLookup.Provider provider)
                {
                    super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
                }

                @Override
                protected void generate()
                {
                    this.add(PackingTapeMod.PACKAGED_BLOCK.get(), this::dropWithPackagedContents);
                }

                protected LootTable.Builder dropWithPackagedContents(Block block)
                {
                    return LootTable.lootTable()
                            .withPool(applyExplosionCondition(block, LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1))
                                    .add(LootItem.lootTableItem(block)
                                            .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                                    .include(PackingTapeMod.CONTAINED_BLOCK.get()))
                                    )
                            ));
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

package dev.gigaherz.packingtape;

import dev.gigaherz.packingtape.tape.ContainedBlockData;
import dev.gigaherz.packingtape.tape.PackagedBlock;
import dev.gigaherz.packingtape.tape.PackagedBlockEntity;
import dev.gigaherz.packingtape.tape.TapeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypeIds;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
import java.util.function.Consumer;
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
                            .mapColor(MapColor.WOOL).strength(0.5f, 0.5f).sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem>
            PACKAGED_BLOCK_ITEM = ITEMS.registerItem(PACKAGED_BLOCK.getId().getPath(), props ->
                    new PackagedBlockItem(PackingTapeMod.PACKAGED_BLOCK.get(), props.stacksTo(16).useBlockDescriptionPrefix()));

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

    private void gatherData(GatherDataEvent.Client event)
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

    public static Identifier location(String path)
    {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static class DataGen
    {
        public static void gatherData(GatherDataEvent.Client event)
        {
            DataGenerator gen = event.getGenerator();
            PackOutput output = gen.getPackOutput();

            gen.addProvider(true, Loot.create(output, event.getLookupProvider()));
            gen.addProvider(true, new Recipes(output, event.getLookupProvider()));
            gen.addProvider(true, new BlockTagGen(output, event.getLookupProvider()));
            gen.addProvider(true, new BlockEntityTagGen(output, event.getLookupProvider()));
        }

        private static class BlockTagGen extends TagsProvider<Block>
        {
            public static TagKey<Block> BLACKLIST_CHEATY = TagKey.create(Registries.BLOCK, PackingTapeMod.location("blacklist/cheaty"));
            public static TagKey<Block> BLACKLIST_MULTIBLOCK = TagKey.create(Registries.BLOCK, PackingTapeMod.location("blacklist/multiblock"));
            public static TagKey<Block> BLACKLIST_POINTLESS = TagKey.create(Registries.BLOCK, PackingTapeMod.location("blacklist/pointless"));
            public static TagKey<Block> BLACKLIST_PROBLEMATIC = TagKey.create(Registries.BLOCK, PackingTapeMod.location("blacklist/problematic"));
            public static TagKey<Block> BLACKLIST_UNSAFE = TagKey.create(Registries.BLOCK, PackingTapeMod.location("blacklist/unsafe"));

            public BlockTagGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
            {
                super(packOutput, Registries.BLOCK, lookupProvider, MODID);
            }

            @SuppressWarnings({"unchecked", "DataFlowIssue", "deprecation"})
            @Override
            protected void addTags(HolderLookup.Provider lookup)
            {
                tag(BLACKLIST_CHEATY).add(BlockItemIds.END_PORTAL_FRAME.block(), Blocks.END_GATEWAY.builtInRegistryHolder().getKey(), BlockItemIds.SPAWNER.block(), BlockItemIds.TRIAL_SPAWNER.block());
                tag(BLACKLIST_MULTIBLOCK).addTags(BlockTags.DOORS).add(BlockItemIds.PISTON.block(), Blocks.PISTON_HEAD.builtInRegistryHolder().getKey());
                tag(BLACKLIST_POINTLESS).add(
                        BlockItemIds.ENCHANTING_TABLE.block(), BlockItemIds.BELL.block(), BlockItemIds.COMPARATOR.block(),
                        BlockItemIds.DAYLIGHT_DETECTOR.block(), BlockItemIds.POTENT_SULFUR.block(),
                        PACKAGED_BLOCK.getKey());
                tag(BLACKLIST_PROBLEMATIC).addTags(Tags.Blocks.SKULLS, BlockTags.BANNERS).add(BlockItemIds.CONDUIT.block());
                tag(BLACKLIST_UNSAFE).add(BlockItemIds.COMMAND_BLOCK.block(), BlockItemIds.CHAIN_COMMAND_BLOCK.block(), BlockItemIds.REPEATING_COMMAND_BLOCK.block(),
                        BlockItemIds.STRUCTURE_BLOCK.block(), BlockItemIds.JIGSAW.block(), BlockItemIds.TEST_BLOCK.block(), BlockItemIds.TEST_INSTANCE_BLOCK.block()).addTags(BlockTags.ALL_SIGNS);
                tag(ConfigValues.BLOCK_BLACKLIST).addTags(
                        BLACKLIST_CHEATY, BLACKLIST_MULTIBLOCK, BLACKLIST_POINTLESS,
                        BLACKLIST_PROBLEMATIC, BLACKLIST_UNSAFE,
                        Tags.Blocks.RELOCATION_NOT_SUPPORTED);
                tag(ConfigValues.BLOCK_WHITELIST);
            }
        }

        private static class BlockEntityTagGen extends TagsProvider<BlockEntityType<?>>
        {
            public static TagKey<BlockEntityType<?>> BLACKLIST_CHEATY = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("blacklist/cheaty"));
            public static TagKey<BlockEntityType<?>> BLACKLIST_MULTIBLOCK = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("blacklist/multiblock"));
            public static TagKey<BlockEntityType<?>> BLACKLIST_POINTLESS = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("blacklist/pointless"));
            public static TagKey<BlockEntityType<?>> BLACKLIST_PROBLEMATIC = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("blacklist/problematic"));
            public static TagKey<BlockEntityType<?>> BLACKLIST_UNSAFE = TagKey.create(Registries.BLOCK_ENTITY_TYPE, PackingTapeMod.location("blacklist/unsafe"));

            public BlockEntityTagGen(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider)
            {
                super(packOutput, Registries.BLOCK_ENTITY_TYPE, lookupProvider, MODID);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void addTags(HolderLookup.Provider lookup)
            {
                tag(BLACKLIST_CHEATY).add(BlockEntityTypeIds.END_PORTAL, BlockEntityTypeIds.END_GATEWAY);
                tag(BLACKLIST_MULTIBLOCK).add(BlockEntityTypeIds.PISTON);
                tag(BLACKLIST_POINTLESS).add(BlockEntityTypeIds.ENCHANTING_TABLE, BlockEntityTypeIds.BELL,
                        BlockEntityTypeIds.COMPARATOR, BlockEntityTypeIds.DAYLIGHT_DETECTOR, BlockEntityTypeIds.POTENT_SULFUR,
                        PACKAGED_BLOCK_ENTITY.getKey());
                tag(BLACKLIST_PROBLEMATIC).add(BlockEntityTypeIds.SKULL, BlockEntityTypeIds.BANNER, BlockEntityTypeIds.CONDUIT);
                tag(BLACKLIST_UNSAFE).add(BlockEntityTypeIds.COMMAND_BLOCK, BlockEntityTypeIds.STRUCTURE_BLOCK, BlockEntityTypeIds.JIGSAW, BlockEntityTypeIds.SIGN, BlockEntityTypeIds.HANGING_SIGN);
                tag(ConfigValues.TE_BLACKLIST).addTags(
                        BLACKLIST_CHEATY, BLACKLIST_MULTIBLOCK, BLACKLIST_POINTLESS,
                        BLACKLIST_PROBLEMATIC, BLACKLIST_UNSAFE);
                tag(ConfigValues.TE_WHITELIST);
            }
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
                                            .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                                    .include(DataComponents.CUSTOM_NAME)
                                                    .include(PackingTapeMod.CONTAINED_BLOCK.get()))
                                    )
                            ));
                }

                @Override
                protected Iterable<Block> getKnownBlocks()
                {
                    return BuiltInRegistries.BLOCK.entrySet().stream()
                            .filter(e -> e.getKey().identifier().getNamespace().equals(PackingTapeMod.MODID))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList());
                }
            }
        }
    }

    private static class PackagedBlockItem extends BlockItem
    {

        public PackagedBlockItem(PackagedBlock block, Properties properties)
        {
            super(block, properties);
        }

        @Deprecated
        @Override
        public void appendHoverText(ItemStack p_41421_, TooltipContext p_339594_, TooltipDisplay p_399753_, Consumer<Component> p_399884_, TooltipFlag p_41424_)
        {
            ((PackagedBlock) getBlock()).appendHoverText(p_41421_, p_399884_);
        }
    }
}

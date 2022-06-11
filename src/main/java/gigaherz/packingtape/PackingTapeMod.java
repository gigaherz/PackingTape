package gigaherz.packingtape;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gigaherz.packingtape.tape.PackagedBlock;
import gigaherz.packingtape.tape.PackagedBlockEntity;
import gigaherz.packingtape.tape.TapeItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod(PackingTapeMod.MODID)
public class PackingTapeMod
{
    public static final String MODID = "packingtape";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MODID);

    public static final RegistryObject<PackagedBlock> PACKAGED_BLOCK = BLOCKS.register("packaged_block", () ->
            new PackagedBlock(BlockBehaviour.Properties.of(Material.WOOL).strength(0.5f, 0.5f).sound(SoundType.WOOD)));
    public static final RegistryObject<BlockItem> PACKAGED_BLOCK_ITEM = ITEMS.register(PACKAGED_BLOCK.getId().getPath(), () ->
        new BlockItem(PACKAGED_BLOCK.get(), new Item.Properties().stacksTo(16).tab(CreativeModeTab.TAB_MISC)));
    public static final RegistryObject<BlockEntityType<?>> PACKAGED_BLOCK_ENTITY = BLOCK_ENTITIES.register(PACKAGED_BLOCK.getId().getPath(), () ->
        BlockEntityType.Builder.of(PackagedBlockEntity::new, PACKAGED_BLOCK.get()).build(null));
    public static final RegistryObject<TapeItem> TAPE = ITEMS.register("tape", () ->
            new TapeItem(new Item.Properties().stacksTo(16).tab(CreativeModeTab.TAB_MISC)));

    public PackingTapeMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::serverConfig);
        modEventBus.addListener(this::gatherData);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
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

            gen.addProvider(event.includeServer(), new LootTables(gen));
            gen.addProvider(event.includeServer(), new Recipes(gen));
        }

        private static class Recipes extends RecipeProvider implements DataProvider, IConditionBuilder
        {
            public Recipes(DataGenerator gen)
            {
                super(gen);
            }

            @Override
            protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer)
            {
                ShapelessRecipeBuilder.shapeless(PackingTapeMod.TAPE.get())
                        .requires(Items.SLIME_BALL)
                        .requires(Items.STRING)
                        .requires(Items.PAPER)
                        .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                        .save(consumer);
            }
        }

        private static class LootTables extends LootTableProvider implements DataProvider
        {
            public LootTables(DataGenerator gen)
            {
                super(gen);
            }

            private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> tables = ImmutableList.of(
                    Pair.of(BlockTables::new, LootContextParamSets.BLOCK)
                    //Pair.of(FishingLootTables::new, LootParameterSets.FISHING),
                    //Pair.of(ChestLootTables::new, LootParameterSets.CHEST),
                    //Pair.of(EntityLootTables::new, LootParameterSets.ENTITY),
                    //Pair.of(GiftLootTables::new, LootParameterSets.GIFT)
            );

            @Override
            protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables()
            {
                return tables;
            }

            @Override
            protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker)
            {
                map.forEach((p_218436_2_, p_218436_3_) -> {
                    net.minecraft.world.level.storage.loot.LootTables.validate(validationtracker, p_218436_2_, p_218436_3_);
                });
            }

            public static class BlockTables extends BlockLoot
            {
                @Override
                protected void addTables()
                {
                    this.add(PackingTapeMod.PACKAGED_BLOCK.get(), BlockTables::dropWithPackagedContents);
                }

                protected static LootTable.Builder dropWithPackagedContents(Block p_218544_0_)
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
                    return ForgeRegistries.BLOCKS.getEntries().stream()
                            .filter(e -> e.getKey().location().getNamespace().equals(PackingTapeMod.MODID))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList());
                }
            }
        }
    }
}

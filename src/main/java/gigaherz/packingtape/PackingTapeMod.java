package gigaherz.packingtape;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gigaherz.packingtape.tape.PackagedBlock;
import gigaherz.packingtape.tape.PackagedBlockEntity;
import gigaherz.packingtape.tape.TapeItem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.data.*;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.loot.*;
import net.minecraft.loot.functions.CopyName;
import net.minecraft.loot.functions.CopyNbt;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
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
    private static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MODID);

    public static final RegistryObject<PackagedBlock> PACKAGED_BLOCK = BLOCKS.register("packaged_block", () ->
            new PackagedBlock(Block.Properties.create(Material.WOOL).hardnessAndResistance(0.5f, 0.5f).sound(SoundType.WOOD)));
    static
    {
        ITEMS.register(PACKAGED_BLOCK.getId().getPath(), () ->
                new BlockItem(PACKAGED_BLOCK.get(), new Item.Properties().maxStackSize(16).group(ItemGroup.MISC)));
        TILE_ENTITIES.register(PACKAGED_BLOCK.getId().getPath(), () ->
                TileEntityType.Builder.create(PackagedBlockEntity::new, PACKAGED_BLOCK.get()).build(null));
    }

    public static final RegistryObject<TapeItem> TAPE = ITEMS.register("tape", () ->
            new TapeItem(new Item.Properties().maxStackSize(16).group(ItemGroup.MISC)));

    public static Logger logger = LogManager.getLogger(MODID);

    public PackingTapeMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TILE_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::serverConfig);
        modEventBus.addListener(this::gatherData);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
    }

    private void gatherData(GatherDataEvent event)
    {
        DataGen.gatherData(event);
    }

    public void serverConfig(ModConfig.ModConfigEvent event)
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

            if (event.includeServer())
            {
                gen.addProvider(new LootTables(gen));
                gen.addProvider(new Recipes(gen));
            }
        }

        private static class Recipes extends RecipeProvider implements IDataProvider, IConditionBuilder
        {
            public Recipes(DataGenerator gen)
            {
                super(gen);
            }

            @Override
            protected void registerRecipes(Consumer<IFinishedRecipe> consumer)
            {
                ShapelessRecipeBuilder.shapelessRecipe(PackingTapeMod.TAPE.get())
                        .addIngredient(Items.SLIME_BALL)
                        .addIngredient(Items.STRING)
                        .addIngredient(Items.PAPER)
                        .addCriterion("has_slime_ball", hasItem(Items.SLIME_BALL))
                        .build(consumer);

            }
        }

        private static class LootTables extends LootTableProvider implements IDataProvider
        {
            public LootTables(DataGenerator gen)
            {
                super(gen);
            }

            private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> tables = ImmutableList.of(
                    Pair.of(BlockTables::new, LootParameterSets.BLOCK)
                    //Pair.of(FishingLootTables::new, LootParameterSets.FISHING),
                    //Pair.of(ChestLootTables::new, LootParameterSets.CHEST),
                    //Pair.of(EntityLootTables::new, LootParameterSets.ENTITY),
                    //Pair.of(GiftLootTables::new, LootParameterSets.GIFT)
            );

            @Override
            protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables()
            {
                return tables;
            }

            @Override
            protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationtracker) {
                map.forEach((p_218436_2_, p_218436_3_) -> {
                    LootTableManager.func_227508_a_(validationtracker, p_218436_2_, p_218436_3_);
                });
            }

            public static class BlockTables extends BlockLootTables
            {
                @Override
                protected void addTables()
                {
                    this.registerLootTable(PackingTapeMod.PACKAGED_BLOCK.get(), BlockTables::dropWithPackagedContents);
                }

                protected static LootTable.Builder dropWithPackagedContents(Block p_218544_0_) {
                    return LootTable.builder()
                            .addLootPool(withSurvivesExplosion(p_218544_0_, LootPool.builder()
                                    .rolls(ConstantRange.of(1))
                                    .addEntry(ItemLootEntry.builder(p_218544_0_)
                                            .acceptFunction(CopyName.builder(CopyName.Source.BLOCK_ENTITY))
                                            .acceptFunction(CopyNbt.builder(CopyNbt.Source.BLOCK_ENTITY)
                                                    .replaceOperation("Block", "BlockEntityTag.Block")
                                                    .replaceOperation("BlockEntity", "BlockEntityTag.BlockEntity")
                                                    .replaceOperation("PreferredDirection", "BlockEntityTag.PreferredDirection")))));
                }

                @Override
                protected Iterable<Block> getKnownBlocks()
                {
                    return ForgeRegistries.BLOCKS.getValues().stream()
                            .filter(b -> b.getRegistryName().getNamespace().equals(PackingTapeMod.MODID))
                            .collect(Collectors.toList());
                }
            }
        }

    }
}

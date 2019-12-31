package gigaherz.packingtape.util;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MiniReg
{
    private final List<DeferredRegister<?>> registerList = Lists.newArrayList();
    private final NonNullLazy<DeferredRegister<Block>> BLOCKS = NonNullLazy.of(() -> createDeferred(ForgeRegistries.BLOCKS));
    private final NonNullLazy<DeferredRegister<Item>> ITEMS = NonNullLazy.of(() -> createDeferred(ForgeRegistries.ITEMS));
    private final NonNullLazy<DeferredRegister<TileEntityType<?>>> TILE_ENTITIES = NonNullLazy.of(() -> createDeferred(ForgeRegistries.TILE_ENTITIES));
    private final String modId;

    public MiniReg(String modId)
    {
        this.modId = modId;
    }

    private final <T extends IForgeRegistryEntry<T>> DeferredRegister<T> createDeferred(IForgeRegistry<T> registry)
    {
        DeferredRegister<T> deferred = new DeferredRegister<>(registry, MiniReg.this.modId);
        registerList.add(deferred);
        return deferred;
    }

    public void subscribeEvents(IEventBus bus)
    {
        registerList.forEach(def -> def.register(bus));
    }

    public <T extends Block> MiniBlock<T> block(String name, Supplier<T> factory)
    {
        return new MiniBlock<>(name, factory);
    }

    public <T extends Item> MiniItem<T> item(String name, Supplier<T> factory)
    {
        return new MiniItem<>(name, factory);
    }

    public <T extends Item> MiniItem<T> tileEntity(String name, Supplier<T> factory)
    {
        return new MiniItem<>(name, factory);
    }

    public class MiniBlock<T extends Block>
    {
        private final String name;
        private final Supplier<T> factory;
        private Function<Supplier<T>, ? extends Item> itemFactory;
        private Function<Supplier<T>, ? extends TileEntityType<?>> tileEntityFactory;

        private MiniBlock(String name, Supplier<T> factory)
        {
            this.name = name;
            this.factory = factory;
        }

        public MiniBlock<T> withItem()
        {
            return withItem(new Item.Properties());
        }

        public MiniBlock<T> withItem(Item.Properties properties)
        {
            return withItem((block) -> new BlockItem(block.get(), properties));
        }

        public MiniBlock<T> withItem(Function<Supplier<T>, ? extends Item> itemFactory)
        {
            this.itemFactory = itemFactory;
            return this;
        }

        public <E extends TileEntity> MiniBlock<T> withTileEntity(Supplier<E> factory)
        {
            return withTileEntity((block) -> TileEntityType.Builder.create(factory, block.get()).build(null));
        }

        public <E extends TileEntity> MiniBlock<T> withTileEntity(Supplier<E> factory, Function<Supplier<T>, Collection<Supplier<? extends Block>>> validBlocks)
        {
            return withTileEntity((block) -> TileEntityType.Builder.create(factory, validBlocks.apply(block).stream().map(Supplier::get).toArray(Block[]::new)).build(null));
        }

        public MiniBlock<T> withTileEntity(Function<Supplier<T>, ? extends TileEntityType<?>> tileEntityFactory)
        {
            this.tileEntityFactory = tileEntityFactory;
            return this;
        }

        public RegistryObject<T> defer()
        {
            RegistryObject<T> block = BLOCKS.get().register(name, factory);
            if (itemFactory != null)
                ITEMS.get().register(name, () -> itemFactory.apply(block));
            if (tileEntityFactory != null)
                TILE_ENTITIES.get().register(name, () -> tileEntityFactory.apply(block));
            return block;
        }
    }

    public class MiniItem<T extends Item>
    {
        private final String name;
        private final Supplier<T> factory;

        private MiniItem(String name, Supplier<T> factory)
        {
            this.name = name;
            this.factory = factory;
        }

        public RegistryObject<T> defer()
        {
            return ITEMS.get().register(name, factory);
        }
    }

    public class MiniTileEntity<T extends TileEntityType<?>>
    {
        private final String name;
        private final Supplier<T> factory;

        private MiniTileEntity(String name, Supplier<T> factory)
        {
            this.name = name;
            this.factory = factory;
        }

        public RegistryObject<T> defer()
        {
            return TILE_ENTITIES.get().register(name, factory);
        }
    }
}

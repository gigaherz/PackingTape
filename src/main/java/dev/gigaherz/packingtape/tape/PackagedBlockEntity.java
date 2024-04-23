package dev.gigaherz.packingtape.tape;

import dev.gigaherz.packingtape.PackingTapeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PackagedBlockEntity extends BlockEntity
{
    private static Logger LOGGER = LogManager.getLogger();

    private BlockState containedBlockState;
    private CompoundTag containedTile;
    private Direction preferredDirection;

    public PackagedBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public PackagedBlockEntity(BlockPos pos, BlockState state)
    {
        super(PackingTapeMod.PACKAGED_BLOCK_ENTITY.get(), pos, state);
    }

    public BlockState getContainedBlockState()
    {
        return containedBlockState;
    }

    public CompoundTag getContainedTile()
    {
        return containedTile;
    }

    @Nullable
    public Direction getPreferredDirection()
    {
        return preferredDirection;
    }

    public void setPreferredDirection(Direction preferredDirection)
    {
        this.preferredDirection = preferredDirection;
    }

    @NotNull
    private ContainedBlockData makeContainedData()
    {
        return new ContainedBlockData(
                Objects.requireNonNull(containedBlockState),
                Objects.requireNonNullElseGet(containedTile, CompoundTag::new),
                Optional.ofNullable(preferredDirection));
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider)
    {
        super.saveAdditional(compound, provider);

        if (containedBlockState != null)
        {
            CompoundTag blockData = NbtUtils.writeBlockState(containedBlockState);
            compound.put("Block", blockData);
            compound.put("BlockEntity", containedTile.copy());
            if (preferredDirection != null)
            {
                compound.putInt("PreferredDirection", preferredDirection.ordinal());
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider provider)
    {
        super.loadAdditional(compound, provider);

        HolderGetter<Block> holdergetter = this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup();

        CompoundTag blockTag = compound.getCompound("Block");
        containedBlockState = NbtUtils.readBlockState(holdergetter, blockTag);
        containedTile = compound.getCompound("BlockEntity").copy();
        if (compound.contains("PreferredDirection"))
        {
            preferredDirection = Direction.byName(compound.getString("PreferredDirection"));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input)
    {
        var data = input.get(PackingTapeMod.CONTAINED_BLOCK);
        if (data != null)
        {
            containedBlockState = data.state();
            containedTile = data.blockEntityTag();
            preferredDirection = data.preferredDirection().orElse(null);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder collector)
    {
        if (containedBlockState != null)
        {
            collector.set(PackingTapeMod.CONTAINED_BLOCK, makeContainedData());
        }
    }

    public void setContents(BlockState state, CompoundTag tag)
    {
        containedBlockState = state;
        containedTile = tag;
    }

    public ItemStack getPackedStack()
    {
        ItemStack stack = new ItemStack(PackingTapeMod.PACKAGED_BLOCK.get());

        stack.set(PackingTapeMod.CONTAINED_BLOCK, makeContainedData());

        LOGGER.debug(String.format("Created Packed stack with %s", containedBlockState.toString()));

        return stack;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider)
    {
        this.loadWithComponents(tag, provider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider)
    {
        handleUpdateTag(pkt.getTag(), provider);
    }

    public boolean isEmpty()
    {
        return containedBlockState == null || containedBlockState.isAir();
    }
}
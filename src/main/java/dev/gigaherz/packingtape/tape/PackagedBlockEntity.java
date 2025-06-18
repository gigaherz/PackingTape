package dev.gigaherz.packingtape.tape;

import dev.gigaherz.packingtape.PackingTapeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private Direction preferredAll;
    private Direction preferredHorizontal;

    public PackagedBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public PackagedBlockEntity(BlockPos pos, BlockState state)
    {
        super(PackingTapeMod.PACKAGED_BLOCK_ENTITY.get(), pos, state);
    }

    public void setContents(BlockState state, CompoundTag tag)
    {
        containedBlockState = state;
        containedTile = tag;
    }

    @Nullable
    public BlockState getContainedBlockState()
    {
        return containedBlockState;
    }

    @Nullable
    public CompoundTag getContainedTile()
    {
        return containedTile;
    }

    @Nullable
    public Direction getPreferredDirectionAll()
    {
        return preferredAll;
    }

    @Nullable
    public Direction getPreferredDirectionHorizontal()
    {
        return preferredHorizontal;
    }

    public void setPreferredDirection(Direction preferredAll, Direction preferredHorizontal)
    {
        this.preferredAll = preferredAll;
        this.preferredHorizontal = preferredHorizontal;
    }

    @Override
    public void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);

        if (containedBlockState != null)
        {
            output.store("Block", BlockState.CODEC, containedBlockState);
            output.store("BlockEntity", CompoundTag.CODEC, containedTile.copy());
            if (preferredAll != null)
            {
                output.putString("PreferredDirection", preferredAll.getSerializedName());
            }
            if (preferredHorizontal != null)
            {
                output.putString("PreferredHorizontal", preferredHorizontal.getSerializedName());
            }
        }
    }

    @Override
    public void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);

        var blockStateOpt = input.read("Block", BlockState.CODEC);
        if (blockStateOpt.isEmpty())
            return;
        containedBlockState = blockStateOpt.get();
        var blockEntityTagOpt = input.read("BlockEntity", CompoundTag.CODEC);
        if (blockEntityTagOpt.isEmpty())
            return;
        containedTile = blockEntityTagOpt.get().copy();
        preferredAll = input.getString("PreferredDirection").map(Direction::byName).orElse(null);
        preferredHorizontal = input.getString("PreferredHorizontal").map(Direction::byName).orElse(null);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input)
    {
        var data = input.get(PackingTapeMod.CONTAINED_BLOCK);
        if (data != null)
        {
            containedBlockState = data.state();
            containedTile = data.blockEntityTag();
            preferredAll = data.preferredAll().orElse(null);
            preferredHorizontal = data.preferredHorizontal().orElse(null);
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

    @NotNull
    private ContainedBlockData makeContainedData()
    {
        return new ContainedBlockData(
                Objects.requireNonNull(containedBlockState),
                Objects.requireNonNullElseGet(containedTile, CompoundTag::new),
                Optional.ofNullable(preferredAll),
                Optional.ofNullable(preferredHorizontal));
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
    public void handleUpdateTag(ValueInput input)
    {
        this.loadWithComponents(input);
    }



    @Override
    public void onDataPacket(Connection net, ValueInput input)
    {
        handleUpdateTag(input);
    }

    public boolean isEmpty()
    {
        return containedBlockState == null || containedBlockState.isAir();
    }
}
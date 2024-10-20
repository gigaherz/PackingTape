package dev.gigaherz.packingtape.tape;

import dev.gigaherz.packingtape.ConfigValues;
import dev.gigaherz.packingtape.PackingTapeMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

public class PackagedBlock extends Block implements EntityBlock
{
    private static Logger LOGGER = LogManager.getLogger();

    public static final BooleanProperty UNPACKING = BooleanProperty.create("unpacking");

    public PackagedBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.getStateDefinition().any().setValue(UNPACKING, false));
    }

    @Deprecated
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
    {
        return state.getValue(UNPACKING);
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new PackagedBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(UNPACKING);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
    {
        if (player instanceof FakePlayer || player.level().isClientSide && ClientKeys.isStrictPicking(player))
            return new ItemStack(asItem(), 1);
        else
            return new ItemStack(PackingTapeMod.TAPE.get(), 1);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player)
    {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof PackagedBlockEntity packaged)
        {
            if (!world.isClientSide && player.isCreative() && !packaged.isEmpty())
            {
                ItemStack stack = packaged.getPackedStack();
                ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
            }
        }

        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        if (placer instanceof Player player && !placer.isShiftKeyDown())
        {
            PackagedBlockEntity te = (PackagedBlockEntity) worldIn.getBlockEntity(pos);
            assert te != null;
            var allDirection = Direction.getNearest(player.getLookAngle()).getOpposite();
            var horizontalDirection = player.getDirection().getOpposite();
            te.setPreferredDirection(allDirection, horizontalDirection);
        }
        super.setPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        return use(level, pos, player).result();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        return use(level, pos, player);
    }

    public ItemInteractionResult use(Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        BlockEntity te = level.getBlockEntity(pos);

        if (!(te instanceof PackagedBlockEntity))
        {
            return displayBlockMissingError(level, pos);
        }

        PackagedBlockEntity packagedBlock = (PackagedBlockEntity) te;

        BlockState newState = packagedBlock.getContainedBlockState();
        CompoundTag entityData = packagedBlock.getContainedTile();
        Direction preferredAll = packagedBlock.getPreferredDirectionAll();
        Direction preferredHorizontal = packagedBlock.getPreferredDirectionHorizontal();

        if (newState == null || entityData == null)
        {
            return displayBlockMissingError(level, pos);
        }

        EnumProperty<Direction> facing = findFacingProperty(newState);

        if (facing != null)
        {
            if (preferredAll != null && facing.getPossibleValues().contains(preferredAll))
            {
                newState = newState.setValue(facing, preferredAll);
            }
            else if (preferredHorizontal != null && facing.getPossibleValues().contains(preferredHorizontal))
            {
                newState = newState.setValue(facing, preferredHorizontal);
            }
        }

        if (facing != null
                && !player.isShiftKeyDown()
                && newState.getBlock() instanceof ChestBlock)
        {
            if (newState.hasProperty(ChestBlock.TYPE))
            {
                Direction chestFacing = newState.getValue(facing);

                Direction left = chestFacing.getClockWise();
                Direction right = chestFacing.getCounterClockWise();

                // test left side connection
                BlockState leftState = level.getBlockState(pos.relative(left));
                if (leftState.getBlock() == newState.getBlock()
                        && leftState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                        && leftState.getValue(ChestBlock.FACING) == chestFacing)
                {
                    level.setBlockAndUpdate(pos.relative(left), leftState.setValue(ChestBlock.TYPE, ChestType.RIGHT));
                    newState = newState.setValue(ChestBlock.TYPE, ChestType.LEFT);
                }
                else
                {
                    // test right side connection
                    BlockState rightState = level.getBlockState(pos.relative(right));
                    if (rightState.getBlock() == newState.getBlock()
                            && rightState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                            && rightState.getValue(ChestBlock.FACING) == chestFacing)
                    {
                        level.setBlockAndUpdate(pos.relative(right), rightState.setValue(ChestBlock.TYPE, ChestType.LEFT));
                        newState = newState.setValue(ChestBlock.TYPE, ChestType.RIGHT);
                    }
                }
            }
        }

        level.removeBlockEntity(pos);
        level.setBlockAndUpdate(pos, newState);

        setTileEntityNBT(level, player, pos, entityData);

        return ItemInteractionResult.SUCCESS;
    }

    public static @org.jetbrains.annotations.Nullable EnumProperty<Direction> findFacingProperty(BlockState newState)
    {
        EnumProperty<Direction> facing = null;
        for (Property<?> prop : newState.getProperties())
        {
            if (prop.getName().equalsIgnoreCase("facing") || prop.getName().equalsIgnoreCase("rotation"))
            {
                if (prop instanceof EnumProperty && prop.getValueClass() == Direction.class)
                {
                    //noinspection unchecked
                    facing = (EnumProperty<Direction>) prop;
                    break;
                }
            }
        }
        return facing;
    }

    private ItemInteractionResult displayBlockMissingError(Level world, BlockPos pos)
    {
        LOGGER.error("The packaged block does not contain valid data");
        world.addParticle(ParticleTypes.ANGRY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return ItemInteractionResult.CONSUME;
    }

    public static void setTileEntityNBT(Level level, @Nullable Player player, BlockPos pos, CompoundTag compoundtag)
    {
        if (level.isClientSide)
            return;

        MinecraftServer minecraftserver = level.getServer();
        if (minecraftserver == null)
        {
            return;
        }

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity != null)
        {
            if (blockentity.onlyOpCanSetNbt() && player != null && !player.canUseGameMasterBlocks() && !ConfigValues.isBlockEntityWhitelisted(blockentity))
            {
                return;
            }

            CompoundTag current = blockentity.saveWithoutMetadata(level.registryAccess());
            CompoundTag original = current.copy();
            current.merge(compoundtag);
            if (!current.equals(original))
            {
                blockentity.loadWithComponents(current, level.registryAccess());
                blockentity.setChanged();
            }
        }
    }

    private static Component makeError(String detail)
    {
        return Component.translatable("text.packingtape.packaged.missing_data", Component.translatable(detail));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced)
    {
        ContainedBlockData data = stack.get(PackingTapeMod.CONTAINED_BLOCK);
        if (data == null)
        {
            tooltip.add(makeError("text.packingtape.packaged.no_nbt"));
            return;
        }

        Block block = data.getBlock();
        ResourceLocation blockName = BuiltInRegistries.BLOCK.getKey(block);
        if (block == Blocks.AIR)
        {
            tooltip.add(Component.translatable("text.packingtape.packaged.unknown_block"));
            tooltip.add(Component.literal("  " + blockName));
            return;
        }

        Item item = block.asItem();
        if (item == Items.AIR)
        {
            item = BuiltInRegistries.ITEM.get(blockName);
            if (item == Items.AIR)
            {
                tooltip.add(Component.translatable("text.packingtape.packaged.no_item"));
                tooltip.add(Component.literal("  " + blockName));
                return;
            }
        }

        ItemStack stack1 = new ItemStack(item, 1);
        tooltip.add(Component.translatable("text.packingtape.packaged.contains", stack1.getHoverName()));
    }

    private static class ClientKeys
    {
        public static boolean isStrictPicking(Player player)
        {
            return Screen.hasShiftDown() || (player.getAbilities().instabuild && Screen.hasControlDown());
        }
    }
}

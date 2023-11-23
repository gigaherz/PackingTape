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
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player)
    {
        if (Screen.hasShiftDown() || (player.getAbilities().instabuild && Screen.hasControlDown()))
            return new ItemStack(asItem(), 1);
        else
            return new ItemStack(PackingTapeMod.TAPE.get(), 1);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player)
    {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof PackagedBlockEntity)
        {
            PackagedBlockEntity packaged = (PackagedBlockEntity) te;
            if (!world.isClientSide && player.isCreative() && !packaged.isEmpty())
            {
                ItemStack stack = packaged.getPackedStack();
                ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
            }
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        if (placer instanceof Player && !placer.isShiftKeyDown())
        {
            Player player = (Player) placer;
            PackagedBlockEntity te = (PackagedBlockEntity) worldIn.getBlockEntity(pos);
            assert te != null;
            te.setPreferredDirection(Direction.fromYRot(player.getYHeadRot()).getOpposite());
        }
        super.setPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockRayTraceResult)
    {
        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity te = world.getBlockEntity(pos);

        if (!(te instanceof PackagedBlockEntity))
        {
            return displayBlockMissingError(world, pos);
        }

        PackagedBlockEntity packagedBlock = (PackagedBlockEntity) te;

        BlockState newState = packagedBlock.getContainedBlockState();
        CompoundTag entityData = packagedBlock.getContainedTile();
        Direction preferred = packagedBlock.getPreferredDirection();

        if (newState == null || entityData == null)
        {
            return displayBlockMissingError(world, pos);
        }

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

        if (preferred != null && facing != null)
        {
            if (facing.getPossibleValues().contains(preferred))
            {
                newState = newState.setValue(facing, preferred);
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
                BlockState leftState = world.getBlockState(pos.relative(left));
                if (leftState.getBlock() == newState.getBlock()
                        && leftState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                        && leftState.getValue(ChestBlock.FACING) == chestFacing)
                {
                    world.setBlockAndUpdate(pos.relative(left), leftState.setValue(ChestBlock.TYPE, ChestType.RIGHT));
                    newState = newState.setValue(ChestBlock.TYPE, ChestType.LEFT);
                }
                else
                {
                    // test right side connection
                    BlockState rightState = world.getBlockState(pos.relative(right));
                    if (rightState.getBlock() == newState.getBlock()
                            && rightState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                            && rightState.getValue(ChestBlock.FACING) == chestFacing)
                    {
                        world.setBlockAndUpdate(pos.relative(right), rightState.setValue(ChestBlock.TYPE, ChestType.LEFT));
                        newState = newState.setValue(ChestBlock.TYPE, ChestType.RIGHT);
                    }
                }
            }
        }

        world.removeBlockEntity(pos);
        world.setBlockAndUpdate(pos, newState);

        setTileEntityNBT(world, player, pos, entityData);

        return InteractionResult.SUCCESS;
    }

    private InteractionResult displayBlockMissingError(Level world, BlockPos pos)
    {
        LOGGER.error("The packaged block does not contain valid data");
        world.addParticle(ParticleTypes.ANGRY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return InteractionResult.CONSUME;
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

            CompoundTag current = blockentity.saveWithoutMetadata();
            CompoundTag original = current.copy();
            current.merge(compoundtag);
            if (!current.equals(original))
            {
                blockentity.load(current);
                blockentity.setChanged();
            }
        }
    }

    private static Component makeError(String detail)
    {
        return Component.translatable("text.packingtape.packaged.missing_data", Component.translatable(detail));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter worldIn, List<Component> tooltip, TooltipFlag advanced)
    {
        super.appendHoverText(stack, worldIn, tooltip, advanced);

        CompoundTag tag = stack.getTag();
        if (tag == null)
        {
            tooltip.add(makeError("text.packingtape.packaged.no_nbt"));
            return;
        }

        CompoundTag info = (CompoundTag) tag.get("BlockEntityTag");
        if (info == null)
        {
            tooltip.add(makeError("text.packingtape.packaged.no_tag"));
            return;
        }

        if (!info.contains("Block") || !info.contains("BlockEntity"))
        {
            tooltip.add(makeError("text.packingtape.packaged.no_block"));
            return;
        }

        String blockName = info.getCompound("Block").getString("Name");

        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockName));
        if (block == null || block == Blocks.AIR)
        {
            tooltip.add(Component.translatable("text.packingtape.packaged.unknown_block"));
            tooltip.add(Component.literal("  " + blockName));
            return;
        }

        Item item = block.asItem();
        if (item == Items.AIR)
        {
            item = BuiltInRegistries.ITEM.get(BuiltInRegistries.BLOCK.getKey(block));
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
}

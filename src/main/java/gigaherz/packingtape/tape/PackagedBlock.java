package gigaherz.packingtape.tape;

import gigaherz.packingtape.ConfigValues;
import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.ChestType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class PackagedBlock extends Block
{
    public static final BooleanProperty UNPACKING = BooleanProperty.create("unpacking");

    public PackagedBlock(Properties properties)
    {
        super(properties);
        setDefaultState(this.getStateContainer().getBaseState().with(UNPACKING, false));
    }

    @Deprecated
    @Override
    public boolean isReplaceable(BlockState state, BlockItemUseContext useContext)
    {
        return state.get(UNPACKING);
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return new PackagedBlockEntity();
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(UNPACKING);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player)
    {
        if (player.abilities.isCreativeMode && Screen.hasShiftDown())
            return new ItemStack(asItem(), 1);
        else
            return new ItemStack(PackingTapeMod.TAPE.get(), 1);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof PackagedBlockEntity) {
            PackagedBlockEntity packaged = (PackagedBlockEntity)te;
            if (!world.isRemote && player.isCreative() && !packaged.isEmpty()) {
                ItemStack stack = packaged.getPackedStack();
                ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemEntity.setDefaultPickupDelay();
                world.addEntity(itemEntity);
            }
        }

        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        if (!placer.isSneaking() && placer instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) placer;
            PackagedBlockEntity te = (PackagedBlockEntity) worldIn.getTileEntity(pos);
            assert te != null;
            te.setPreferredDirection(Direction.fromAngle(player.getRotationYawHead()).getOpposite());
        }
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult blockRayTraceResult)
    {
        if (world.isRemote)
            return ActionResultType.SUCCESS;

        TileEntity te = world.getTileEntity(pos);

        if (!(te instanceof PackagedBlockEntity))
            return ActionResultType.FAIL;

        PackagedBlockEntity packagedBlock = (PackagedBlockEntity)te;

        BlockState newState = packagedBlock.getContainedBlockState();
        CompoundNBT entityData = packagedBlock.getContainedTile();
        Direction preferred = packagedBlock.getPreferredDirection();

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
            if (facing.getAllowedValues().contains(preferred))
            {
                newState = newState.with(facing, preferred);
            }
        }

        if (facing != null
                && !player.isSneaking()
                && newState.getBlock() instanceof ChestBlock)
        {
            if (newState.hasProperty(ChestBlock.TYPE))
            {
                Direction chestFacing = newState.get(facing);

                Direction left = chestFacing.rotateY();
                Direction right = chestFacing.rotateYCCW();

                // test left side connection
                BlockState leftState = world.getBlockState(pos.offset(left));
                if (leftState.getBlock() == newState.getBlock()
                        && leftState.get(ChestBlock.TYPE) == ChestType.SINGLE
                        && leftState.get(ChestBlock.FACING) == chestFacing)
                {
                    world.setBlockState(pos.offset(left), leftState.with(ChestBlock.TYPE, ChestType.RIGHT));
                    newState = newState.with(ChestBlock.TYPE, ChestType.LEFT);
                }
                else
                {
                    // test right side connection
                    BlockState rightState = world.getBlockState(pos.offset(right));
                    if (rightState.getBlock() == newState.getBlock()
                            && rightState.get(ChestBlock.TYPE) == ChestType.SINGLE
                            && rightState.get(ChestBlock.FACING) == chestFacing)
                    {
                        world.setBlockState(pos.offset(right), rightState.with(ChestBlock.TYPE, ChestType.LEFT));
                        newState = newState.with(ChestBlock.TYPE, ChestType.RIGHT);
                    }
                }
            }
        }

        world.removeTileEntity(pos);
        world.setBlockState(pos, newState);

        setTileEntityNBT(world, pos, newState, entityData, player);

        return ActionResultType.SUCCESS;
    }

    public static void setTileEntityNBT(World worldIn, BlockPos pos, BlockState state,
                                           @Nullable CompoundNBT tag,
                                           @Nullable PlayerEntity playerIn)
    {
        MinecraftServer minecraftserver = worldIn.getServer();
        if (minecraftserver == null)
        {
            return;
        }

        if (tag != null)
        {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity != null)
            {
                if (ConfigValues.isTileEntityBlocked(tileentity) && (playerIn == null || !playerIn.canUseCommandBlock()))
                {
                    return;
                }

                CompoundNBT merged = new CompoundNBT();
                CompoundNBT empty = merged.copy();
                tileentity.write(merged);
                merged.merge(tag);
                merged.putInt("x", pos.getX());
                merged.putInt("y", pos.getY());
                merged.putInt("z", pos.getZ());

                if (!merged.equals(empty))
                {
                    tileentity.read(state, merged);
                    tileentity.markDirty();
                }
            }
        }
    }

    private static ITextComponent makeError(String detail)
    {
        return new TranslationTextComponent("text.packingtape.packaged.missing_data", new TranslationTextComponent(detail));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag advanced)
    {
        super.addInformation(stack, worldIn, tooltip, advanced);

        CompoundNBT tag = stack.getTag();
        if (tag == null)
        {
            tooltip.add(makeError("text.packingtape.packaged.no_nbt"));
            return;
        }

        CompoundNBT info = (CompoundNBT) tag.get("BlockEntityTag");
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

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
        if (block == null || block == Blocks.AIR)
        {
            tooltip.add(new TranslationTextComponent("text.packingtape.packaged.unknown_block"));
            tooltip.add(new StringTextComponent("  " + blockName));
            return;
        }

        Item item = block.asItem();
        if (item == Items.AIR)
        {
            item = ForgeRegistries.ITEMS.getValue(block.getRegistryName());
            if (item == Items.AIR)
            {
                tooltip.add(new TranslationTextComponent("text.packingtape.packaged.no_item"));
                tooltip.add(new StringTextComponent("  " + blockName));
                return;
            }
        }

        ItemStack stack1 = new ItemStack(item, 1);
        tooltip.add(new TranslationTextComponent("text.packingtape.packaged.contains", stack1.getDisplayName()));
    }
}

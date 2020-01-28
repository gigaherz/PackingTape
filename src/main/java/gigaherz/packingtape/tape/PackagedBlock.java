package gigaherz.packingtape.tape;

import gigaherz.packingtape.ConfigValues;
import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.block.*;
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
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.ChestType;
import net.minecraft.tileentity.ShulkerBoxTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
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
        if (player.abilities.isCreativeMode && Screen.hasControlDown())
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
        if (!placer.isShiftKeyDown() && placer instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) placer;
            PackagedBlockEntity te = (PackagedBlockEntity) worldIn.getTileEntity(pos);
            assert te != null;
            te.setPreferredDirection(Direction.fromAngle(player.getRotationYawHead()).getOpposite());
        }
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState p_225533_1_, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult blockRayTraceResult)
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
        for (IProperty<?> prop : newState.getProperties())
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
                && !player.isShiftKeyDown()
                && newState.getBlock() instanceof ChestBlock)
        {
            if (newState.getProperties().contains(ChestBlock.TYPE))
            {
                Direction chestFacing = newState.get(facing);

                Direction left = chestFacing.rotateY();
                Direction right = chestFacing.rotateYCCW();

                // test left side connection
                BlockState leftState = world.getBlockState(pos.offset(left));
                if (leftState.getBlock() == newState.getBlock() && leftState.get(ChestBlock.TYPE) == ChestType.SINGLE)
                {
                    world.setBlockState(pos.offset(left), leftState.with(ChestBlock.TYPE, ChestType.RIGHT));
                    newState = newState.with(ChestBlock.TYPE, ChestType.LEFT);
                }
                else
                {
                    // test right side connection
                    BlockState rightState = world.getBlockState(pos.offset(right));
                    if (rightState.getBlock() == newState.getBlock() && rightState.get(ChestBlock.TYPE) == ChestType.SINGLE)
                    {
                        world.setBlockState(pos.offset(left), rightState.with(ChestBlock.TYPE, ChestType.LEFT));
                        newState = newState.with(ChestBlock.TYPE, ChestType.RIGHT);
                    }
                }
            }
        }

        world.removeTileEntity(pos);
        world.setBlockState(pos, newState);

        setTileEntityNBT(world, pos, entityData, player);

        return ActionResultType.SUCCESS;
    }

    public static void setTileEntityNBT(World worldIn, BlockPos pos,
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
                if (!ConfigValues.isTileEntityAllowed(tileentity) && (playerIn == null || !playerIn.canUseCommandBlock()))
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
                    tileentity.read(merged);
                    tileentity.markDirty();
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag advanced)
    {
        super.addInformation(stack, worldIn, tooltip, advanced);

        CompoundNBT tag = stack.getTag();
        if (tag == null)
        {
            tooltip.add(new StringTextComponent("Missing data (no nbt)!"));
            return;
        }

        CompoundNBT info = (CompoundNBT) tag.get("BlockEntityTag");
        if (info == null)
        {
            tooltip.add(new StringTextComponent("Missing data (no tag)!"));
            return;
        }

        if (!info.contains("Block") || !info.contains("BlockEntity"))
        {
            tooltip.add(new StringTextComponent("Missing data (no block info)!"));
            return;
        }

        String blockName = info.getCompound("Block").getString("Name");

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
        if (block == Blocks.AIR)
        {
            tooltip.add(new StringTextComponent("Unknown block:"));
            tooltip.add(new StringTextComponent("  " + blockName));
            return;
        }

        Item item = block.asItem();
        if (item == Items.AIR)
        {
            item = ForgeRegistries.ITEMS.getValue(block.getRegistryName());
            if (item == Items.AIR)
            {
                tooltip.add(new StringTextComponent("Can't find item for:"));
                tooltip.add(new StringTextComponent("  " + blockName));
                return;
            }
        }

        ItemStack stack1 = new ItemStack(item, 1);
        tooltip.add(new TranslationTextComponent("text.packingtape.packaged.contains", stack1.getDisplayName()));
    }
}

package gigaherz.packingtape.tape;

import gigaherz.packingtape.PackingTapeMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.*;
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
        if (player.abilities.isCreativeMode && Screen.hasControlDown())
            return new ItemStack(asItem(), 1);
        else
            return new ItemStack(PackingTapeMod.Items.TAPE, 1);
    }

    //@Override
    public void getDrops(NonNullList<ItemStack> drops, @Nullable TileEntity teWorld)
    {
        if (teWorld instanceof PackagedBlockEntity)
        {
            // TE exists here thanks to the willHarvest above.
            PackagedBlockEntity packaged = (PackagedBlockEntity) teWorld;
            ItemStack stack = packaged.getPackedStack();

            drops.add(stack);
        }
    }

    @Override
    public void harvestBlock(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        player.addStat(Stats.BLOCK_MINED.get(this));
        player.addExhaustion(0.005F);

        NonNullList<ItemStack> items = NonNullList.create();
        getDrops(items, te);
        boolean isSilkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, 0, 1.0f, isSilkTouch, player);
        items.forEach(e -> spawnAsEntity(worldIn, pos, e));
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

    @Deprecated
    @Override
    public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (worldIn.isRemote)
            return true;

        PackagedBlockEntity te = (PackagedBlockEntity) worldIn.getTileEntity(pos);
        assert te != null;

        BlockState newState = te.getContainedBlockState();

        CompoundNBT entityData = te.getContainedTile();

        worldIn.removeTileEntity(pos);
        worldIn.setBlockState(pos, newState);

        Direction preferred = te.getPreferredDirection();
        if (preferred != null)
        {
            EnumProperty facing = null;
            for (IProperty prop : newState.getProperties())
            {
                if (prop.getName().equalsIgnoreCase("facing") || prop.getName().equalsIgnoreCase("rotation"))
                {
                    if (prop instanceof EnumProperty)
                    {
                        facing = (EnumProperty) prop;
                        break;
                    }
                }
            }

            if (facing != null)
            {
                if (facing.getValueClass() == Direction.class && facing.getAllowedValues().contains(preferred))
                {
                    if (!rotateBlockToward(worldIn, pos, preferred, facing))
                    {
                        worldIn.setBlockState(pos, newState.with(facing, preferred));
                    }
                }
            }
        }

        setTileEntityNBT(worldIn, pos, entityData, player);

        return true;
    }

    private static boolean rotateBlockToward(World worldIn, BlockPos pos, Direction preferred, EnumProperty prop)
    {
        /*BlockState stored = worldIn.getBlockState(pos);
        Block block = stored.getBlock();
        if (stored.get(prop) == preferred)
        {
            return true;
        }

        for (Object ignored : prop.getAllowedValues())
        {
            if (preferred.getAxis() == Direction.Axis.Y)
                block.rotateBlock(stored, worldIn, pos, Direction.WEST);
            else
                block.rotateBlock(stored, worldIn, pos, Direction.UP);

            stored = worldIn.getBlockState(pos);
            block = stored.getBlock();
            if (stored.get(prop) == preferred)
            {
                return true;
            }
        }*/

        return false;
    }

    public static boolean setTileEntityNBT(World worldIn, BlockPos pos,
                                           @Nullable CompoundNBT tag,
                                           @Nullable PlayerEntity playerIn)
    {
        MinecraftServer minecraftserver = worldIn.getServer();

        if (minecraftserver == null)
        {
            return false;
        }
        else
        {
            if (tag != null)
            {
                TileEntity tileentity = worldIn.getTileEntity(pos);

                if (tileentity != null)
                {
                    if (!worldIn.isRemote && tileentity.onlyOpsCanSetNbt() && (playerIn == null || !playerIn.canUseCommandBlock()))
                    {
                        return false;
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
                        return true;
                    }
                }
            }

            return false;
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

package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class BlockPackaged extends Block implements ITileEntityProvider
{
    public static final BooleanProperty UNPACKING = BooleanProperty.create("unpacking");

    public BlockPackaged()
    {
        super(Block.Properties.create(Material.CLOTH)
                .hardnessAndResistance(0.5f,0.5f).sound(SoundType.WOOD));
        setDefaultState(this.getStateContainer().getBaseState().with(UNPACKING, false));
    }

    @Deprecated
    @Override
    public boolean isReplaceable(IBlockState state, BlockItemUseContext useContext)
    {
        return state.get(UNPACKING);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    //@Override
    public TileEntity createTileEntity(IBlockReader world, IBlockState state)
    {
        return new TilePackaged();
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder)
    {
        builder.add(UNPACKING);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, EntityPlayer player)
    {
        if (player.abilities.isCreativeMode && GuiScreen.isCtrlKeyDown())
            return new ItemStack(asItem(), 1);
        else
            return new ItemStack(ModPackingTape.itemTape, 1);
    }

    //@Override
    public void getDrops(NonNullList<ItemStack> drops, @Nullable TileEntity teWorld)
    {
        if (teWorld instanceof TilePackaged)
        {
            // TE exists here thanks to the willHarvest above.
            TilePackaged packaged = (TilePackaged) teWorld;
            ItemStack stack = packaged.getPackedStack();

            drops.add(stack);
        }
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        player.addStat(StatList.BLOCK_MINED.get(this));
        player.addExhaustion(0.005F);

        NonNullList<ItemStack> items = NonNullList.create();
        getDrops(items, te);
        boolean isSilkTouch = this.canSilkHarvest(state, worldIn, pos, player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, 0, 1.0f, isSilkTouch, player);
        items.forEach(e -> spawnAsEntity(worldIn, pos, e));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        if (!placer.isSneaking() && placer instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) placer;
            TilePackaged te = (TilePackaged) worldIn.getTileEntity(pos);
            assert te != null;
            te.setPreferredDirection(EnumFacing.fromAngle(player.getRotationYawHead()).getOpposite());
        }
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

    @Override
    public boolean onBlockActivated(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (worldIn.isRemote)
            return true;

        TilePackaged te = (TilePackaged) worldIn.getTileEntity(pos);
        assert te != null;

        ResourceLocation containedBlock = te.getContainedBlock();

        if (containedBlock == null)
            return false;

        if (!ForgeRegistries.BLOCKS.containsKey(containedBlock))
            return false;

        Block b = ForgeRegistries.BLOCKS.getValue(containedBlock);
        if (b == null)
            return false;

        /*worldIn.setBlockState(pos, state.with(UNPACKING, true), 0);
        if (!b.canPlaceBlockAt(worldIn, pos))
        {
            TextComponentTranslation textComponent = new TextComponentTranslation("text.packingtape.packaged.cant_place");
            playerIn.sendStatusMessage(textComponent, true);
            worldIn.setBlockState(pos, state.with(UNPACKING, false), 0);
            return false;
        }
        worldIn.setBlockState(pos, state.withProperty(UNPACKING, false), 0);*/

        IBlockState newState = te.getParsedBlockState(b);

        NBTTagCompound tag = te.getContainedTile();
        if (tag == null)
            return false;

        worldIn.removeTileEntity(pos);
        worldIn.setBlockState(pos, newState);

        EnumFacing preferred = te.getPreferredDirection();
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
                    }
                    break;
                }
            }

            if (facing != null)
            {
                if (facing.getValueClass() == EnumFacing.class && facing.getAllowedValues().contains(preferred))
                {
                    if (!rotateBlockToward(worldIn, pos, preferred, facing))
                    {
                        worldIn.setBlockState(pos, newState.with(facing, preferred));
                    }
                }
            }
        }

        setTileEntityNBT(worldIn, pos, tag, player);

        return true;
    }

    private static boolean rotateBlockToward(World worldIn, BlockPos pos, EnumFacing preferred, EnumProperty prop)
    {
        /*IBlockState stored = worldIn.getBlockState(pos);
        Block block = stored.getBlock();
        if (stored.get(prop) == preferred)
        {
            return true;
        }

        for (Object ignored : prop.getAllowedValues())
        {
            if (preferred.getAxis() == EnumFacing.Axis.Y)
                block.rotateBlock(stored, worldIn, pos, EnumFacing.WEST);
            else
                block.rotateBlock(stored, worldIn, pos, EnumFacing.UP);

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
                                           @Nullable NBTTagCompound tag,
                                           @Nullable EntityPlayer playerIn)
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

                    NBTTagCompound merged = new NBTTagCompound();
                    NBTTagCompound empty = merged.copy();
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

        NBTTagCompound tag = stack.getTag();
        if (tag == null)
        {
            tooltip.add(new TextComponentString("Missing data (no nbt)!"));
            return;
        }

        NBTTagCompound info = (NBTTagCompound) tag.get("BlockEntityTag");
        if (info == null)
        {
            tooltip.add(new TextComponentString("Missing data (no tag)!"));
            return;
        }

        if (!info.contains("containedBlock") ||
                !info.contains("containedTile"))
        {
            tooltip.add(new TextComponentString("Missing data (no block info)!"));
            return;
        }

        String blockName = info.getString("containedBlock");

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
        if (block == null)
        {
            tooltip.add(new TextComponentString("Unknown block:"));
            tooltip.add(new TextComponentString("  " + blockName));
            return;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null)
        {
            tooltip.add(new TextComponentString("No ItemBlock:"));
            tooltip.add(new TextComponentString("  " + blockName));
            return;
        }

        tooltip.add(new TextComponentString("Contains:"));
        ItemStack stack1 = new ItemStack(item, 1);
        for (ITextComponent s : stack1.getTooltip(Minecraft.getInstance().player, advanced))
        {
            tooltip.add(new TextComponentTranslation("  %s", s));
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn)
    {
        return createTileEntity(worldIn, getDefaultState());
    }
}

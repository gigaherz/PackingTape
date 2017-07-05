package gigaherz.packingtape.tape;

import gigaherz.common.BlockRegistered;
import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class BlockPackaged extends BlockRegistered
{
    public static final PropertyBool UNPACKING = PropertyBool.create("unpacking");

    public BlockPackaged(String name)
    {
        super(name, Material.CLOTH);
        setHardness(0.5F);
        setSoundType(SoundType.WOOD);
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
        return worldIn.getBlockState(pos).getValue(UNPACKING);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new TilePackaged();
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, UNPACKING);
    }

    @Deprecated
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
    {
        Item item = Item.getItemFromBlock(this);

        if (player.capabilities.isCreativeMode && GuiScreen.isCtrlKeyDown())
            return new ItemStack(item, 1);
        else
            return new ItemStack(ModPackingTape.itemTape, 1);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        // If it will harvest, delay deletion of the block until after getDrops.
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        TileEntity teWorld = world.getTileEntity(pos);
        if (teWorld != null && teWorld instanceof TilePackaged)
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
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        // Finished making use of the TE, so we can now safely destroy the block.
        worldIn.setBlockToAir(pos);
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
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
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

        worldIn.setBlockState(pos, state.withProperty(UNPACKING, true), 0);
        if (!b.canPlaceBlockAt(worldIn, pos))
        {
            ModPackingTape.proxy.showCantPlaceMessage();
            worldIn.setBlockState(pos, state.withProperty(UNPACKING, false), 0);
            return false;
        }
        worldIn.setBlockState(pos, state.withProperty(UNPACKING, false), 0);

        @SuppressWarnings("deprecation")
        IBlockState newState = b.getStateFromMeta(te.getContainedMetadata());

        NBTTagCompound tag = te.getContainedTile();
        if (tag == null)
            return false;

        worldIn.setBlockState(pos, newState);

        EnumFacing preferred = te.getPreferredDirection();
        if (preferred != null)
        {
            PropertyEnum facing = null;
            for (IProperty prop : newState.getPropertyKeys())
            {
                if (prop.getName().equalsIgnoreCase("facing") || prop.getName().equalsIgnoreCase("rotation"))
                {
                    if (prop instanceof PropertyEnum)
                    {
                        facing = (PropertyEnum) prop;
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
                        worldIn.setBlockState(pos, newState);
                    }
                }
            }
        }

        setTileEntityNBT(worldIn, pos, tag, playerIn);

        return true;
    }

    private static boolean rotateBlockToward(World worldIn, BlockPos pos, EnumFacing preferred, PropertyEnum prop)
    {
        IBlockState stored = worldIn.getBlockState(pos);
        Block block = stored.getBlock();
        IBlockState actual = stored.getActualState(worldIn, pos);
        if (actual.getValue(prop) == preferred)
        {
            return true;
        }

        for (Object ignored : prop.getAllowedValues())
        {
            if (preferred.getAxis() == EnumFacing.Axis.Y)
                block.rotateBlock(worldIn, pos, EnumFacing.WEST);
            else
                block.rotateBlock(worldIn, pos, EnumFacing.UP);

            stored = worldIn.getBlockState(pos);
            block = stored.getBlock();
            actual = stored.getActualState(worldIn, pos);
            if (actual.getValue(prop) == preferred)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean setTileEntityNBT(World worldIn, BlockPos pos,
                                           @Nullable NBTTagCompound tag,
                                           @Nullable EntityPlayer playerIn)
    {
        MinecraftServer minecraftserver = worldIn.getMinecraftServer();

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
                    tileentity.writeToNBT(merged);
                    merged.merge(tag);
                    merged.setInteger("x", pos.getX());
                    merged.setInteger("y", pos.getY());
                    merged.setInteger("z", pos.getZ());

                    if (!merged.equals(empty))
                    {
                        tileentity.readFromNBT(merged);
                        tileentity.markDirty();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag advanced)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
        {
            tooltip.add("Missing data (no nbt)!");
            return;
        }

        NBTTagCompound info = (NBTTagCompound) tag.getTag("BlockEntityTag");
        if (info == null)
        {
            tooltip.add("Missing data (no tag)!");
            return;
        }

        if (!info.hasKey("containedBlock", Constants.NBT.TAG_STRING) ||
                !info.hasKey("containedBlockMetadata", Constants.NBT.TAG_INT) ||
                !info.hasKey("containedTile", Constants.NBT.TAG_COMPOUND))
        {
            tooltip.add("Missing data (no block info)!");
            return;
        }

        String blockName = info.getString("containedBlock");
        int meta = info.getInteger("containedBlockMetadata");

        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName));
        if (block == null)
        {
            tooltip.add("Unknown block:");
            tooltip.add("  " + blockName);
            return;
        }

        Item item = Item.getItemFromBlock(block);
        if (item == null)
        {
            tooltip.add("No ItemBlock:");
            tooltip.add("  " + blockName);
            return;
        }

        tooltip.add("Contains:");
        ItemStack stack1 = new ItemStack(item, 1, meta);
        for (String s : stack1.getTooltip(Minecraft.getMinecraft().player, advanced))
        {
            tooltip.add("  " + s);
        }
    }
}

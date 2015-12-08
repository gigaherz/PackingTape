package gigaherz.packingtape.tape;

import gigaherz.packingtape.ModPackingTape;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlockPackaged extends Block
{
    public BlockPackaged() {
        super(Material.cloth);
        this.setUnlocalizedName(ModPackingTape.MODID + ".packedBlock");
        setHardness(0.5F);
        setStepSound(Block.soundTypeSand);
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

    ThreadLocal<TilePackaged> lastHarvestedTE = new ThreadLocal<TilePackaged>();

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te)
    {
        if(te instanceof TilePackaged)
        {
            lastHarvestedTE.set((TilePackaged)te);
        }
        super.harvestBlock(worldIn, player, pos, state, te);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player)
    {
        if(GuiScreen.isCtrlKeyDown())
            return new ItemStack(Item.getItemFromBlock(this),1);
        else
            return new ItemStack(ModPackingTape.itemTape,1);
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        List<ItemStack> drops = new ArrayList<ItemStack>();

        if(lastHarvestedTE.get() != null)
        {
            ItemStack stack = getPackedStack(lastHarvestedTE.get());

            drops.add(stack);

            lastHarvestedTE.set(null);
        }

        return drops;
    }

    private ItemStack getPackedStack(TilePackaged tilePackaged)
    {
        ItemStack stack = new ItemStack(Item.getItemFromBlock(this), 1);

        NBTTagCompound tag0 = new NBTTagCompound();
        NBTTagCompound tag = new NBTTagCompound();

        tilePackaged.writeToNBT(tag);

        tag.removeTag("x");
        tag.removeTag("y");
        tag.removeTag("z");

        tag0.setTag("BlockEntityTag", tag);
        stack.setTagCompound(tag0);

        ModPackingTape.logger.warn("Created Packed stack with " + tilePackaged.containedBlock + "[" + tilePackaged.containedBlockMetadata + "]");

        return stack;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if(worldIn.isRemote)
            return false;

        TilePackaged te = (TilePackaged) worldIn.getTileEntity(pos);

        if(te == null || te.containedBlock == null)
            return false;

        Block b = Block.getBlockFromName(te.containedBlock);
        if(b == null)
            return false;

        IBlockState newState = b.getStateFromMeta(te.containedBlockMetadata);
        if(newState == null)
            return false;

        NBTTagCompound tag = te.containedTile;
        if(tag == null)
            return false;

        EnumFacing preferred = te.getPreferredDirection();
        if(preferred != null)
        {
            for(IProperty prop : (Collection<IProperty>)newState.getPropertyNames())
            {
                if(prop.getName().equalsIgnoreCase("facing") && prop instanceof PropertyEnum)
                {
                    PropertyEnum pe = (PropertyEnum)prop;
                    if(pe.getValueClass() == EnumFacing.class && prop.getAllowedValues().contains(preferred))
                    {
                        newState = newState.withProperty(pe, preferred);
                    }

                    break;
                }
            }

        }

        worldIn.setBlockState(pos, newState);
        setTileEntityNBT(worldIn, pos, tag, playerIn);

        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        if(!placer.isSneaking() && placer instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer)placer;
            TilePackaged te = (TilePackaged) worldIn.getTileEntity(pos);
            te.setPreferredDirection(EnumFacing.fromAngle(player.getRotationYawHead()).getOpposite());
        }
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

    public static boolean setTileEntityNBT(World worldIn, BlockPos pos, NBTTagCompound tag, EntityPlayer player)
    {
        if (tag != null)
        {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity != null)
            {
                //Forge: Fixes  MC-75630 - Exploit with signs and command blocks
                final net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
                if (!worldIn.isRemote && tileentity.restrictNBTCopy() &&
                        (server == null || !server.getConfigurationManager().canSendCommands(player.getGameProfile())))
                    return false;
                NBTTagCompound existingData = new NBTTagCompound();
                NBTTagCompound originalTag = (NBTTagCompound)existingData.copy();
                tileentity.writeToNBT(existingData);
                existingData.merge(tag);
                existingData.setInteger("x", pos.getX());
                existingData.setInteger("y", pos.getY());
                existingData.setInteger("z", pos.getZ());

                if (!existingData.equals(originalTag))
                {
                    tileentity.readFromNBT(existingData);
                    tileentity.markDirty();
                    return true;
                }
            }
        }

        return false;
    }

}

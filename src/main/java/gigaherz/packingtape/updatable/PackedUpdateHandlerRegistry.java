package gigaherz.packingtape.updatable;

import com.google.common.collect.Maps;
import gigaherz.packingtape.tape.TilePackaged;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Map;

public class PackedUpdateHandlerRegistry
{
    public static Map<ResourceLocation, IPackedTickHandler> handlers = Maps.newHashMap();

    public static void register(@Nonnull ResourceLocation blockRegistrationName, @Nonnull IPackedTickHandler handler)
    {
        if(handlers.containsKey(blockRegistrationName))
            throw new KeyAlreadyExistsException("A handler for this ResourceLocation is already registered");

        handlers.put(blockRegistrationName, handler);
    }

    public static IPackedTickHandler find(EntityItem entityItem)
    {
        return find(entityItem.getEntityItem());
    }

    public static IPackedTickHandler find(TilePackaged packedData)
    {
        return handlers.get(packedData.getContainedBlock());
    }

    public static IPackedTickHandler find(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound info = (NBTTagCompound) tag.getTag("BlockEntityTag");
        String blockname = info.getString("containedBlock");

        if(blockname == null)
            return null;

        return handlers.get(new ResourceLocation(blockname));
    }

}

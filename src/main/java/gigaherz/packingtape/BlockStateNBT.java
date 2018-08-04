package gigaherz.packingtape;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;

import java.util.Map;

public class BlockStateNBT
{
    public static NBTBase encodeBlockState(IBlockState state)
    {
        ImmutableMap<IProperty<?>, Comparable<?>> props = state.getProperties();
        if (props.size() == 0)
            return new NBTTagString("normal");

        NBTTagCompound nbt = new NBTTagCompound();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : props.entrySet())
        {
            IProperty<?> prop = entry.getKey();
            nbt.setString(prop.getName(), getValueName(prop, entry.getValue()));
        }
        return nbt;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getValueName(IProperty<T> property, Comparable<?> value)
    {
        return property.getName((T) value);
    }

    public static IBlockState decodeBlockState(Block block, NBTBase data)
    {
        IBlockState state = block.getDefaultState();

        if (!(data instanceof NBTTagCompound)) // TODO: Can this receive a string other than "normal"?
            return state;

        BlockStateContainer container = block.getBlockState();

        NBTTagCompound nbt = (NBTTagCompound) data;
        for (String prop : nbt.getKeySet())
        {
            String value = nbt.getString(prop);
            IProperty<?> property = container.getProperty(prop);
            if (property == null)
                continue;
            Comparable<?> comparable = property.parseValue(value).orNull();
            state = applyProperty(state, property, comparable);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> IBlockState applyProperty(IBlockState baseState, IProperty<T> property, Comparable<?> value)
    {
        return baseState.withProperty(property, (T) value);
    }
}

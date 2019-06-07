package gigaherz.packingtape;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;

import java.util.Map;

public class BlockStateNBT
{
    public static INBT encodeBlockState(BlockState state)
    {
        ImmutableMap<IProperty<?>, Comparable<?>> props = state.getValues();
        if (props.size() == 0)
            return new StringNBT("normal");

        CompoundNBT nbt = new CompoundNBT();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : props.entrySet())
        {
            IProperty<?> prop = entry.getKey();
            nbt.putString(prop.getName(), getValueName(prop, entry.getValue()));
        }
        return nbt;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getValueName(IProperty<T> property, Comparable<?> value)
    {
        return property.getName((T) value);
    }

    public static BlockState decodeBlockState(Block block, INBT data)
    {
        BlockState state = block.getDefaultState();

        if (!(data instanceof CompoundNBT)) // TODO: Can this receive a string other than "normal"?
            return state;

        StateContainer container = block.getStateContainer();

        CompoundNBT nbt = (CompoundNBT) data;
        for (String prop : nbt.keySet())
        {
            String value = nbt.getString(prop);
            IProperty<?> property = container.getProperty(prop);
            if (property == null)
                continue;
            Comparable<?> comparable = property.parseValue(value).orElse(null);
            state = applyProperty(state, property, comparable);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState baseState, IProperty<T> property, Comparable<?> value)
    {
        return baseState.with(property, (T) value);
    }
}

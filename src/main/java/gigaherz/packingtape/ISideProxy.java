package gigaherz.packingtape;

import net.minecraft.entity.player.EntityPlayer;

public interface ISideProxy
{
    void showPaperMessage();

    void showCantPlaceMessage();

    default EntityPlayer getPlayer() { return null; }
}

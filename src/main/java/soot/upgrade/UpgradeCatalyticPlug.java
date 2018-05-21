package soot.upgrade;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import soot.capability.CapabilityUpgradeProvider;
import soot.capability.IUpgradeProvider;
import soot.tile.TileEntityCatalyticPlug;

import java.util.HashSet;
import java.util.List;

public class UpgradeCatalyticPlug extends CapabilityUpgradeProvider {
    public UpgradeCatalyticPlug(TileEntity tile) {
        super("catalytic_plug", tile);
    }
    private static HashSet<Class<? extends TileEntity>> blacklist = new HashSet<>();

    public static void registerBlacklistedTile(Class<? extends TileEntity> tile) {
        blacklist.add(tile);
    }

    @Override
    public int getLimit(TileEntity tile) {
        return blacklist.contains(tile.getClass()) ? 0 : 2;
    }

    @Override
    public double getSpeed(TileEntity tile, double speed) {
        return hasCatalyst() ? speed * 2.0 : speed; //+200% if catalyst available
    }

    @Override
    public boolean doWork(TileEntity tile, List<IUpgradeProvider> upgrades) {
        if(hasCatalyst() && this.tile instanceof TileEntityCatalyticPlug) {
            depleteCatalyst(1);
            ((TileEntityCatalyticPlug) this.tile).setActive(20);
        }
        return false; //No cancel
    }

    private boolean hasCatalyst() {
        if(!tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,null))
            return false;
        IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,null);
        return handler.drain(FluidRegistry.getFluidStack("alchemical_redstone",1),false) != null;
    }

    private void depleteCatalyst(int amt) {
        if(!tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,null))
            return;
        IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,null);
        handler.drain(FluidRegistry.getFluidStack("alchemical_redstone",amt),true);
    }
}

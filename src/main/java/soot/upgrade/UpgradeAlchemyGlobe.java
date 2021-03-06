package soot.upgrade;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import soot.SoundEvents;
import soot.network.PacketHandler;
import soot.network.message.MessageMixerFailFX;
import soot.recipe.RecipeAlchemicalMixer;
import soot.recipe.CraftingRegistry;
import soot.tile.TileEntityAlchemyGlobe;
import teamroots.embers.api.alchemy.AlchemyResult;
import teamroots.embers.api.upgrades.IUpgradeProvider;
import teamroots.embers.api.upgrades.UpgradeUtil;
import teamroots.embers.tileentity.TileEntityMixerBottom;
import teamroots.embers.tileentity.TileEntityMixerTop;
import teamroots.embers.util.DefaultUpgradeProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class UpgradeAlchemyGlobe extends DefaultUpgradeProvider {
    public UpgradeAlchemyGlobe(TileEntity tile) {
        super("alchemy_globe", tile);
    }

    int failure = 0;

    @Override
    public int getLimit(TileEntity tile) {
        return 1;
    }

    @Override
    public boolean doWork(TileEntity tile, List<IUpgradeProvider> upgrades) {
        if(failure > 0)
            failure--;

        if (tile instanceof TileEntityMixerBottom && failure <= 0) //Only works with the fixed up class
        {
            doAlchemicMixing((TileEntityMixerBottom) tile, upgrades);
            return true; //Block normal mixing.
        }

        return false;
    }

    public void doAlchemicMixing(TileEntityMixerBottom bottom, List<IUpgradeProvider> upgrades)
    {
        if(!(tile instanceof TileEntityAlchemyGlobe))
            return;
        TileEntityAlchemyGlobe globe = (TileEntityAlchemyGlobe) this.tile;
        World world = bottom.getWorld();
        TileEntityMixerTop top = (TileEntityMixerTop) world.getTileEntity(bottom.getPos().up());
        if (top != null) {
            double emberCost = UpgradeUtil.getTotalEmberConsumption(bottom, 2.0, upgrades);
            if (top.capability.getEmber() >= emberCost) {
                ArrayList<FluidStack> fluids = bottom.getFluids();
                RecipeAlchemicalMixer recipe = CraftingRegistry.getAlchemicalMixingRecipe(fluids);
                if (recipe != null && !world.isRemote) {
                    AlchemyResult result = recipe.matchAshes(globe.getAspects(), world);
                    if(result.getAccuracy() == 1.0) {
                        IFluidHandler tank = top.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                        FluidStack output = UpgradeUtil.transformOutput(bottom, recipe.output, upgrades);
                        int amount = tank.fill(output, false);
                        if (amount != 0) {
                            tank.fill(output, true);
                            bottom.consumeFluids(recipe);
                            top.capability.removeAmount(emberCost, true);
                            bottom.markDirty();
                            top.markDirty();
                        }
                    }
                    else if(result.areAllPresent()) //That's not fair if we just void fluid without any ash
                    {
                        ItemStack failure = result.createFailure();
                        BlockPos topPos = top.getPos();
                        ejectFailure(world, topPos,failure,EnumFacing.HORIZONTALS);
                        bottom.consumeFluids(recipe);
                        top.capability.removeAmount(emberCost*200, true);
                        fail(world.rand.nextInt(100)+200);
                        globe.consumeAsh();
                    }
                }
            }
        }
    }

    private void fail(int time) {
        failure = time;
    }

    @Nullable
    public void ejectFailure(World world, BlockPos pos, ItemStack failure, EnumFacing[] directions)
    {
        int ioff = 0;

        for(int i = 0; i < directions.length; i++)
        {
            EnumFacing direction = directions[(i + ioff) % directions.length];
            BlockPos ejectPos = pos.offset(direction);
            IBlockState state = world.getBlockState(ejectPos);
            if(state.getBlockFaceShape(world,ejectPos,direction.getOpposite()) == BlockFaceShape.UNDEFINED)
            {
                ejectFailure(world,pos,failure,direction);
                return;
            }
        }

        ejectFailure(world,pos,failure,EnumFacing.UP);
    }

    //TODO: this should probably be a utility method
    public void ejectFailure(World world, BlockPos pos, ItemStack failure, EnumFacing direction)
    {
        float xEject = direction.getFrontOffsetX();
        float zEject = direction.getFrontOffsetZ();

        float xOff = world.rand.nextFloat() * 0.05F + 0.475F + xEject * 0.7F;
        float yOff = 0.5F;
        float zOff = world.rand.nextFloat() * 0.05F + 0.475F + zEject * 0.7F;

        world.playSound(null,pos.getX() + xOff, pos.getY() + yOff, pos.getZ() + zOff, SoundEvents.ALCHEMICAL_MIXER_WASTE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        PacketHandler.INSTANCE.sendToAll(new MessageMixerFailFX(pos.getX() + xOff, pos.getY() + yOff, pos.getZ() + zOff, direction));
        EntityItem item = new EntityItem(world, pos.getX() + xOff, pos.getY() + yOff - 0.4f, pos.getZ() + zOff, failure);
        item.motionX = xEject * 0.1f;
        item.motionY = 0.0D;
        item.motionZ = zEject * 0.1f;
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
    }
}

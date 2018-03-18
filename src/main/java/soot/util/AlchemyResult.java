package soot.util;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import soot.util.AspectList.AspectRangeList;
import teamroots.embers.item.ItemAlchemicWaste;

import java.util.HashMap;

public class AlchemyResult {
    double accuracy;
    HashMap<String,Integer> deltas = new HashMap<>();
    int totalAsh;
    boolean allPresent = true;

    public double getAccuracy()
    {
        return accuracy;
    }

    public double getTotal() { return totalAsh; }

    public boolean areAllPresent() { return allPresent; }

    public static AlchemyResult create(AspectList list,AspectRangeList range,World world)
    {
        AlchemyResult result = new AlchemyResult();
        double totalDelta = 0;
        double totalCompare = 0;
        for (String aspect: range.minAspects.getAspects()) {
            int amt = list.getAspect(aspect);
            if(amt < range.getMin(aspect))
                result.allPresent = false;
            int compareAmt = range.getExact(aspect,world);
            double delta = Math.abs(amt - compareAmt);
            result.totalAsh += amt;
            totalDelta += delta;
            totalCompare += compareAmt;
            result.deltas.put(aspect,(int)delta);
        }
        result.accuracy = Math.max(0.0,1.0 - totalDelta / totalCompare);
        result.accuracy = Math.round(result.accuracy * 100.0) / 100.0;
        return result;
    }

    public ItemStack createFailure()
    {
        int ironDelta = deltas.getOrDefault("iron",0);
        int dawnstoneDelta = deltas.getOrDefault("dawnstone",0);
        int copperDelta = deltas.getOrDefault("copper",0);
        int silverDelta = deltas.getOrDefault("silver",0);
        int leadDelta = deltas.getOrDefault("lead",0);
        return ItemAlchemicWaste.create(ironDelta,copperDelta,silverDelta,dawnstoneDelta,leadDelta,totalAsh);
    }
}

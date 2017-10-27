package soot;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import soot.block.IBlockVariants;
import soot.util.IBlockColored;
import soot.util.IItemColored;
import teamroots.embers.block.IBlock;

import java.util.ArrayList;

public class ClientProxy implements IProxy {
    ArrayList<IBlockColored> COLOR_BLOCKS = new ArrayList<>();
    ArrayList<IItemColored> COLOR_ITEMS = new ArrayList<>();

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerItemModel(Block block)
    {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event)
    {
        Registry.registerBlockModels();
        Registry.registerItemModels();
    }

    @Override
    public void init() {
        BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
        ItemColors itemColors = Minecraft.getMinecraft().getItemColors();

        for (IBlockColored block : COLOR_BLOCKS) {
            blockColors.registerBlockColorHandler(block::getColorMultiplier,(Block)block);
        }

        for (IItemColored item : COLOR_ITEMS) {
            itemColors.registerItemColorHandler(item::getColorFromItemstack,(Item)item);
        }

        Registry.registerTESRs();
    }

    @Override
    public void postInit() {

    }

    @Override
    public void registerBlockModel(Block block) {
        if(block instanceof IBlockColored)
            COLOR_BLOCKS.add((IBlockColored) block);
    }

    @Override
    public void registerItemModel(Item item) {
        if(item instanceof IItemColored)
            COLOR_ITEMS.add((IItemColored) item);
        if(item instanceof ItemBlock)
        {
            ItemBlock itemBlock = (ItemBlock) item;
            Block block = itemBlock.getBlock();
            ResourceLocation resloc = block.getRegistryName();
            if(block instanceof IBlockVariants) {
                for (IBlockState state : ((IBlockVariants) block).getValidStates()) {
                    ModelLoader.setCustomModelResourceLocation(item, block.getMetaFromState(state), new ModelResourceLocation(resloc, ((IBlockVariants) block).getBlockStateName(state)));
                }
            }
            else
                ModelLoader.setCustomModelResourceLocation(item,0,new ModelResourceLocation(resloc, "inventory"));
        }
    }
}

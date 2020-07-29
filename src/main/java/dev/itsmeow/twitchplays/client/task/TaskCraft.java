package dev.itsmeow.twitchplays.client.task;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class TaskCraft extends Task {

    private String itemName;
    private int meta = 0;

    public TaskCraft(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 2 || args.length == 3) {
            itemName = args[1];
            if(itemName.indexOf(":") == -1) {
                itemName = "minecraft:" + itemName;
            }
            if(args.length == 3) {
                try {
                    meta = Integer.parseInt(args[2]);
                } catch(NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int maxActiveTime() {
        return 0;
    }

    @Override
    protected void update() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
        if(item == null) {
            return;
        }
        ItemStack query = new ItemStack(item, 1, meta);
        if(query != null && query.getItem() != null) {

            int gridSize = 2;

            int x = (int) Math.floor(player.posX);
            int y = (int) Math.floor(player.getEntityBoundingBox().minY);
            int z = (int) Math.floor(player.posZ);
            
            int tableX = 0, tableY = 0, tableZ = 0;

            for(int i = x - 4; i <= x + 4; i++) {
                for(int j = y - 4; j <= y + 4; j++) {
                    for(int k = z - 4; k <= z + 4; k++) {
                        Block b = world.getBlockState(new BlockPos(i, j, k)).getBlock();
                        if(b.equals(Blocks.CRAFTING_TABLE)) {
                            RayTraceResult ray = world.rayTraceBlocks(new Vec3d(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ), new Vec3d(i + 0.5D, j + 0.5D, k + 0.5D));
                            if(ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK && ray.getBlockPos().getX() == i && ray.getBlockPos().getY() == j && ray.getBlockPos().getZ() == k) {
                                gridSize = 3;
                                tableX = i;
                                tableY = j;
                                tableZ = k;
                                break;
                            }
                        }
                    }
                }
            }

            ItemStack result = null;
            NonNullList<Ingredient> required = null;

            for(IRecipe r : CraftingManager.REGISTRY) {
                if(r instanceof ShapedRecipes) {
                    ShapedRecipes recipe = (ShapedRecipes) r;
                    ItemStack res = recipe.getRecipeOutput();
                    if(res != null && query.getItem() == res.getItem() && query.getMetadata() == res.getMetadata() && ItemStack.areItemStackTagsEqual(query, res) && recipe.canFit(gridSize, gridSize)) {
                        result = res;
                        required = recipe.getIngredients();
                        break;
                    }
                } else if(r instanceof ShapedOreRecipe) {
                    ShapedOreRecipe recipe = (ShapedOreRecipe) r;
                    ItemStack res = recipe.getRecipeOutput();
                    if(res != null && query.getItem() == res.getItem() && query.getItemDamage() == res.getItemDamage() && ItemStack.areItemStackTagsEqual(query, res) && recipe.canFit(gridSize, gridSize)) {
                        result = res;
                        required = recipe.getIngredients();
                        break;
                    }
                } else if(r instanceof ShapelessRecipes) {
                    ShapelessRecipes recipe = (ShapelessRecipes) r;
                    ItemStack res = recipe.getRecipeOutput();
                    if(res != null && query.getItem() == res.getItem() && query.getItemDamage() == res.getItemDamage() && ItemStack.areItemStackTagsEqual(query, res) && recipe.canFit(gridSize, gridSize)) {
                        result = res;
                        required = recipe.getIngredients();
                        break;
                    }
                } else if(r instanceof ShapelessOreRecipe) {
                    ShapelessOreRecipe recipe = (ShapelessOreRecipe) r;
                    ItemStack res = recipe.getRecipeOutput();
                    if(res != null && query.getItem() == res.getItem() && query.getItemDamage() == res.getItemDamage() && ItemStack.areItemStackTagsEqual(query, res) && recipe.canFit(gridSize, gridSize)) {
                        result = res;
                        required = recipe.getIngredients();
                        break;
                    }
                }
            }
            if(result != null && required != null) {
                if(gridSize == 3) {
                    player.displayGui(new BlockWorkbench.InterfaceCraftingTable(player.world, new BlockPos(tableX, tableY, tableZ)));
                } else {
                    
                }
            }
        }
    }

    @Override
    public String getName() {
        return "craft";
    }
}

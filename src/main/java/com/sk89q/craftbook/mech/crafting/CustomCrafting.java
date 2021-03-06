package com.sk89q.craftbook.mech.crafting;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.util.ItemUtil;
import com.sk89q.craftbook.util.VerifyUtil;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;

/**
 * Custom Crafting Recipe Handler
 *
 * @author Me4502
 */
public class CustomCrafting implements Listener {

    protected final RecipeManager recipes;
    protected final CraftBookPlugin plugin = CraftBookPlugin.inst();

    public static HashMap<Recipe, RecipeManager.Recipe> advancedRecipes = new HashMap<Recipe, RecipeManager.Recipe>();

    public CustomCrafting() {

        plugin.createDefaultConfiguration(new File(plugin.getDataFolder(), "crafting-recipes.yml"), "crafting-recipes.yml", false);
        recipes = new RecipeManager(new YAMLProcessor(new File(plugin.getDataFolder(), "crafting-recipes.yml"), true, YAMLFormat.EXTENDED));
        Collection<RecipeManager.Recipe> recipeCollection = recipes.getRecipes();
        int recipes = 0;
        for (RecipeManager.Recipe r : recipeCollection) {
            if(addRecipe(r))
                recipes++;
        }
        plugin.getLogger().info("Registered " + recipes + " custom recipes!");
    }

    /**
     * Adds a recipe to the manager.
     */
    public boolean addRecipe(RecipeManager.Recipe r) {
        try {
            if (r.getType() == RecipeManager.RecipeType.SHAPELESS) {
                ShapelessRecipe sh = new ShapelessRecipe(r.getResult().getItemStack());
                for (CraftingItemStack is : r.getIngredients()) {
                    sh.addIngredient(is.getItemStack().getAmount(), is.getItemStack().getType(), is.getItemStack().getData().getData());
                }
                plugin.getServer().addRecipe(sh);
                if(r.hasAdvancedData()) {
                    advancedRecipes.put(sh, r);
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        plugin.getLogger().info("Adding a new recipe with advanced data!");
                }
            } else if (r.getType() == RecipeManager.RecipeType.SHAPED) {
                ShapedRecipe sh = new ShapedRecipe(r.getResult().getItemStack());
                sh.shape(r.getShape());
                for (Entry<CraftingItemStack, Character> is : r.getShapedIngredients().entrySet()) {
                    sh.setIngredient(is.getValue().charValue(), is.getKey().getItemStack().getType(), is.getKey().getItemStack().getData().getData());
                }
                plugin.getServer().addRecipe(sh);
                if(r.hasAdvancedData()) {
                    advancedRecipes.put(sh, r);
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        plugin.getLogger().info("Adding a new recipe with advanced data!");
                }
            } else if (r.getType() == RecipeManager.RecipeType.FURNACE) {
                FurnaceRecipe sh = new FurnaceRecipe(r.getResult().getItemStack(), r.getIngredients().toArray(new CraftingItemStack[r.getIngredients().size()])[0].getItemStack().getType());
                for (CraftingItemStack is : r.getIngredients()) {
                    sh.setInput(is.getItemStack().getType(), is.getItemStack().getData().getData());
                }
                plugin.getServer().addRecipe(sh);
                if(r.hasAdvancedData()) {
                    advancedRecipes.put(sh, r);
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        plugin.getLogger().info("Adding a new recipe with advanced data!");
                }
            } else {
                return false;
            }
            plugin.getLogger().info("Registered a new " + r.getType().toString().toLowerCase() + " recipe!");

            return true;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Corrupt or invalid recipe!");
            plugin.getLogger().severe("Please either delete custom-crafting.yml, or fix the issues with your recipes file!");
            BukkitUtil.printStacktrace(e);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load recipe! Is it incorrectly written?");
            BukkitUtil.printStacktrace(e);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCraft(CraftItemEvent event) {

        ItemStack bits = null;
        if(advancedRecipes.size() > 0 && CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
            plugin.getLogger().info("Crafting has been initiated!");
        for(Recipe rec : advancedRecipes.keySet()) {

            if(checkRecipes(rec, event.getRecipe())) {
                if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                    plugin.getLogger().info("A recipe with custom data is being crafted!");
                RecipeManager.Recipe recipe = advancedRecipes.get(rec);
                if(recipe.hasAdvancedData("permission-node")) {
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        plugin.getLogger().info("A recipe with permission nodes detected!");
                    if(!event.getWhoClicked().hasPermission((String) recipe.getAdvancedData("permission-node"))) {
                        ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You do not have permission to craft this recipe!");
                        event.setCancelled(true);
                        return;
                    }
                }
                if(recipe.hasAdvancedData("extra-results")) {
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        plugin.getLogger().info("A recipe with extra results detected!");
                    ArrayList<CraftingItemStack> stacks = new ArrayList<CraftingItemStack>((Collection<CraftingItemStack>) recipe.getAdvancedData("extra-results"));
                    for(CraftingItemStack stack : stacks) {
                        HashMap<Integer, ItemStack> leftovers = event.getWhoClicked().getInventory().addItem(stack.getItemStack());
                        if(!leftovers.isEmpty()) {
                            for(ItemStack istack : leftovers.values())
                                event.getWhoClicked().getWorld().dropItemNaturally(event.getWhoClicked().getLocation(), istack);
                        }
                    }
                }
                bits = applyAdvancedEffects(event.getRecipe().getResult(),rec);
                break;
            }
        }
        if(bits != null && !bits.equals(event.getRecipe().getResult())) {
            bits.setAmount(event.getCurrentItem().getAmount());
            event.setCurrentItem(bits);
        }
    }

    public static ItemStack craftItem(Recipe recipe) {

        for(Recipe rec : advancedRecipes.keySet()) {
            if(checkRecipes(rec, recipe)) {
                return applyAdvancedEffects(recipe.getResult(),rec);
            }
        }

        return recipe.getResult();
    }

    @SuppressWarnings("unchecked")
    private static ItemStack applyAdvancedEffects(ItemStack stack, Recipe rep) {

        RecipeManager.Recipe recipe = advancedRecipes.get(rep);

        if(recipe == null)
            return stack;

        ItemStack res = stack.clone();
        if(recipe.getResult().hasAdvancedData("name")) {
            ItemMeta meta = res.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + (String) recipe.getResult().getAdvancedData("name"));
            res.setItemMeta(meta);
        }
        if(recipe.getResult().hasAdvancedData("lore")) {
            ItemMeta meta = res.getItemMeta();
            meta.setLore((List<String>) recipe.getResult().getAdvancedData("lore"));
            res.setItemMeta(meta);
        }
        if(recipe.getResult().hasAdvancedData("enchants")) {
            for(Entry<Enchantment,Integer> enchants : ((Map<Enchantment,Integer>)recipe.getResult().getAdvancedData("enchants")).entrySet())
                res.addUnsafeEnchantment(enchants.getKey(), enchants.getValue());
        }
        return res;
    }

    private static boolean checkRecipes(Recipe rec1, Recipe rec2) {

        if(ItemUtil.areItemsIdentical(rec1.getResult(), rec2.getResult())) {
            if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                CraftBookPlugin.logger().info("Recipe passed results test!");
            if(rec1 instanceof ShapedRecipe && rec2 instanceof ShapedRecipe) {
                if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                    CraftBookPlugin.logger().info("Shaped recipe!");
                ShapedRecipe recipe1 = (ShapedRecipe) rec1;
                ShapedRecipe recipe2 = (ShapedRecipe) rec2;
                if(recipe1.getShape().length == recipe2.getShape().length) {
                    if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                        CraftBookPlugin.logger().info("Same shape!");
                    if(VerifyUtil.<ItemStack>withoutNulls(recipe1.getIngredientMap().values()).size() != VerifyUtil.<ItemStack>withoutNulls(recipe2.getIngredientMap().values()).size())
                        return false;
                    List<ItemStack> test = new ArrayList<ItemStack>();
                    test.addAll(((ShapedRecipe) rec1).getIngredientMap().values());
                    test = (List<ItemStack>) VerifyUtil.<ItemStack>withoutNulls(test);
                    if(test.size() == 0)
                        return true;
                    if(!test.removeAll(VerifyUtil.<ItemStack>withoutNulls(recipe2.getIngredientMap().values())) && test.size() > 0)
                        return false;
                    if(test.size() > 0)
                        return false;
                }
            } else if(rec1 instanceof ShapelessRecipe && rec2 instanceof ShapelessRecipe) {

                if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                    CraftBookPlugin.logger().info("Shapeless recipe!");
                ShapelessRecipe recipe1 = (ShapelessRecipe) rec1;
                ShapelessRecipe recipe2 = (ShapelessRecipe) rec2;

                if(VerifyUtil.withoutNulls(recipe1.getIngredientList()).size() != VerifyUtil.withoutNulls(recipe2.getIngredientList()).size())
                    return false;

                if(CraftBookPlugin.isDebugFlagEnabled("advanced-data"))
                    CraftBookPlugin.logger().info("Same size!");

                List<ItemStack> test = new ArrayList<ItemStack>();
                test.addAll(VerifyUtil.<ItemStack>withoutNulls(recipe1.getIngredientList()));
                if(test.size() == 0)
                    return true;
                if(!test.removeAll(VerifyUtil.<ItemStack>withoutNulls(recipe2.getIngredientList())) && test.size() > 0)
                    return false;
                if(test.size() > 0)
                    return false;
            }

            return true;
        }

        return false;
    }
}
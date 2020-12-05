/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

public final class AutoCraftHack extends Hack implements UpdateListener
{
	private final SliderSetting delay = new SliderSetting("Delay",
		"Delay between moving stacks of items.\n"
			+ "Should be at least 70ms for NoCheat+ servers.",
		200, 0, 500, 10, v -> (int)v + "ms");
	
	private final CheckboxSetting ironIngot =
		new CheckboxSetting("Iron", "Craft iron ingots to iron blocks.", true);
	private final CheckboxSetting ironNugget = new CheckboxSetting(
		"Iron Nugget", "Craft iron nuggets to iron ingots.", true);
	private final CheckboxSetting goldIngot =
		new CheckboxSetting("Gold", "Craft gold ingots to gold blocks.", true);
	private final CheckboxSetting goldNugget = new CheckboxSetting(
		"Gold Nugget", "Craft gold nuggets to gold ingots.", true);
	private final CheckboxSetting redstone = new CheckboxSetting("Redstone",
		"Craft redstone to redstone blocks.", false);
	private final CheckboxSetting diamond = new CheckboxSetting("Diamond",
		"Craft diamonds to diamond blocks.", false);
	private final CheckboxSetting emerald = new CheckboxSetting("Emerald",
		"Craft emeralds to emerald blocks.", false);
	private final CheckboxSetting slime = new CheckboxSetting("Slime",
		"Craft slime balls to slime blocks.", false);
	private final CheckboxSetting coal =
		new CheckboxSetting("Coal", "Craft coal to coal blocks.", false);
	private long lastMove = System.currentTimeMillis();
	
	public AutoCraftHack()
	{
		super("AutoCraft", "Automatically compress ingots into blocks,\n"
			+ " or nuggets into ingots.");
		setCategory(Category.ITEMS);
		addSetting(delay);
		addSetting(ironNugget);
		addSetting(ironIngot);
		addSetting(goldNugget);
		addSetting(goldIngot);
		addSetting(redstone);
		addSetting(diamond);
		addSetting(emerald);
		addSetting(coal);
		addSetting(slime);
	}
	
	@Override
	protected void onEnable()
	{
		
		EVENTS.add(UpdateListener.class, this);
		
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
	}
	
	@Override
	public void onUpdate()
	{
		if(System.currentTimeMillis() - lastMove < delay.getValueI())
		{
			return;
		}
		if(MC.currentScreen != null)
		{
			if(MC.currentScreen instanceof CraftingScreen)
			{
				lastMove = System.currentTimeMillis();
				
				List<Slot> slots = MC.player.currentScreenHandler.slots;
				Item out = slots.get(0).getStack().getItem();
				if((out.equals(Items.GOLD_BLOCK) && goldIngot.isChecked())
					|| (out.equals(Items.GOLD_INGOT) && goldNugget.isChecked())
					|| (out.equals(Items.IRON_INGOT) && ironNugget.isChecked())
					|| (out.equals(Items.IRON_BLOCK) && ironIngot.isChecked())
					|| (out.equals(Items.DIAMOND_BLOCK) && diamond.isChecked())
					|| (out.equals(Items.REDSTONE_BLOCK)
						&& redstone.isChecked())
					|| (out.equals(Items.COAL_BLOCK) && coal.isChecked())
					|| (out.equals(Items.EMERALD_BLOCK) && emerald.isChecked())
					|| (out.equals(Items.SLIME_BLOCK) && slime.isChecked()))
				{
					MC.interactionManager.clickSlot(
						MC.player.currentScreenHandler.syncId, 0, 0,
						SlotActionType.QUICK_MOVE, MC.player);
					return;
				}
				
				// CraftingScreen screen = (CraftingScreen)MC.currentScreen;
				// RecipeBookWidget book = screen.getRecipeBookWidget();
				Recipe<?> recipe = null;
				RecipeManager n = MC.world.getRecipeManager();
				List<CraftingRecipe> recipes =
					n.listAllOfType(RecipeType.CRAFTING);
				
				for(CraftingRecipe cr : recipes)
				{
					if(cr.getOutput().getItem().equals(Items.GOLD_BLOCK)
						&& goldIngot.isChecked()
						&& MC.player.inventory.count(Items.GOLD_INGOT) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.GOLD_INGOT)
						&& goldNugget.isChecked()
						&& MC.player.inventory.count(Items.GOLD_NUGGET) >= 9
						&& cr.getPreviewInputs().get(0)
							.test(new ItemStack(Items.GOLD_NUGGET)))
					{
						recipe = cr;
						break;
					}
					if(cr.getOutput().getItem().equals(Items.IRON_INGOT)
						&& ironNugget.isChecked()
						&& MC.player.inventory.count(Items.IRON_NUGGET) >= 9
						&& cr.getPreviewInputs().get(0)
							.test(new ItemStack(Items.IRON_NUGGET)))
					{
						recipe = cr;
						break;
					}
					if(cr.getOutput().getItem().equals(Items.IRON_BLOCK)
						&& ironIngot.isChecked()
						&& MC.player.inventory.count(Items.IRON_INGOT) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.REDSTONE_BLOCK)
						&& redstone.isChecked()
						&& MC.player.inventory.count(Items.REDSTONE) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.DIAMOND_BLOCK)
						&& diamond.isChecked()
						&& MC.player.inventory.count(Items.DIAMOND) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.EMERALD_BLOCK)
						&& emerald.isChecked()
						&& MC.player.inventory.count(Items.EMERALD) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.SLIME_BLOCK)
						&& slime.isChecked()
						&& MC.player.inventory.count(Items.SLIME_BALL) >= 9)
					{
						recipe = cr;
					}
					if(cr.getOutput().getItem().equals(Items.COAL_BLOCK)
						&& coal.isChecked()
						&& MC.player.inventory.count(Items.COAL) >= 9)
					{
						recipe = cr;
					}
				}
				if(recipe != null)
				{
					MC.interactionManager.clickRecipe(
						MC.player.currentScreenHandler.syncId, recipe, true);
				}
				
			}
		}
	}
	
}

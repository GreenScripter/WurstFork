/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IContainer;

@SuppressWarnings("rawtypes")
@Mixin({EnchantmentScreen.class, AnvilScreen.class})
public abstract class HandledScreenMixin extends HandledScreen
	implements IContainer
{
	
	@SuppressWarnings("unchecked")
	public HandledScreenMixin(ScreenHandler handler, PlayerInventory inventory,
		Text title)
	{
		super(handler, inventory, title);
	}
	
	public void click(int slotId, SlotActionType type)
	{
		Slot slot = handler.slots.get(slotId);
		if(slot.getStack().isEmpty())
			return;
		
		if(WurstClient.MC.currentScreen == null)
			return;
		
		onMouseClick(slot, slot.id, 0, type);
	}
	
	public int getSyncId()
	{
		return this.handler.syncId;
	}
	
	public ScreenHandler getHandler()
	{
		return this.handler;
	}
	
	public int findSlot(String s, Predicate<ItemStack> check)
	{
		for(int i = 0; i < handler.slots.size(); i++)
		{
			Slot slot = handler.slots.get(i);
			
			if(slot.hasStack()
				&& Registry.ITEM.getId(slot.getStack().getItem()).toString()
					.equals(s)
				
				&& check.test(slot.getStack()))
			{
				return i;
			}
		}
		return -1;
	}
	
}

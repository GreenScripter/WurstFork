/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.util.ChatUtils;

public final class ElytraSwapCmd extends Command implements UpdateListener
{
	
	public ElytraSwapCmd()
	{
		super("elytraswap",
			"Swaps out your chestplate for elytra and vice versa.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		PlayerInventory inv = MC.player.getInventory();
		int target = -1;// 38
		int chestplateSlot = 38;
		ItemStack chest = inv.getStack(chestplateSlot);
		System.out.println(chest);
		if(!chest.getItem().equals(Items.AIR))
		{
			boolean elytra = chest.getItem().equals(Items.ELYTRA);
			for(int i = 9; i < inv.size() && i < 38; i++)
			{
				ItemStack is = inv.getStack(i);
				// System.out.println(is);
				if((is.getItem() instanceof ArmorItem
					&& ((ArmorItem)is.getItem()).getSlotType()
						.equals(EquipmentSlot.CHEST))
					|| is.getItem().equals(Items.ELYTRA))
				{
					boolean elytra2 = is.getItem().equals(Items.ELYTRA);
					System.out.println(elytra);
					System.out.println(elytra2);
					System.out.println(chest);
					System.out.println(is);
					if(elytra != elytra2)
					{
						target = i;
						break;
					}
				}
				
			}
			if(target == -1)
				for(int i = 0; i < 9; i++)
				{
					ItemStack is = inv.getStack(i);
					// System.out.println(is);
					if((is.getItem() instanceof ArmorItem
						&& ((ArmorItem)is.getItem()).getSlotType()
							.equals(EquipmentSlot.CHEST))
						|| is.getItem().equals(Items.ELYTRA))
					{
						boolean elytra2 = is.getItem().equals(Items.ELYTRA);
						System.out.println(elytra);
						System.out.println(elytra2);
						System.out.println(chest);
						System.out.println(is);
						if(elytra != elytra2)
						{
							target = i;
							break;
						}
					}
					
				}
			if(target != -1)
			{
				if(target < 9)
					target += 36;
				IMC.getInteractionManager().windowClick_PICKUP(target);
				IMC.getInteractionManager().windowClick_PICKUP(6);
				IMC.getInteractionManager().windowClick_PICKUP(target);
			}else
			{
				ChatUtils.error("Nothing to swap with.");
			}
			
			// IMC.getInteractionManager().windowClick_PICKUP(target);
			// IMC.getInteractionManager().windowClick_PICKUP(chestplateSlot);
			// IMC.getInteractionManager().windowClick_PICKUP(target);
			
		}else
		{
			ChatUtils.error("Not wearing a chestplate.");
		}
		
		// IMC.getInteractionManager().windowClick_THROW(slowModeSlotCounter);
		
		EVENTS.remove(UpdateListener.class, this);
	}
	
}

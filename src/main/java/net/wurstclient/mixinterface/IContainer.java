/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public interface IContainer
{
	public void click(int slot, SlotActionType type);
	
	public int findSlot(String s, Predicate<ItemStack> check);
	
	public default int findSlot(String s)
	{
		return findSlot(s, i -> true);
	}
	
	public int getSyncId();
	
	public ScreenHandler getHandler();
}

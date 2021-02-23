/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;

public final class PeekCmd extends Command implements RenderListener
{
	
	public PeekCmd()
	{
		super("peek",
			"Allows you to see the contents of a shulker box.",
			".peek");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		ItemStack is = MC.player.getMainHandStack();
		System.out.println(is);
		System.out.println(is.getTag());
		if(is.hasTag() && is.getTag().getKeys().contains("BlockEntityTag"))
		{
			ListTag tag =
				is.getTag().getCompound("BlockEntityTag").getList("Items", 10);
			SimpleInventory shulker = new SimpleInventory(27);
			
			for(int i = 0; i < tag.size(); i++)
			{
				System.out.println(tag.getCompound(i));
				shulker.setStack(tag.getCompound(i).getByte("Slot"), ItemStack.fromTag(tag.getCompound(i)));
			}
			GenericContainerScreen screen = new GenericContainerScreen(
				new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3,
					0, MC.player.inventory, shulker, 3),
				MC.player.inventory, is.getName());
			
			MC.openScreen(screen);
			
		}
		// ChatUtils.message("Showing inventory of " + "" + ".");
		EVENTS.remove(RenderListener.class, this);
	}
}

/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"no hunger", "nohunger"})
public final class AntiHungerHack extends Hack implements PacketOutputListener
{
	public AntiHungerHack()
	{
		super("AntiHunger", "Prevents you from getting hungry when sprinting.");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event
			.getPacket() instanceof net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket)
		{
			event.cancel();
		}
	}
	
}

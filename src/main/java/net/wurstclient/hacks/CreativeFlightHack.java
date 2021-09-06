/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"creative flight", "CreativeFly", "creative fly"})
public final class CreativeFlightHack extends Hack implements UpdateListener
{
	public final CheckboxSetting anti_flykick = new CheckboxSetting(
		"Anti Flykick", "Bypass the Fly check on Vanilla Servers.", false);
	public final SliderSetting decentRate = new SliderSetting("Decent Delay",
		"Controls the rate of decent while flying.\nHelps prevent laggy servers from kicking you.",
		40, 1, 40, 1, ValueDisplay.INTEGER);
	private int fly_ticks = 0;
	
	public CreativeFlightHack()
	{
		super("CreativeFlight",
			"Allows you to you fly like in Creative Mode.\n\n"
				+ "\u00a7c\u00a7lWARNING:\u00a7r"
				+ " You will take fall damage if you don't use NoFall.");
		
		setCategory(Category.MOVEMENT);
		addSetting(anti_flykick);
		addSetting(decentRate);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().jetpackHack.setEnabled(false);
		WURST.getHax().flightHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		ClientPlayerEntity player = MC.player;
		PlayerAbilities abilities = player.getAbilities();
		
		boolean creative = player.isCreative();
		abilities.flying = creative && !player.isOnGround();
		abilities.allowFlying = creative;
	}
	
	@Override
	public void onUpdate()
	{
		MC.player.getAbilities().allowFlying = true;
		if(anti_flykick.isChecked())
		{
			fly_ticks--;
			if(fly_ticks <= 0)
			{
				fly_ticks = decentRate.getValueI();
				if(MC.player.getVelocity().y >= 0)
					MC.player.setVelocity(MC.player.getVelocity().subtract(0,
						MC.player.getVelocity().y + 0.05, 0));
			}
		}
	}
}

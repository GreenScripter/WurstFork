/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"FlyHack", "fly hack", "flying"})
public final class FlightHack extends Hack
	implements UpdateListener, IsPlayerInWaterListener
{
	
	public final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	private final CheckboxSetting downLimit = new CheckboxSetting(
		"Limit Downward Speed",
		"Prevents the player from taking damage when flying down quickly with NoFall on.",
		false);
	private final CheckboxSetting arrowFix = new CheckboxSetting(
		"Reposition for Bows",
		"Modifies the flight pattern slightly so that arrows will shoot correctly.",
		false);
	
	private int lastGoingUp = 10;
	
	public final CheckboxSetting anti_flykick = new CheckboxSetting(
		"Anti Flykick", "Bypass the Fly check on Vanilla Servers.", false);
	
	private int fly_ticks = 0;
	
	public FlightHack()
	{
		super("Flight",
			"Allows you to you fly.\n\n" + "\u00a7c\u00a7lWARNING:\u00a7r"
				+ " You will take fall damage if you don't use NoFall.");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
		addSetting(downLimit);
		addSetting(arrowFix);
		addSetting(anti_flykick);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().jetpackHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		MC.options.keySneak
			.setPressed(((IKeyBinding)MC.options.keySneak).isActallyPressed());
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		player.abilities.flying = false;
		player.flyingSpeed = speed.getValueF();
		
		player.setVelocity(0, 0, 0);
		if(WURST.getHax().freecamHack.isEnabled())
		{
			// don't move when using freecam
			return;
		}
		Vec3d velocity = player.getVelocity();
		if(MC.currentScreen == null)
		{
			// Allow shifing in air
			if(MC.options.keyJump.isPressed()
				&& ((IKeyBinding)MC.options.keySneak).isActallyPressed())
			{
				MC.options.keySneak.setPressed(true);
				return;
			}
			if(MC.options.keyJump.isPressed())
			{
				lastGoingUp = 0;
				player.setVelocity(velocity.add(0, speed.getValue(), 0));
				player.flyingSpeed = (float)Math.min(speed.getValueF(),
					10 - player.getVelocity().y);
			}else if(((IKeyBinding)MC.options.keySneak).isActallyPressed())
			{
				MC.options.keySneak.setPressed(false);
				player
					.setVelocity(velocity.subtract(
						0, (downLimit.isChecked()
							? Math.min(3, speed.getValue()) : speed.getValue()),
						0));
				player.flyingSpeed = (float)Math.min(speed.getValueF(),
					10 + player.getVelocity().y);
			}else
			{
				if(arrowFix.isChecked())
				{
					if(lastGoingUp < 3)
					{
						lastGoingUp++;
						player.setVelocity(velocity.add(0, -1, 0));
					}else
					{
						player.setVelocity(velocity.add(0, 0.01, 0));
						
					}
				}
			}
		}
		// If enabled, fly down 0.02 blocks every 2 seconds to prevent kicks
		if(anti_flykick.isChecked())
		{
			fly_ticks--;
			if(fly_ticks <= 0)
			{
				fly_ticks = 40;
				if(velocity.y >= 0)
					player.setVelocity(
						velocity.subtract(0, velocity.y + 0.05, 0));
			}
		}
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
}

/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RotationUtils;

@SearchTags({"BoatFlight", "boat fly", "boat flight"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	public final SliderSetting speed =
		new SliderSetting("Speed", 1, 1, 20, 0.05, ValueDisplay.DECIMAL);
	
	public BoatFlyHack()
	{
		super("BoatFly", "Allows you to fly with boats");
		setCategory(Category.MOVEMENT);
		addSetting(speed);

	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check if riding
		if(!MC.player.hasVehicle())
			return;
		
		// fly
		Entity vehicle = MC.player.getVehicle();
		Vec3d velocity = vehicle.getVelocity();
		double motionY = MC.options.keyJump.isPressed() ? speed.getValueF() : 0;
		if (speed.getValueF() != 1 && velocity.length() >= 0.1 && MC.options.keyForward.isPressed()) {
			velocity = RotationUtils.getMoveVec(vehicle.yaw);
			velocity = new Vec3d(velocity.x, 0, velocity.z).normalize().multiply(speed.getValue());
		}
		vehicle.setVelocity(new Vec3d(velocity.x, motionY, velocity.z));
		
	}
}

/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RotationUtils;

public final class BoatControlHack extends Hack implements UpdateListener
{
	public BoatControlHack()
	{
		super("BoatControl", "Control boats by just looking in the correct direction.");
		setCategory(Category.MOVEMENT);

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
		
		Entity vehicle = MC.player.getVehicle();
		vehicle.setYaw(MC.player.getYaw());
		double speed = vehicle.getVelocity().multiply(1, 0, 1).length();
		Vec3d result = RotationUtils.getClientLookVec().multiply(1, 0, 1).normalize().multiply(speed);
		vehicle.setVelocity(result.x, vehicle.getVelocity().y, result.z);
		MC.options.leftKey.setPressed(false);
		MC.options.rightKey.setPressed(false);
		
	}
}

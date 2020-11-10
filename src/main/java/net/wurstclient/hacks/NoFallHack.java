/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"no fall"})
public final class NoFallHack extends Hack implements UpdateListener
{
	private final CheckboxSetting speedLimit = new CheckboxSetting("Limit Falling Speed", "Prevents the player from taking some damage during long falls.", true);

	
	public NoFallHack()
	{
		super("NoFall", "Protects you from fall damage.");
		setCategory(Category.MOVEMENT);
		addSetting(speedLimit);
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
		ClientPlayerEntity player = MC.player;
		if(player.fallDistance <= (player.isFallFlying() ? 1 : 2))
			return;
		
		if(player.isFallFlying() && player.isSneaking()
			&& !isFallingFastEnoughToCauseDamage(player))
			return;
		if (speedLimit.isChecked() && player.getVelocity().y < -3) {
			player.setVelocity(new Vec3d(player.getVelocity().x, -3, player.getVelocity().z));
		}
		player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
	}
	
	private boolean isFallingFastEnoughToCauseDamage(ClientPlayerEntity player)
	{
		return player.getVelocity().y < -0.5;
	}
}

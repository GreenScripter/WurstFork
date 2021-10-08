/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.UUID;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"speed hack"})
public final class SpeedHack extends Hack implements UpdateListener
{
	public final SliderSetting speed =
		new SliderSetting("Speed", 1.5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	public SpeedHack()
	{
		super("Speed", "Simple hack that makes you faster.");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		ClientPlayerEntity player = MC.player;
		EntityAttributeInstance att = player.getAttributes()
			.getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		
		att.addPersistentModifier(modifier);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		ClientPlayerEntity player = MC.player;
		EntityAttributeInstance att = player.getAttributes()
			.getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		att.removeModifier(modifier);
	}
	
	private EntityAttributeModifier modifier =
		new EntityAttributeModifier(UUID.randomUUID(), "Fastness",
			speed.getValue(), Operation.MULTIPLY_TOTAL);
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		EntityAttributeInstance att = player.getAttributes()
			.getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if(modifier.getValue() != speed.getValue())
		{
			att.removeModifier(modifier);
			modifier = new EntityAttributeModifier(UUID.randomUUID(),
				"Fastness", speed.getValue(), Operation.MULTIPLY_TOTAL);
			att.addPersistentModifier(modifier);
			
		}
		if(!att.hasModifier(modifier))
		{
			att.addPersistentModifier(modifier);
			
		}
	}
}

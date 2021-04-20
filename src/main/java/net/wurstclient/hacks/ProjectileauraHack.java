/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.util.Hand;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"fireball aura", "bullet aura", "projectile aura"})
public final class ProjectileauraHack extends Hack
	implements UpdateListener, PostMotionListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far Projectile Aura will reach\n"
			+ "to hit fireballs or shulker bullets.\n",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting targetFireballs = new CheckboxSetting(
		"Target fireballs", "Automatically hit away fireballs.", true);
	
	private final CheckboxSetting targetBullets = new CheckboxSetting(
		"Target bullets", "Automatically smash shulker bullets.", true);
	
	private Entity target;
	private Map<UUID, Long> hit = new HashMap<>();
	
	public ProjectileauraHack()
	{
		super("Projectileaura",
			"Automatically punch fireballs or shulker bullets around you.");
		setCategory(Category.COMBAT);
		addSetting(range);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		
		target = null;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		double rangeSq = Math.pow(range.getValue(), 2);
		Stream<Entity> stream =
			StreamSupport.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> !e.removed)
				.filter(e -> ((e instanceof FireballEntity
					&& targetFireballs.isChecked())
					|| (e instanceof ShulkerBulletEntity
						&& targetBullets.isChecked())))
				.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
				.filter(e -> !hit.keySet().contains(e.getUuid()));
		
		target = stream.findFirst().orElse(null);
		if(target == null)
			return;
		hit.put(target.getUuid(), System.currentTimeMillis());
		if(!hit.isEmpty())
		{
			Set<UUID> remove = new HashSet<>();
			for(UUID u : hit.keySet())
			{
				if(System.currentTimeMillis() - hit.get(u) > 100)
				{
					remove.add(u);
				}
			}
			remove.forEach(hit::remove);
		}
		
		WURST.getRotationFaker()
			.faceVectorPacket(target.getBoundingBox().getCenter());
	}
	
	@Override
	public void onPostMotion()
	{
		if(target == null)
			return;
		
		ClientPlayerEntity player = MC.player;
		MC.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
		
		target = null;
	}
	
}

/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;

@SearchTags({"auto walk"})
public final class AutoDodgeHack extends Hack implements UpdateListener
{
	public AutoDodgeHack()
	{
		super("AutoDodge", "Makes you dodge automatically.");
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
		
		KeyBinding forwardKey = MC.options.keyForward;
		forwardKey.setPressed(((IKeyBinding)forwardKey).isActallyPressed());
	}
	
	List<String> dodged = new ArrayList<>();
	
	@Override
	public void onUpdate()
	{
		// MC.options.keyForward.setPressed(true);
		Stream<Entity> stream =
			StreamSupport.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof ArrowEntity);
		
		List<Entity> arrows =
			stream.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		for(Entity e : arrows)
		{
			if(dodged.contains(e.getUuidAsString()))
			{
				continue;
			}
			Vec3d v = e.getVelocity();
			ArrowEntity a = new ArrowEntity(null, e.getX(), e.getY(), e.getZ());
			a.setVelocity(v);
			for(int i = 0; i < 60; i++)
			{
				if(checkHit(a))
				{
//					dodged.add(e.getUuidAsString());
					// Vec3d pos = MC.player.getPos();
					
					Vec3d mov =
						v.multiply(1, 0, 1).normalize()
							.crossProduct(new Vec3d(0,
								ThreadLocalRandom.current().nextBoolean() ? -1
									: 1,
								0))
							.multiply(1, 0, 1).normalize().multiply(2);
					
					// MC.player.setPos(pos.x + mov.x, pos.y + mov.y,
					// pos.z + mov.z);
					
					// pos = MC.player.getVelocity();
					MC.player.updatePosition(a.getX() + mov.x, a.getY(),
						a.getZ() + mov.z);
					// MC.player
					// .setVelocity(pos.add(mov.normalize().multiply(0.5)));
					continue;
				}else
				{
					v = new Vec3d(v.x, v.y - 0.05000000074505806D, v.z);
					
					a.setPos(a.getX() + v.getX(), a.getY() + v.getY(),
						a.getZ() + v.getZ());
					
				}
			}
		}
		
	}
	
	private boolean checkHit(ArrowEntity e)
	{
		Vec3d vec3d = e.getVelocity();
		Vec3d vec3d3 = e.getPos();
		Vec3d vec3d4 = vec3d3.add(vec3d);
		HitResult hitResult = MC.world.raycast(new RaycastContext(vec3d3,
			vec3d4, RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE, e));
		if(((HitResult)hitResult).getType() != HitResult.Type.MISS)
		{
			vec3d4 = ((HitResult)hitResult).getPos();
		}
		
		EntityHitResult entityHitResult = getEntityCollision(e, vec3d3, vec3d4);
		if(entityHitResult != null)
		{
			hitResult = entityHitResult;
		}
		
		if(hitResult != null
			&& ((HitResult)hitResult).getType() == HitResult.Type.ENTITY)
		{
			Entity entity = ((EntityHitResult)hitResult).getEntity();
			Entity entity2 = e.getOwner();
			if(entity instanceof PlayerEntity && entity2 instanceof PlayerEntity
				&& !((PlayerEntity)entity2)
					.shouldDamagePlayer((PlayerEntity)entity))
			{
				hitResult = null;
				entityHitResult = null;
			}
			if(entity instanceof PlayerEntity)
			{
				if(entity.getUuidAsString().equals(MC.player.getUuidAsString()))
					return true;
			}
		}
		
		if(hitResult != null)
		{
			
		}
		
		if(entityHitResult == null || e.getPierceLevel() <= 0)
		{
			return false;
		}
		
		hitResult = null;
		
		return false;
	}
	
	private EntityHitResult getEntityCollision(Entity e, Vec3d currentPosition,
		Vec3d nextPosition)
	{
		return ProjectileUtil.getEntityCollision(MC.world, e, currentPosition,
			nextPosition,
			e.getBoundingBox().stretch(e.getVelocity()).expand(1.0D),
			(Predicate<Entity>)en -> true);
	}
}

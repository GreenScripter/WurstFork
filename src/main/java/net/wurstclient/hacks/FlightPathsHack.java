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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IPersistentProjectileEntity;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.RenderUtils;

public final class FlightPathsHack extends Hack
	implements UpdateListener, RenderListener
{
	
	private final CheckboxSetting showEndpoints = new CheckboxSetting(
		"Draw box at target",
		"If a box should be drawn at the predicted landing position.", false);
	
	private final CheckboxSetting depthTest = new CheckboxSetting("Depth test",
		"If paths should be on top of blocks.", false);
	
	public FlightPathsHack()
	{
		super("FlightPaths",
			"Shows the paths of arrows and other projectiles in flight.");
		setCategory(Category.RENDER);
		addSetting(showEndpoints);
		addSetting(depthTest);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		projectiles.clear();
		
	}
	
	List<ProjectileEntity> projectiles = new ArrayList<>();
	
	@Override
	public void onUpdate()
	{
		projectiles.clear();
		@SuppressWarnings("unchecked")
		Stream<ProjectileEntity> stream =
			(Stream<ProjectileEntity>)(Object)(StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof ProjectileEntity)
				.filter(e -> !(e instanceof ShulkerBulletEntity))
				.filter(e -> !(e instanceof PersistentProjectileEntity)
					|| !((IPersistentProjectileEntity)e).inGround()));
		projectiles.addAll(stream.collect(Collectors.toList()));
		
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		if(!depthTest.isChecked())
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		RenderUtils.applyCameraRotationOnly();
		for(ProjectileEntity player : projectiles)
		{
			ArrayList<Vec3d> path = getPath(partialTicks, player);
			Vec3d camPos = RenderUtils.getCameraPos();
			
			drawLine(path, camPos);
			
			if(!path.isEmpty() && showEndpoints.isChecked())
			{
				Vec3d end = path.get(path.size() - 1);
				drawEndOfLine(end, camPos);
			}
		}
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		if(!depthTest.isChecked())
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glPopMatrix();
	}
	
	private void drawLine(ArrayList<Vec3d> path, Vec3d camPos)
	{
		GL11.glBegin(GL11.GL_LINE_STRIP);
		if(pathHitsUs)
		{
			GL11.glColor4f(1, 0, 0, 0.75F);
		}else
		{
			GL11.glColor4f(1, 1, 0, 0.75F);
		}
		
		for(Vec3d point : path)
			GL11.glVertex3d(point.x - camPos.x, point.y - camPos.y,
				point.z - camPos.z);
		
		GL11.glEnd();
	}
	
	private void drawEndOfLine(Vec3d end, Vec3d camPos)
	{
		double renderX = end.x - camPos.x;
		double renderY = end.y - camPos.y;
		double renderZ = end.z - camPos.z;
		
		GL11.glPushMatrix();
		GL11.glTranslated(renderX - 0.5, renderY - 0.5, renderZ - 0.5);
		
		GL11.glColor4f(1, 0, 0, 0.25F);
		RenderUtils.drawSolidBox();
		
		GL11.glColor4f(1, 0, 0, 0.75F);
		RenderUtils.drawOutlinedBox();
		
		GL11.glPopMatrix();
	}
	
	private boolean pathHitsUs = false;
	
	private ArrayList<Vec3d> getPath(float partialTicks,
		ProjectileEntity projectile)
	{
		pathHitsUs = false;
		ArrayList<Vec3d> path = new ArrayList<>();
		
		// calculate starting position
		double arrowPosX = projectile.lastRenderX
			+ (projectile.getX() - projectile.lastRenderX) * partialTicks
			- Math.cos(Math.toRadians(projectile.yaw)) * 0.16;
		
		double arrowPosY = projectile.lastRenderY
			+ (projectile.getY() - projectile.lastRenderY) * partialTicks
			+ projectile.getStandingEyeHeight() - 0.1;
		
		double arrowPosZ = projectile.lastRenderZ
			+ (projectile.getZ() - projectile.lastRenderZ) * partialTicks
			- Math.sin(Math.toRadians(projectile.yaw)) * 0.16;
		
		// calculate starting motion
		double arrowMotionX = projectile.getVelocity().x;
		double arrowMotionY = projectile.getVelocity().y;
		double arrowMotionZ = projectile.getVelocity().z;
		
		double gravity = getProjectileGravity(projectile);
		Vec3d lastPos = new Vec3d(projectile.getX(), projectile.getEyeY(),
			projectile.getZ());
		
		for(int i = 0; i < 1000; i++)
		{
			// add to path
			Vec3d arrowPos = new Vec3d(arrowPosX, arrowPosY, arrowPosZ);
			path.add(arrowPos);
			
			// apply motion
			arrowPosX += arrowMotionX * 0.1;
			arrowPosY += arrowMotionY * 0.1;
			arrowPosZ += arrowMotionZ * 0.1;
			
			// apply air friction
			arrowMotionX *= 0.999;
			arrowMotionY *= 0.999;
			arrowMotionZ *= 0.999;
			
			// apply gravity
			arrowMotionY -= gravity * 0.1;
			
			// check for collision
			if(!pathHitsUs)
			{
				if(MC.player.getBoundingBox().expand(1)
					.raycast(lastPos, arrowPos).isPresent())
				{
					pathHitsUs = true;
				}
			}
			RaycastContext context = new RaycastContext(lastPos, arrowPos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, projectile);
			BlockHitResult result = MC.world.raycast(context);
			if(result.getType() != HitResult.Type.MISS)
				break;
			lastPos = arrowPos;
		}
		
		return path;
	}
	
	private double getProjectileGravity(Entity entity)
	{
		if(entity instanceof ArrowEntity)
			return 0.05;
		
		if(entity instanceof PotionEntity)
			return 0.4;
		
		if(entity instanceof FishingBobberEntity)
			return 0.15;
		
		if(entity instanceof TridentEntity)
			return 0.015;
		
		if(entity instanceof FireballEntity)
			return 0;
		
		if(entity instanceof SmallFireballEntity)
			return 0;
		
		if(entity instanceof WitherSkullEntity)
			return 0;
		
		if(entity instanceof DragonFireballEntity)
			return 0;
		
		return 0.03;
	}
}

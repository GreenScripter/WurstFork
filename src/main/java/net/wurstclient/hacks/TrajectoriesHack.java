/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;

@SearchTags({"ArrowTrajectories", "ArrowPrediction", "aim assist",
	"arrow trajectories"})
public final class TrajectoriesHack extends Hack
	implements RenderListener, UpdateListener
{
	
	private final CheckboxSetting showOthers =
		new CheckboxSetting("Draw other players",
			"Will draw the trajectories of items held by other players.", true);
	private final CheckboxSetting showSelf =
		new CheckboxSetting("Draw your trajectories",
			"Will draw the trajectories of items held by you.", true);
	private final CheckboxSetting othersDepth = new CheckboxSetting(
		"Depth test other players",
		"If the trajectories of other player's items\nshould be on top of blocks.",
		true);
	
	public TrajectoriesHack()
	{
		super("Trajectories",
			"Predicts the flight path of arrows and throwable items.");
		setCategory(Category.RENDER);
		addSetting(showSelf);
		addSetting(showOthers);
		addSetting(othersDepth);
	}
	
	private final ArrayList<PlayerEntity> players = new ArrayList<>();
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientWorld world = MC.world;
		
		players.clear();
		if(showOthers.isChecked())
		{
			Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
				.parallelStream().filter(e -> !e.isRemoved() && e.getHealth() > 0)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
			
			players.addAll(stream.collect(Collectors.toList()));
		}else
		{
			players.add(MC.player);
		}
		if(!showSelf.isChecked())
		{
			players.remove(MC.player);
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		matrixStack.push();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		if(!othersDepth.isChecked())
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		
		RenderUtils.applyCameraRotationOnly();
		for(PlayerEntity player : players)
		{
			if(othersDepth.isChecked())
			{
				if(player == MC.player)
				{
					GL11.glDisable(GL11.GL_DEPTH_TEST);
				}
			}
			ArrayList<Vec3d> path = getPath(partialTicks, player);
			Vec3d camPos = RenderUtils.getCameraPos();
			
			drawLine(matrixStack, path, camPos);
			
			if(!path.isEmpty())
			{
				Vec3d end = path.get(path.size() - 1);
				drawEndOfLine(matrixStack, end, camPos);
			}
			if(othersDepth.isChecked())
			{
				if(player == MC.player)
				{
					GL11.glEnable(GL11.GL_DEPTH_TEST);
				}
			}
		}
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		matrixStack.pop();
	}
	
	private void drawLine(MatrixStack matrixStack, ArrayList<Vec3d> path,
		Vec3d camPos)
	{
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		RenderSystem.setShaderColor(0, 1, 0, 0.75F);
		
		for(Vec3d point : path)
			bufferBuilder
				.vertex(matrix, (float)(point.x - camPos.x),
					(float)(point.y - camPos.y), (float)(point.z - camPos.z))
				.next();
		
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
	}
	
	private void drawEndOfLine(MatrixStack matrixStack, Vec3d end, Vec3d camPos)
	{
		double renderX = end.x - camPos.x;
		double renderY = end.y - camPos.y;
		double renderZ = end.z - camPos.z;
		
		matrixStack.push();
		matrixStack.translate(renderX - 0.5, renderY - 0.5, renderZ - 0.5);
		
		RenderSystem.setShaderColor(0, 1, 0, 0.25F);
		RenderUtils.drawSolidBox(matrixStack);
		
		RenderSystem.setShaderColor(0, 1, 0, 0.75F);
		RenderUtils.drawOutlinedBox(matrixStack);
		
		matrixStack.pop();
	}
	
	private ArrayList<Vec3d> getPath(float partialTicks, PlayerEntity player)
	{
		ArrayList<Vec3d> path = new ArrayList<>();
		
		ItemStack stack = player.getMainHandStack();
		Item item = stack.getItem();
		
		// check if item is throwable
		if(stack.isEmpty() || !isThrowable(item))
		{
			// If it isn't look at offhand
			stack = player.getOffHandStack();
			item = stack.getItem();
			if(stack.isEmpty() || !isThrowable(item))
				return path;
		}
		
		// calculate starting position
		double arrowPosX = player.lastRenderX
			+ (player.getX() - player.lastRenderX) * partialTicks
			- Math.cos(Math.toRadians(player.getYaw())) * 0.16;
		
		double arrowPosY = player.lastRenderY
			+ (player.getY() - player.lastRenderY) * partialTicks
			+ player.getStandingEyeHeight() - 0.1;
		
		double arrowPosZ = player.lastRenderZ
			+ (player.getZ() - player.lastRenderZ) * partialTicks
			- Math.sin(Math.toRadians(player.getYaw())) * 0.16;
		
		// Motion factor. Arrows go faster than snowballs and all that...
		double arrowMotionFactor = item instanceof RangedWeaponItem ? 1.0 : 0.4;
		
		double yaw = Math.toRadians(player.getYaw());
		double pitch = Math.toRadians(player.getPitch());
		
		// calculate starting motion
		double arrowMotionX =
			-Math.sin(yaw) * Math.cos(pitch) * arrowMotionFactor;
		double arrowMotionY = -Math.sin(pitch) * arrowMotionFactor;
		double arrowMotionZ =
			Math.cos(yaw) * Math.cos(pitch) * arrowMotionFactor;
		
		// 3D Pythagorean theorem. Returns the length of the arrowMotion vector.
		double arrowMotion = Math.sqrt(arrowMotionX * arrowMotionX
			+ arrowMotionY * arrowMotionY + arrowMotionZ * arrowMotionZ);
		
		arrowMotionX /= arrowMotion;
		arrowMotionY /= arrowMotion;
		arrowMotionZ /= arrowMotion;
		
		// apply bow charge
		if(item instanceof RangedWeaponItem)
		{
			float bowPower = (72000 - player.getItemUseTimeLeft()) / 20.0f;
			bowPower = (bowPower * bowPower + bowPower * 2.0f) / 3.0f;
			
			if(bowPower > 1 || bowPower <= 0.1F)
				bowPower = 1;
			
			bowPower *= 3F;
			arrowMotionX *= bowPower;
			arrowMotionY *= bowPower;
			arrowMotionZ *= bowPower;
			
		}else
		{
			arrowMotionX *= 1.5;
			arrowMotionY *= 1.5;
			arrowMotionZ *= 1.5;
		}
		
		double gravity = getProjectileGravity(item);
		Vec3d eyesPos =
			new Vec3d(player.getX(), player.getEyeY(), player.getZ());
		
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
			RaycastContext context = new RaycastContext(eyesPos, arrowPos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player);
			if(MC.world.raycast(context).getType() != HitResult.Type.MISS)
				break;
			eyesPos = arrowPos;
		}
		
		return path;
	}
	
	private double getProjectileGravity(Item item)
	{
		if(item instanceof BowItem || item instanceof CrossbowItem)
			return 0.05;
		
		if(item instanceof PotionItem)
			return 0.4;
		
		if(item instanceof FishingRodItem)
			return 0.15;
		
		if(item instanceof TridentItem)
			return 0.015;
		
		return 0.03;
	}
	
	private boolean isThrowable(Item item)
	{
		return item instanceof BowItem || item instanceof CrossbowItem
			|| item instanceof SnowballItem || item instanceof EggItem
			|| item instanceof EnderPearlItem
			|| item instanceof SplashPotionItem
			|| item instanceof LingeringPotionItem
			|| item instanceof FishingRodItem || item instanceof TridentItem;
	}
}

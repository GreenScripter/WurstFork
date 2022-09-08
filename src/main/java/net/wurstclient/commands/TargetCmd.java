/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.StreamSupport;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class TargetCmd extends Command
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting debugMode =
		new CheckboxSetting("Debug mode", false);
	
	private final CheckboxSetting depthTest =
		new CheckboxSetting("Depth test", false);
	
	private PathFinder pathFinder;
	private boolean enabled;
	private long startTime;
	private BlockPos goal;
	
	public TargetCmd()
	{
		super("target",
			"Draw a tracer to specific coordinates.",
			".target <x> [y] <z>", ".path <entity>");
		
		addSetting(debugMode);
		addSetting(depthTest);
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(enabled)
		{
			EVENTS.remove(UpdateListener.class, this);
			EVENTS.remove(RenderListener.class, this);
			enabled = false;
			
			if(args.length == 0)
				return;
		}
		
		// set PathFinder
		goal = argsToPos(args);
		enabled = true;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		System.out.println("Finding path...");
		startTime = System.nanoTime();
	}
	
	private BlockPos argsToPos(String... args) throws CmdException
	{
		switch(args.length)
		{
			default:
			throw new CmdSyntaxError("Invalid coordinates.");
			
			case 1:
			return argsToEntityPos(args[0]);
			
			case 2:
			return argsToXyzPos(
				new String[]{args[0], MC.player.getBlockY() + "", args[1]});
			
			case 3:
			return argsToXyzPos(args);
		}
	}
	
	private BlockPos argsToEntityPos(String name) throws CmdError
	{
		LivingEntity entity = StreamSupport
			.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e)
			.filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> name.equalsIgnoreCase(e.getDisplayName().getString()))
			.min(
				Comparator.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError("Entity \"" + name + "\" could not be found.");
		
		return new BlockPos(entity.getPos());
	}
	
	private BlockPos argsToXyzPos(String... xyz) throws CmdSyntaxError
	{
		BlockPos playerPos = new BlockPos(MC.player.getPos());
		int[] player = {playerPos.getX(), playerPos.getY(), playerPos.getZ()};
		int[] pos = new int[3];
		
		for(int i = 0; i < 3; i++)
			if(MathUtils.isInteger(xyz[i]))
				pos[i] = Integer.parseInt(xyz[i]);
			else if(xyz[i].equals("~"))
				pos[i] = player[i];
			else if(xyz[i].startsWith("~")
				&& MathUtils.isInteger(xyz[i].substring(1)))
				pos[i] = player[i] + Integer.parseInt(xyz[i].substring(1));
			else
				throw new CmdSyntaxError("Invalid coordinates.");
			
		return new BlockPos(pos[0], pos[1], pos[2]);
	}
	
	@Override
	public void onUpdate()
	{
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		
		BlockPos pos = goal;
		if(pos == null)
			return;
		// GL settings
		matrixStack.push();
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		RenderUtils.applyRenderOffset(matrixStack);
		
		RenderSystem.setShaderColor(1, 0, 1, 0.5F);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		// tracer line
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		// set start position
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos());
		
		// set end position
		Vec3d end = Vec3d.ofCenter(pos);
		
		// draw line
		bufferBuilder
			.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
			.next();
		bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
			.next();
		
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		// block box
		{
			matrixStack.push();
			matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
			
			RenderUtils.drawOutlinedBox(matrixStack);
			
			RenderSystem.setShaderColor(1, 0, 1, 0.5F);
			RenderUtils.drawSolidBox(matrixStack);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	public boolean isDepthTest()
	{
		return depthTest.isChecked();
	}
}

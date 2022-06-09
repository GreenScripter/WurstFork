package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"compass hack", "tracer"})
public final class CompassHack extends Hack implements RenderListener
{
	
	private final CheckboxSetting worldSpawn =
		new CheckboxSetting("World Spawn",
			"Tracer to the world spawn, or plugin modified compass.", true);
	private final CheckboxSetting lastDeath = new CheckboxSetting("Last Death",
		"Tracer to your last death compass if you died.", false);
	
	public CompassHack()
	{
		super("CompassHack", "Draws a line to the current compass target.");
		setCategory(Category.RENDER);
		addSetting(worldSpawn);
		addSetting(lastDeath);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
		
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		
		if(worldSpawn.isChecked())
		{
			BlockPos pos = MC.world.getSpawnPos();
			if(pos == null)
				return;
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			RenderUtils.applyRegionalRenderOffset(matrixStack);
			RenderSystem.setShaderColor(1, 1, 1, 0.5f);
			
			BlockPos camPos = RenderUtils.getCameraBlockPos();
			int regionX = (camPos.getX() >> 9) * 512;
			int regionZ = (camPos.getZ() >> 9) * 512;
			// System.out.println(regionX + ", " + regionZ);
			Vec3d start = RotationUtils.getClientLookVec()
				.add(RenderUtils.getCameraPos()).subtract(regionX, 0, regionZ);
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			Matrix4f matrix = matrixStack.peek().getPositionMatrix();
			
			Tessellator tessellator = RenderSystem.renderThreadTesselator();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION_COLOR);
			
			{
				Vec3d end = new Vec3d(0.5, 0.5, 0.5)
					.add(pos.getX(), pos.getY(), pos.getZ())
					.subtract(regionX, 0, regionZ);
				
				float r, g, b;
				
				r = 0;
				g = 1;
				b = 1;
				
				bufferBuilder.vertex(matrix, (float)start.x, (float)start.y,
					(float)start.z).color(r, g, b, 0.5F).next();
				
				bufferBuilder
					.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
					.color(r, g, b, 0.5F).next();
			}
			
			tessellator.draw();
			RenderSystem.setShaderColor(1, 1, 1, 1);
			RenderUtils.unapplyRegionalRenderOffset(matrixStack);
			
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}
		if(lastDeath.isChecked())
		{
			GlobalPos gpos = MC.player.getLastDeathPos().orElse(null);
			if(gpos == null)
				return;
			BlockPos pos = gpos.getPos();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			RenderUtils.applyRegionalRenderOffset(matrixStack);
			RenderSystem.setShaderColor(1, 1, 1, 0.5f);
			
			BlockPos camPos = RenderUtils.getCameraBlockPos();
			int regionX = (camPos.getX() >> 9) * 512;
			int regionZ = (camPos.getZ() >> 9) * 512;
			// System.out.println(regionX + ", " + regionZ);
			Vec3d start = RotationUtils.getClientLookVec()
				.add(RenderUtils.getCameraPos()).subtract(regionX, 0, regionZ);
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			Matrix4f matrix = matrixStack.peek().getPositionMatrix();
			
			Tessellator tessellator = RenderSystem.renderThreadTesselator();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION_COLOR);
			
			{
				Vec3d end = new Vec3d(0.5, 0.5, 0.5)
					.add(pos.getX(), pos.getY(), pos.getZ())
					.subtract(regionX, 0, regionZ);
				
				float r, g, b;
				
				r = 1;
				g = 0;
				b = 0;
				
				bufferBuilder.vertex(matrix, (float)start.x, (float)start.y,
					(float)start.z).color(r, g, b, 0.5F).next();
				
				bufferBuilder
					.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
					.color(r, g, b, 0.5F).next();
			}
			
			tessellator.draw();
			RenderSystem.setShaderColor(1, 1, 1, 1);
			RenderUtils.unapplyRegionalRenderOffset(matrixStack);
			
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}
	}
	
}

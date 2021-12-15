package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"compass hack", "tracer"})
public final class CompassHack extends Hack implements RenderListener
{
	public CompassHack()
	{
		super("CompassHack", "Draws a line to the current compass target.");
		setCategory(Category.RENDER);
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
		
		BlockPos pos = MC.world.getSpawnPos();
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
		
		RenderSystem.setShaderColor(0, 1, 1, 0.5F);
		
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
			
			RenderSystem.setShaderColor(0, 1, 1, 0.5F);
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
	
}

package net.wurstclient.hacks;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"end portal", "portal finder"})
public final class EnderEyeHack extends Hack
	implements PacketInputListener, RenderListener
{
	public EnderEyeHack()
	{
		super("EnderEye",
			"Triangulates the location of the end portal based on the directions of two thrown eyes of ender.");
		setCategory(Category.RENDER);
	}
	
	Vec2f positionLast;
	Vec2f positionNext;
	Vec2f directionLast;
	Vec3i pos;
	
	Vec3d start = null;
	
	int next = Integer.MIN_VALUE;
	Map<Integer, Vec3d> eyePaths = new HashMap<>();
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		positionLast = null;
		positionNext = null;
		directionLast = null;
		pos = null;
		
		start = null;
		
		next = Integer.MIN_VALUE;
		eyePaths.clear();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
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
		
		// generate rainbow color
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float red = 0.5F + 0.5F * MathHelper.sin(x * (float)Math.PI);
		float green =
			0.5F + 0.5F * MathHelper.sin((x + 4F / 3F) * (float)Math.PI);
		float blue =
			0.5F + 0.5F * MathHelper.sin((x + 8F / 3F) * (float)Math.PI);
		RenderSystem.setShaderColor(red, green, blue, 0.5F);
		
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
			
			RenderSystem.setShaderColor(red, green, blue, 0.25F);
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
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(event.getPacket() instanceof EntitiesDestroyS2CPacket)
		{
			EntitiesDestroyS2CPacket packet =
				(EntitiesDestroyS2CPacket)event.getPacket();
			for(int id : packet.getEntityIds())
			{
				if(eyePaths.containsKey(id))
				{
					Vec3d position = eyePaths.remove(id);
					Vec3d now =
						MC.world.getEntityById(id).getPos().subtract(position);
					Vec2f direction =
						new Vec2f((float)now.getX(), (float)now.getZ());
					if(positionLast != null)
					{
						float m1 = directionLast.y / directionLast.x;
						float m2 = direction.y / direction.x;
						float a = positionLast.y - m1 * positionLast.x;
						float b = (float)(position.z - m2 * position.x);
						float x = (b - a) / (m1 - m2);
						float y = m2 * x + b;
						pos = new Vec3i(x, 60, y);
						ChatUtils.message("End portal location: X: "
							+ pos.getX() + " Z: " + pos.getZ());
					}
					positionLast =
						new Vec2f((float)position.x, (float)position.z);
					directionLast = direction;
					next = Integer.MIN_VALUE;
					return;
				}
			}
		}
		if(event.getPacket() instanceof EntitySpawnS2CPacket)
		{
			EntitySpawnS2CPacket packet =
				(EntitySpawnS2CPacket)event.getPacket();
			if(packet.getEntityTypeId().equals(EntityType.EYE_OF_ENDER))
			{
				next = packet.getId();
				Vec2f position =
					new Vec2f((float)packet.getX(), (float)packet.getZ());
				start = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
				positionNext = position;
				eyePaths.put(next, start);
				
			}
			
		}
	}
	
}

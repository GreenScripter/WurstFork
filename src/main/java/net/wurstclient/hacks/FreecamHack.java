/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;


import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.IsNormalCubeListener;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.PlayerMoveListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.SetOpaqueCubeListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
@SearchTags({"free camera", "spectator"})
public final class FreecamHack extends Hack
	implements UpdateListener, PacketOutputListener, PlayerMoveListener,
	IsPlayerInWaterListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener
{
	
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"Draws a line to your character's actual position.", false);
	public final CheckboxSetting lockLooking = new CheckboxSetting(
		"Lock Looking",
		"If looking is locked you will control your character's direction instead of the camera.",
		false);
	public final CheckboxSetting lockMovement = new CheckboxSetting(
		"Lock Movement",
		"If movement is locked you will control your character's movement instead of the camera.",
		false);
	
	
	public static Vec3d position;
	public static float partialTicks;
	private Perspective start;
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(tracer);
		addSetting(lockLooking);
		addSetting(lockMovement);
		addSetting(color);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(PlayerMoveListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(RenderListener.class, this);
		position = new Vec3d(MC.player.getPos().x, MC.player.getEyeY(),
			MC.player.getPos().z);
		// fakePlayer = new FakePlayerEntity();
		// fakePlayerDisplay = new FakePlayerEntity();
		// fakePlayer.setPos(MC.player.getX(), MC.player.getY(),
		// MC.player.getZ());
		// fakePlayer.setGameMode(GameMode.SPECTATOR);
		MC.chunkCullingEnabled = false;
		start = MC.options.getPerspective();
		MC.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		
		GameOptions gs = MC.options;
		KeyBinding[] bindings = {gs.forwardKey, gs.backKey, gs.leftKey,
			gs.rightKey, gs.jumpKey, gs.sneakKey};
		
		for(KeyBinding binding : bindings)
			binding.setPressed(((IKeyBinding)binding).isActallyPressed());
			
		// playerBox = GL11.glGenLists(1);
		// GL11.glNewList(playerBox, GL11.GL_COMPILE);
		// Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		// RenderUtils.drawOutlinedBox(bb, GL11.);
		// GL11.glEndList();
		// MC.setCameraEntity(fakePlayer);
		
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(PlayerMoveListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		MC.options.setPerspective(start);
		
		position = null;
		// fakePlayer.resetPlayerPosition();
		// fakePlayer.despawn();
		// fakePlayerDisplay.despawn();
		// MC.setCameraEntity(MC.player);
		
		// ClientPlayerEntity player = MC.player;
		// player.setVelocity(Vec3d.ZERO);
		MC.chunkCullingEnabled = true;
		
		MC.worldRenderer.reload();
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.currentScreen == null)
		{
			if(!MC.options.getPerspective()
				.equals(Perspective.THIRD_PERSON_BACK))
			{
				MC.options.setPerspective(Perspective.THIRD_PERSON_BACK);
				
			}
			if(!lockMovement.isChecked())
			{
				MC.options.jumpKey.setPressed(false);
				MC.options.sneakKey.setPressed(false);
				MC.options.forwardKey.setPressed(false);
				MC.options.backKey.setPressed(false);
				MC.options.leftKey.setPressed(false);
				MC.options.rightKey.setPressed(false);
				WurstClient.MC.player.setSprinting(false);
			}
		}
		
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
	}
	
	@Override
	public void onPlayerMove(IClientPlayerEntity player)
	{
		// player.setNoClip(true);
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		// event.setInWater(false);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(tracer.isChecked())
			event.cancel();
	}
	
	@Override
	public void onIsNormalCube(IsNormalCubeEvent event)
	{
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event)
	{
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		float passed = partialTicks - FreecamHack.partialTicks;
		if(FreecamHack.partialTicks > partialTicks)
		{
			passed = (1 + partialTicks) - FreecamHack.partialTicks;
		}
		if(!lockMovement.isChecked())
		{
			if(MC.currentScreen == null)
			{
				if(((IKeyBinding)MC.options.jumpKey).isActallyPressed())
				{
					position = new Vec3d(position.x,
						position.y + speed.getValue() * passed, position.z);
					MC.options.jumpKey.setPressed(false);
					
				}
				if(((IKeyBinding)MC.options.sneakKey).isActallyPressed())
				{
					position = new Vec3d(position.x,
						position.y - speed.getValue() * passed, position.z);
					MC.options.sneakKey.setPressed(false);
					
				}
				Vec3d look = RotationUtils
					.getMoveVec(MC.gameRenderer.getCamera().getYaw());
				look = new Vec3d(look.x, 0, look.z).normalize()
					.multiply(speed.getValue() * passed);
				if(((IKeyBinding)MC.options.forwardKey).isActallyPressed())
				{
					position = new Vec3d(position.x + look.x, position.y,
						position.z + look.z);
					MC.options.forwardKey.setPressed(false);
				}
				if(((IKeyBinding)MC.options.backKey).isActallyPressed())
				{
					position = new Vec3d(position.x - look.x, position.y,
						position.z - look.z);
					MC.options.backKey.setPressed(false);
				}
				look = look.crossProduct(new Vec3d(0, 1, 0)).normalize()
					.multiply(speed.getValue() * passed);
				if(((IKeyBinding)MC.options.leftKey).isActallyPressed())
				{
					position = new Vec3d(position.x - look.x, position.y,
						position.z - look.z);
					MC.options.leftKey.setPressed(false);
				}
				if(((IKeyBinding)MC.options.rightKey).isActallyPressed())
				{
					position = new Vec3d(position.x + look.x, position.y,
						position.z + look.z);
					MC.options.rightKey.setPressed(false);
				}
			}
		}
		FreecamHack.partialTicks = partialTicks;
		if(!tracer.isChecked())
			return;
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRenderOffset(matrixStack);
		
		float[] colorF = color.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
		
		// box
		Vec3d start =
			RotationUtils.getCameraLookVec().add(RenderUtils.getCameraPos());
		Vec3d end = MC.player.getBoundingBox().getCenter();
		
		matrixStack.push();
		matrixStack.translate(MC.player.getX(), MC.player.getY(),
			MC.player.getZ());
		matrixStack.scale(MC.player.getWidth() + 0.1f,
			MC.player.getHeight() + 0.1f, MC.player.getWidth() + 0.1f);
		Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		RenderUtils.drawOutlinedBox(bb, matrixStack);
		matrixStack.pop();
		
		// line
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder
			.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
			.next();
		bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
			.next();
		tessellator.draw();
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
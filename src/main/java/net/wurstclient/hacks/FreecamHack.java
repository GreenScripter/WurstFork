/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.lang.reflect.Field;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.options.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
@SearchTags({ "free camera", "spectator" })
public final class FreecamHack extends Hack implements UpdateListener, PacketOutputListener, PlayerMoveListener, IsPlayerInWaterListener, CameraTransformViewBobbingListener, IsNormalCubeListener, SetOpaqueCubeListener, RenderListener {
	
	private final SliderSetting speed = new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer", "Draws a line to your character's actual position.", false);
	
	private int playerBox;
	
	public static Vec3d position;
	public static float partialTicks;
	private Perspective start;
	public FreecamHack() {
		super("Freecam", "Allows you to move the camera without moving your character.");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(tracer);
	}
	
	@Override
	public void onEnable() {
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(PlayerMoveListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(RenderListener.class, this);
		position = new Vec3d(MC.player.getPos().x, MC.player.getEyeY(), MC.player.getPos().z);
//		fakePlayer = new FakePlayerEntity();
//		fakePlayerDisplay = new FakePlayerEntity();
//		fakePlayer.setPos(MC.player.getX(), MC.player.getY(), MC.player.getZ());
//		fakePlayer.setGameMode(GameMode.SPECTATOR);
		MC.chunkCullingEnabled = false;
		start = MC.options.getPerspective();
		MC.options.method_31043(Perspective.THIRD_PERSON_BACK);
		
		GameOptions gs = MC.options;
		KeyBinding[] bindings = { gs.keyForward, gs.keyBack, gs.keyLeft, gs.keyRight, gs.keyJump, gs.keySneak };
		
		for (KeyBinding binding : bindings)
			binding.setPressed(((IKeyBinding) binding).isActallyPressed());
		
		playerBox = GL11.glGenLists(1);
		GL11.glNewList(playerBox, GL11.GL_COMPILE);
		Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
//		MC.setCameraEntity(fakePlayer);
		try {
			Field f = Camera.class.getDeclaredField("pos");
			f.setAccessible(true);
			Camera c = MC.gameRenderer.getCamera();
			f.set(c, c.getPos().add(0, 100, 0));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(PlayerMoveListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		MC.options.method_31043(start);

		position = null;
//		fakePlayer.resetPlayerPosition();
//		fakePlayer.despawn();
//		fakePlayerDisplay.despawn();
//		MC.setCameraEntity(MC.player);
		
//		ClientPlayerEntity player = MC.player;
//		player.setVelocity(Vec3d.ZERO);
		MC.chunkCullingEnabled = true;

		MC.worldRenderer.reload();
		
		GL11.glDeleteLists(playerBox, 1);
		playerBox = 0;
	}
	
	@Override
	public void onUpdate() {
		if (MC.currentScreen == null) {
			if (!MC.options.getPerspective().equals(Perspective.THIRD_PERSON_BACK)) {
				MC.options.method_31043(Perspective.THIRD_PERSON_BACK);
				
			}
			MC.options.keyJump.setPressed(false);
			MC.options.keySneak.setPressed(false);
			MC.options.keyForward.setPressed(false);
			MC.options.keyBack.setPressed(false);
			MC.options.keyLeft.setPressed(false);
			MC.options.keyRight.setPressed(false);
			WurstClient.MC.player.setSprinting(false);
		}
		
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event) {
	}
	
	@Override
	public void onPlayerMove(IClientPlayerEntity player) {
		//		player.setNoClip(true);
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event) {
		//		event.setInWater(false);
	}
	
	@Override
	public void onCameraTransformViewBobbing(CameraTransformViewBobbingEvent event) {
		if (tracer.isChecked()) event.cancel();
	}
	
	@Override
	public void onIsNormalCube(IsNormalCubeEvent event) {
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event) {
	}
	
	@Override
	public void onRender(float partialTicks) {
		float passed = partialTicks - FreecamHack.partialTicks;
		if (FreecamHack.partialTicks > partialTicks) {
			 passed = (1 + partialTicks) - FreecamHack.partialTicks;
		}
		if (MC.currentScreen == null) {
			if (((IKeyBinding) MC.options.keyJump).isActallyPressed()) {
				position = new Vec3d(position.x, position.y + speed.getValue() * passed, position.z);
				MC.options.keyJump.setPressed(false);
				
			}
			if (((IKeyBinding) MC.options.keySneak).isActallyPressed()) {
				position = new Vec3d(position.x, position.y - speed.getValue() * passed, position.z);
				MC.options.keySneak.setPressed(false);
				
			}
			Vec3d look = RotationUtils.getMoveVec(MC.gameRenderer.getCamera().getYaw());
			look = new Vec3d(look.x, 0, look.z).normalize().multiply(speed.getValue() * passed);
			if (((IKeyBinding) MC.options.keyForward).isActallyPressed()) {
				position = new Vec3d(position.x + look.x, position.y, position.z + look.z);
				MC.options.keyForward.setPressed(false);
			}
			if (((IKeyBinding) MC.options.keyBack).isActallyPressed()) {
				position = new Vec3d(position.x - look.x, position.y, position.z - look.z);
				MC.options.keyBack.setPressed(false);
			}
			look = look.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(speed.getValue() * passed);
			if (((IKeyBinding) MC.options.keyLeft).isActallyPressed()) {
				position = new Vec3d(position.x - look.x, position.y, position.z - look.z);
				MC.options.keyLeft.setPressed(false);
			}
			if (((IKeyBinding) MC.options.keyRight).isActallyPressed()) {
				position = new Vec3d(position.x + look.x, position.y, position.z + look.z);
				MC.options.keyRight.setPressed(false);
			}
		}
		FreecamHack.partialTicks = partialTicks;
		if (!tracer.isChecked()) return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		GL11.glColor4f(1, 1, 1, 0.5F);
		
		// box
		GL11.glPushMatrix();
		GL11.glTranslated(MC.player.getX(), MC.player.getY(), MC.player.getZ());
		GL11.glScaled(MC.player.getWidth() + 0.1, MC.player.getHeight() + 0.1, MC.player.getWidth() + 0.1);
		GL11.glCallList(playerBox);
		GL11.glPopMatrix();
		
		// line
		Vec3d start = RotationUtils.getCameraLookVec().add(RenderUtils.getCameraPos());
		Vec3d end = MC.player.getBoundingBox().getCenter();
		
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(start.x, start.y, start.z);
		GL11.glVertex3d(end.x, end.y, end.z);
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}

/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;
import net.wurstclient.mixinterface.ICamera;

@Mixin(Camera.class)
public abstract class CameraMixin implements ICamera
{
	
	@Shadow
	private Vec3d pos;
	@Shadow
	private float pitch;
	@Shadow
	private float yaw;

	
	@Inject(at = {@At("HEAD")},
		method = {"clipToSpace(D)D"},
		cancellable = true)
	private void onClipToSpace(double desiredCameraDistance,
		CallbackInfoReturnable<Double> cir)
	{
		if (WurstClient.INSTANCE.getHax().freecamHack.isEnabled()){
			cir.setReturnValue(0d);
		} else if(WurstClient.INSTANCE.getHax().cameraNoClipHack.isEnabled())
			cir.setReturnValue(desiredCameraDistance);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getSubmersionType()Lnet/minecraft/client/render/CameraSubmersionType;"},
		cancellable = true)
	private void onGetSubmersionType(
		CallbackInfoReturnable<CameraSubmersionType> cir)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled() || WurstClient.INSTANCE.getHax().freecamHack.isEnabled())
			cir.setReturnValue(CameraSubmersionType.NONE);
	}
	
	@Inject(at = {@At("HEAD")},
			method = {"getPos()Lnet/minecraft/util/math/Vec3d;"},
			cancellable = true)
	public void getPos(CallbackInfoReturnable<Vec3d> cir) {
		if (FreecamHack.position != null) {//MathHelper.lerp(FreecamHack.partialTicks, FreecamHack.offset.x, FreecamHack.lastOffset.x), MathHelper.lerp(FreecamHack.partialTicks, FreecamHack.offset.y, FreecamHack.lastOffset.y), MathHelper.lerp(FreecamHack.partialTicks, FreecamHack.offset.z, FreecamHack.lastOffset.z)
			cir.setReturnValue(FreecamHack.position);
		}
	}
	
	@Inject(at = {@At("HEAD")},
			method = {"setRotation(FF)V"},
			cancellable = true)
	public void setRotation(CallbackInfo cir) {
		if (WurstClient.INSTANCE.getHax().freecamHack.isEnabled()) {
			cir.cancel();
		}
	}
	
	public void setPitch(float p){
		pitch = p;
	}
	public void setYaw(float y){
		yaw = y;
	}
}

/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VelocityFromFluidListener.VelocityFromFluidEvent;
import net.wurstclient.mixinterface.ICamera;

@Mixin(Entity.class)
public abstract class EntityMixin implements Nameable, CommandOutput
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0),
		method = {"updateMovementInFluid(Lnet/minecraft/tag/Tag;D)Z"})
	private void setVelocityFromFluid(Entity entity, Vec3d velocity)
	{
		VelocityFromFluidEvent event = new VelocityFromFluidEvent();
		EventManager.fire(event);
		
		if(!event.isCancelled())
			entity.setVelocity(velocity);
	}
	
	@Inject(at = { @At("HEAD") }, method = { "changeLookDirection(DD)V" }, cancellable = true)
	private void changeLookDirection(double dx, double dy, CallbackInfo ci) {
		
		if (WurstClient.INSTANCE.getHax().freecamHack.isEnabled()) {
			double d = dy * 0.15D;
		      double e = dx * 0.15D;
		      Camera c = MinecraftClient.getInstance().gameRenderer.getCamera();
		      ((ICamera)c).setPitch(MathHelper.clamp((float)((double)c.getPitch() + d), -90.0F, 90.0F));
		      ((ICamera)c).setYaw((float)((double)c.getYaw() + e));
		      
//		      this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
//		      this.prevPitch = (float)((double)this.prevPitch + d);
//		      this.prevYaw = (float)((double)this.prevYaw + e);
//		      this.prevPitch = MathHelper.clamp(this.prevPitch, -90.0F, 90.0F);
//		      if (this.vehicle != null) {
//		         this.vehicle.onPassengerLookAround(this);
//		      }
			ci.cancel();
		}
		
	}
}

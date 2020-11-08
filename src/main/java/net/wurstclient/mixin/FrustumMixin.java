/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved. This source code is subject to
 * the terms of the GNU General Public License, version 3. If a copy of the GPL was not distributed
 * with this file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Box;
import net.wurstclient.WurstClient;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
	
	@Shadow
	private Vector4f[] homogeneousCoordinates;
	
	@Shadow
	private double x;
	
	@Shadow
	private double y;
	
	@Shadow
	private double z;
	
	public boolean isVisible(Box box) {
		if (WurstClient.INSTANCE.getHax().freecamHack.isEnabled()) {
			return true;
		}
		return this.isVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}
	
	private boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		float f = (float) (minX - this.x);
		float g = (float) (minY - this.y);
		float h = (float) (minZ - this.z);
		float i = (float) (maxX - this.x);
		float j = (float) (maxY - this.y);
		float k = (float) (maxZ - this.z);
		return this.isAnyCornerVisible(f, g, h, i, j, k);
	}
	
	private boolean isAnyCornerVisible(float x1, float y1, float z1, float x2, float y2, float z2) {
		for (int i = 0; i < 6; ++i) {
			Vector4f vector4f = this.homogeneousCoordinates[i];
			if (vector4f.dotProduct(new Vector4f(x1, y1, z1, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x2, y1, z1, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x1, y2, z1, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x2, y2, z1, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x1, y1, z2, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x2, y1, z2, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x1, y2, z2, 1.0F)) <= 0.0F && vector4f.dotProduct(new Vector4f(x2, y2, z2, 1.0F)) <= 0.0F) {
				return false;
			}
		}
		
		return true;
	}
}
/*
value
[03:40:41] [main/WARN] (mixin) @Mixin target net/minecraft/client/particle/ParticleManager$SimpleSpriteProvider is public in fabric-particles-v1.mixins.json:ParticleManagerAccessor$SimpleSpriteProviderAccessor and should be specified in value
[03:40:41] [main/WARN] (mixin) @Mixin target net/minecraft/tag/Tag$1 is public in fabric-tag-extensions-v0.mixins.json:MixinTagImpl and should be specified in value
[03:40:41] [main/WARN] (mixin) @Mixin target net/minecraft/tag/RequiredTagList$TagWrapper is public in fabric-tag-extensions-v0.mixins.json:MixinTagImpl and should be specified in value
[03:40:41] [main/ERROR] (mixin) wurst.mixins.json:FrustumMixin: Super class 'net.minecraft.client.render.Frustum' of FrustumMixin was not found in the hierarchy of target class 'net/minecraft/client/render/Frustum'
org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException: Super class 'net.minecraft.client.render.Frustum' of FrustumMixin was not found in the hierarchy of target class 'net/minecraft/client/render/Frustum'
*/
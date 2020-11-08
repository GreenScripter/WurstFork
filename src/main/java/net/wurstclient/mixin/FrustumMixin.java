/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved. This source code is subject to
 * the terms of the GNU General Public License, version 3. If a copy of the GPL was not distributed
 * with this file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import net.wurstclient.WurstClient;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
	
	@Inject(at = {@At("HEAD")},
			method = {"isVisible(Lnet/minecraft/util/math/Box;)Z"},
			cancellable = true)
	public void isVisible(Box box, CallbackInfoReturnable<Boolean> cir) {
		if (WurstClient.INSTANCE.getHax().freecamHack.isEnabled()) {
			cir.setReturnValue(true);
		}
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
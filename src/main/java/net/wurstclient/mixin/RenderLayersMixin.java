package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.wurstclient.WurstClient;

@Mixin(RenderLayers.class)
public class RenderLayersMixin
{
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;"},
		cancellable = true)
	private static void getBlockLayer(BlockState state,
		CallbackInfoReturnable<RenderLayer> cir)
	{
		if(state.getBlock() instanceof BarrierBlock)
		{
			if(WurstClient.INSTANCE.getHax().barrierSightHack.opaque
				.isChecked())
			{
				cir.setReturnValue(RenderLayer.getSolid());
			}else
			{
				cir.setReturnValue(RenderLayer.getTranslucent());
			}
		}
		
	}
}

/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.wurstclient.WurstClient;

@Mixin(BarrierBlock.class)
public abstract class BarrierBlockMixin extends Block
{
	public BarrierBlockMixin(Settings settings)
	{
		super(settings);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getRenderType(Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockRenderType;"},
		cancellable = true)
	private void getRenderType(BlockState state,
		CallbackInfoReturnable<BlockRenderType> cir)
	{
		if(WurstClient.INSTANCE.getHax().barrierSightHack.isEnabled())
			cir.setReturnValue(BlockRenderType.MODEL);
		
	}
	
	@Environment(EnvType.CLIENT)
	@Override
	public boolean isSideInvisible(BlockState state, BlockState stateFrom,
		Direction direction)
	{
		return(stateFrom.isOf(this) ? true
			: super.isSideInvisible(state, stateFrom, direction));
	}
	
}

/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.SlabBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.hack.Hack;

public final class SingleSlabHack extends Hack implements RightClickListener
{
	public SingleSlabHack()
	{
		super("SingleSlab",
			"Prevents you from placing multiple slabs in the same block.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		
		if(!MC.interactionManager.isBreakingBlock())
		{
			Hand[] var1 = Hand.values();
			int var2 = var1.length;
			
			for(int var3 = 0; var3 < var2; ++var3)
			{
				Hand hand = var1[var3];
				if(MC.crosshairTarget != null)
				{
					switch(MC.crosshairTarget.getType())
					{
						case BLOCK:
						BlockHitResult hit =
							(BlockHitResult)MC.crosshairTarget;
						
						if (MC.world.getBlockState(hit.getBlockPos()).getBlock() instanceof SlabBlock) {
								event.cancel();
						}
						
						break;
						default:
						break;
					}
				}
				
			}
		}
		
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RightClickListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RightClickListener.class, this);
	}
}

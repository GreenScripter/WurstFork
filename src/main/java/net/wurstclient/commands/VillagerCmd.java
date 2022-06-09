/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.LecternBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.RaycastContext;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

public final class VillagerCmd extends Command
	implements UpdateListener, PacketInputListener
{
	
	public VillagerCmd()
	{
		super("villager",
			"Automatically manipulate Librarians to get specific enchantments.",
			".villager <enchantment> <level>");
	}
	
	Set<String> goals = new HashSet<>();
	Map<String, Integer> levelGoal = new HashMap<>();
	private BlockPos currentBlock;
	
	boolean waiting = false;
	boolean waitForNone = false;
	
	@Override
	public void call(String[] args) throws CmdException
	{
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		waiting = false;
		goals.clear();
		levelGoal.clear();
		if(args.length < 2 || args.length % 2 == 1)
			throw new CmdSyntaxError();
		
		for(int i = 0; i < args.length; i += 2)
		{
			String goal = args[i];
			
			boolean found = false;
			for(String e : Enchantments.ALL_ENCHANTMENTS)
			{
				if(goal.equalsIgnoreCase(e))
				{
					goal = e;
					found = true;
					break;
				}
			}
			
			if(!found)
			{
				throw new CmdSyntaxError(
					"Valid enchantments: " + Enchantments.ALL_ENCHANTMENTS);
			}
			try
			{
				int level = Integer.parseInt(args[i + 1]);
				goals.add(goal);
				levelGoal.put(goal, level);
			}catch(Exception e)
			{
				throw new CmdSyntaxError("Invalid level");
			}
		}
		EVENTS.add(PacketInputListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		setCurrentBlockFromHitResult();
		
		if(currentBlock != null)
		{
			breakCurrentBlock();
		}else
		{
			if(MC.crosshairTarget != null && MC.crosshairTarget.getPos() != null
				&& MC.crosshairTarget.getType() == HitResult.Type.BLOCK
				&& (MC.crosshairTarget instanceof BlockHitResult))
			{
				if(!waitForNone)
				{
					BlockHitResult result =
						((BlockHitResult)MC.crosshairTarget);
					placeBlockLegit(result.getBlockPos()
						.add(result.getSide().getVector().multiply(1)));
					EVENTS.remove(UpdateListener.class, this);
				}
				
			}
		}
	}
	
	private void setCurrentBlockFromHitResult()
	{
		if(MC.crosshairTarget == null || MC.crosshairTarget.getPos() == null
			|| MC.crosshairTarget.getType() != HitResult.Type.BLOCK
			|| !(MC.crosshairTarget instanceof BlockHitResult)
			|| !(MC.world
				.getBlockState(
					((BlockHitResult)MC.crosshairTarget).getBlockPos())
				.getBlock() instanceof LecternBlock))
		{
			stopMiningAndResetProgress();
			return;
		}
		
		currentBlock = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
	}
	
	private void breakCurrentBlock()
	{
		if(MC.player.getAbilities().creativeMode)
			BlockBreaker.breakBlocksWithPacketSpam(Arrays.asList(currentBlock));
		else
			BlockBreaker.breakOneBlock(currentBlock);
	}
	
	private void stopMiningAndResetProgress()
	{
		if(currentBlock == null)
			return;
		
		IMC.getInteractionManager().setBreakingBlock(true);
		MC.interactionManager.cancelBlockBreaking();
		currentBlock = null;
	}
	
	private boolean placeBlockLegit(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3d dirVec = Vec3d.of(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));
			
			// check if hitVec is within range (4.25 blocks)
			if(eyesPos.squaredDistanceTo(hitVec) > 18.0625)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;
			
			// check line of sight
			if(MC.world
				.raycast(new RaycastContext(eyesPos, hitVec,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			// face block
			Rotation rotation = RotationUtils.getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookAndOnGround packet =
				new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(),
					rotation.getPitch(), MC.player.isOnGround());
			MC.player.networkHandler.sendPacket(packet);
			
			// place block
			MC.interactionManager.interactBlock(MC.player, Hand.OFF_HAND,
				new BlockHitResult(hitVec, side, pos, false));
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(event.getPacket() instanceof EntityTrackerUpdateS2CPacket)
		{
			EntityTrackerUpdateS2CPacket packet =
				(EntityTrackerUpdateS2CPacket)event.getPacket();
			packet.getTrackedValues().forEach((e) -> {
				if(e.get() instanceof VillagerData)
				{
					VillagerData data = (VillagerData)e.get();
					if(data.getProfession().equals(VillagerProfession.NONE)
						&& waitForNone)
					{
						waitForNone = false;
					}
					if(data.getProfession().equals(VillagerProfession.LIBRARIAN)
						&& !waitForNone)
					{
						Entity target = MC.world.getEntityById(packet.id());
						if(target != null && target.distanceTo(MC.player) < 5)
						{
							Rotation rotation =
								RotationUtils.getNeededRotations(
									target.getBoundingBox().getCenter());
							PlayerMoveC2SPacket.LookAndOnGround out =
								new PlayerMoveC2SPacket.LookAndOnGround(
									rotation.getYaw(), rotation.getPitch(),
									MC.player.isOnGround());
							MC.player.networkHandler.sendPacket(out);
							
							MC.interactionManager.interactEntity(MC.player,
								target, Hand.MAIN_HAND);
							MC.player.swingHand(Hand.MAIN_HAND);
							waiting = true;
						}
					}
				}
			});
		}
		if(event.getPacket() instanceof SetTradeOffersS2CPacket && waiting)
		{
			SetTradeOffersS2CPacket packet =
				(SetTradeOffersS2CPacket)event.getPacket();
			ItemStack stack = null;
			boolean match = false;
			for(TradeOffer o : packet.getOffers())
			{
				if(o.getSellItem().getItem() instanceof EnchantedBookItem)
				{
					stack = o.getSellItem();
				}
			}
			if(stack != null)
			{
				NbtCompound tag = stack.getOrCreateNbt();
				NbtList stored =
					tag.getList("StoredEnchantments", NbtType.COMPOUND);
				NbtCompound entry = stored.getCompound(0);
				short lvl = entry.getShort("lvl");
				String id = entry.getString("id");
				System.out.println(id + " " + lvl);
				for(Entry<String, Integer> e : levelGoal.entrySet())
				{
					if(id.equalsIgnoreCase("minecraft:" + e.getKey())
						&& lvl >= e.getValue())
					{
						System.out.println("Found match");
						ChatUtils.message("Found enchantment "
							+ id.substring(10) + " " + lvl);
						EVENTS.remove(PacketInputListener.class, this);
						EVENTS.remove(UpdateListener.class, this);
						
						match = true;
					}
				}
			}
			waiting = false;
			MC.player.networkHandler.sendPacket(
				new CloseHandledScreenC2SPacket(packet.getSyncId()));
			event.cancel();
			if(!match)
			{
				waitForNone = true;
				EVENTS.add(UpdateListener.class, this);
			}
			
		}
		if(event.getPacket() instanceof OpenScreenS2CPacket && waiting)
		{
			OpenScreenS2CPacket packet = (OpenScreenS2CPacket)event.getPacket();
			if(packet.getScreenHandlerType().equals(ScreenHandlerType.MERCHANT))
			{
				event.cancel();
			}
		}
	}
}

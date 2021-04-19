/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;

import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.mixinterface.IContainer;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

public final class RandomCmd extends Command
	implements PacketInputListener, PacketOutputListener, UpdateListener
{
	
	EnchCrackerController cont;
	
	public RandomCmd()
	{
		super("random", "Crackes the player seed to manipulate randomness.",
			".random");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		
		try
		{
			if(args.length == 0 || (args.length >= 1 && args[0].equals("help")))
			{
				if(args.length <= 1)
				{
					ChatUtils.message(".random reset");
					ChatUtils.message(".random anvil");
					ChatUtils.message(".random enchant <enchantments...>");
					ChatUtils.message(".random check");
					ChatUtils.message(".random bookshelves");
					ChatUtils.message(".random drop <type> <amount> [delay]");
					ChatUtils.message(".random dummy <item>");
					ChatUtils.message(".random dropitem <item>");
					ChatUtils.message(".random dropdelay <miliseconds>");
					ChatUtils.message(".random crack");
					ChatUtils.message(".random help <command> - Detailed help");
					ChatUtils.message(
						"When the seed is cracked you can run the other commands, however many things break it such as sprinting, eating or taking damage.");
					
				}else
				{
					switch(args[1])
					{
						case "help":
						ChatUtils.message(
							"help <command> - Get help on a specific sub command.");
						break;
						case "reset":
						ChatUtils.message(
							"reset - Reset the state of this random engine.\n"
								+ "Run this to stop trying to keep track of the random state.");
						break;
						case "anvil":
						ChatUtils.message(
							"anvil - Once cracked, determine if an anvil will break when you use it.\n"
								+ "Will also print how many items you would need to drop to change this.");
						break;
						case "enchant":
						ChatUtils.message(
							"enchant <enchantments...> - Once cracked, manipulate what enchantments you get. \n"
								+ "This command will tell you how many items to drop and how many bookshelves "
								+ "to use to get certain enchantments for the item you are holding. \n"
								+ "Use autoenchant to perform the process automatically.\n"
								+ "Use deepenchant to search further possibly requiring you to drop more items.\n"
								+ "You must have the item you want to enchant, torches in your hotbar, a dummy item and some items to drop to use this.");
						break;
						case "check":
						ChatUtils.message(
							"check - Check if you are in a valid position to run the automatic commands. This will validate the setup for several other commands.");
						break;
						case "bookshelves":
						ChatUtils.message(
							"bookshelves - Automatically block bookshelves to get a specific number for enchanting. You must stand on the enchanting setup to use this.");
						break;
						case "drop":
						ChatUtils.message(
							"drop <type> <amount> [delay] - Drop a lot of items quickly 1 by 1. Dropping items is a way to manipulate randomness.");
						break;
						case "dummy":
						ChatUtils.message(
							"dummy <item> - Set the dummy item to be used when automatically enchanting. Dummy items must be enchantable. Default: wooden_sword");
						break;
						case "dropitem":
						ChatUtils.message(
							"dropitem <item> - Set the item to be used when automatically dropping items. Default: cobblestone");
						break;
						case "dropdelay":
						ChatUtils.message(
							"dropdelay <miliseconds> - Set the delay to be used when automatically dropping items. Default: 0");
						break;
						case "crack":
						ChatUtils.message(
							"crack - Stand on an enchantment table and automatically crack the random seed.\n"
								+ "You must have torches in your hotbar, at least 3 dummy items, 2 lapis and be standing on an enchantment table.");
						break;
						default:
						ChatUtils.message("Invalid subcommand.");
					}
				}
			}
			if(args.length == 1 && args[0].equals("reset"))
			{
				EVENTS.remove(PacketOutputListener.class, this);
				EVENTS.remove(PacketInputListener.class, this);
				new Thread(() -> {
					stop();
				}).start();
				return;
			}
			if(args.length >= 1 && (args[0].equals("anvil")))
			{
				if(cont == null || !cont.found)
				{
					ChatUtils.message("Player seed not yet cracked.");
					return;
				}
				if(cont.willAnvilBreak())
				{
					ChatUtils.message("The next anvil you use will break.");
					ChatUtils.message("Drop " + cont.findAnvilBreak(false)
						+ " to make it not break.");
				}else
				{
					ChatUtils.message("The next anvil you use will not break.");
					ChatUtils.message("Drop " + cont.findAnvilBreak(true)
						+ " to make it break.");
				}
				
			}
			if(args.length >= 1
				&& (args[0].equals("enchant") || args[0].equals("deepenchant")
					|| args[0].equals("autoenchant")
					|| args[0].equals("autodeepenchant")
					|| args[0].equals("deepautoenchant")))
			{
				new Thread(() -> {
					
					if(args.length == 1)
					{
						ChatUtils.message(
							"Usage: .random enchant <enchantments...>");
						ChatUtils.message(
							"Will attempt to find enchantments that match the request for the item you are holding.");
						ChatUtils.message(
							"Example: .random enchant protection -thorns unbreaking:1");
						ChatUtils.message(
							"Example will try to find a set of enchantments with the maximum possible protection, no thorns and unbreaking of at least 1.");
					}else
					{
						if(cont == null || !cont.found)
						{
							ChatUtils.message("Player seed not yet cracked.");
							return;
						}
						
						boolean auto = args[0].contains("auto");
						if(auto)
						{
							if(!checkSetup())
							{
								return;
							}
						}
						String item = Registry.ITEM
							.getId(MC.player.inventory.getMainHandStack()
								.getItem())
							.toString().replace("minecraft:", "");
						ArrayList<Enchantments.EnchantmentInstance> wanted =
							new ArrayList<>();
						ArrayList<Enchantments.EnchantmentInstance> unwanted =
							new ArrayList<>();
						for(int i = 1; i < args.length; i++)
						{
							String arg = args[i];
							String enchantment = arg.replace("-", "");
							int level = -1;
							if(enchantment.contains(":"))
							{
								try
								{
									level =
										Integer.parseInt(enchantment.substring(
											enchantment.indexOf(":") + 1));
									enchantment = enchantment.substring(0,
										enchantment.indexOf(":"));
								}catch(Exception e)
								{
									ChatUtils.message(
										"Invalid enchantment level for " + arg);
									e.printStackTrace();
									return;
								}
							}
							if(!Enchantments.ALL_ENCHANTMENTS
								.contains(enchantment))
							{
								ChatUtils.message("Valid enchantments: "
									+ Enchantments.ALL_ENCHANTMENTS);
								ChatUtils.message(
									"Invalid enchantment " + enchantment);
							}
							Enchantments.EnchantmentInstance ench =
								new Enchantments.EnchantmentInstance(
									enchantment, level);
							if(level == -1)
							{
								ench = cont.getMaxed(enchantment, item);
							}
							if(arg.charAt(0) == '-')
							{
								unwanted.add(ench);
							}else
							{
								wanted.add(ench);
							}
						}
						EnchCrackerController.SearchResults results =
							cont.findEnchantments(wanted, unwanted, item,
								64 * 32 * (args[0].contains("deep") ? 10 : 1));
						if(results.timesNeeded == -2)
						{
							ChatUtils.message("Invalid or not found.");
						}else
						{
							if(auto)
							{
								for(int j = 0; j < results.timesNeeded; j++)
								{
									if(!dropItem(dropItem))
									{
										ChatUtils.message(
											"Ran out of droppable items.");
										return;
									}
									if(dropDelay != 0)
									{
										try
										{
											Thread.sleep(dropDelay);
										}catch(InterruptedException e)
										{
											e.printStackTrace();
										}
									}
								}
								swapToTorch();
								placeBlockLegit(MC.player.getBlockPos().up());
								for(int i = 0; i < 8; i++)
								{
									runOnceWait(() -> {
										
									}, 5);
									if(MC.currentScreen != null
										&& MC.currentScreen instanceof EnchantmentScreen)
									{
										break;
									}
								}
								if(MC.currentScreen != null
									&& MC.currentScreen instanceof EnchantmentScreen)
								{
									if(((IContainer)MC.currentScreen).findSlot(
										item.contains("minecraft:") ? item
											: "minecraft:" + item,
										i -> !i.hasEnchantments()) == -1)
									{
										ChatUtils.message("No target item.");
										return;
									}
									if(results.timesNeeded != -1)
									{
										if(((IContainer)MC.currentScreen)
											.findSlot(
												dummy.contains("minecraft:")
													? dummy
													: "minecraft:" + dummy,
												i -> !i
													.hasEnchantments()) == -1)
										{
											ChatUtils.message("No dummy item.");
											return;
										}
										insertItem(dummy);
										enchant(0);
									}
									setTorches(results.bookshelvesNeeded);
									clearTable();
									insertItem(item);
									enchant(results.slot);
									setTorches(15);
									
								}else
								{
									ChatUtils.message(
										"Unable to open enchantment table.");
									return;
								}
								
							}else
							{
								ChatUtils.message("Item: " + results.item);
								ChatUtils
									.message(results.enchantments.toString());
								ChatUtils.message(
									"Enchantment slot: " + (1 + results.slot));
								ChatUtils.message("Bookshelves: "
									+ results.bookshelvesNeeded);
								if(results.timesNeeded == -1)
								{
									ChatUtils.message("No dummy item.");
								}else
								{
									ChatUtils.message("Drop "
										+ results.timesNeeded + " items. (64x"
										+ results.timesNeeded / 64 + "+"
										+ results.timesNeeded % 64 + ")");
								}
							}
							
						}
						
					}
				}).start();
				
				return;
			}
			if(args.length == 1 && args[0].equals("check"))
			{
				boolean b = checkSetup();
				if(b)
				{
					ChatUtils.message("Setup is valid.");
				}
				return;
			}
			if(args.length == 2
				&& (args[0].equals("set") || args[0].equals("bookshelves")))
			{
				try
				{
					boolean b = checkSetup();
					if(!b)
					{
						return;
					}
					int i = Integer.parseInt(args[1]);
					new Thread(() -> {
						setTorches(i);
					}).start();
				}catch(Exception e)
				{
					
				}
				return;
			}
			if(args[0].equals("drop"))
			{
				if(args.length >= 3)
				{
					try
					{
						int delay =
							args.length > 3 ? Integer.parseInt(args[3]) : 0;
						int i = Integer.parseInt(args[2]);
						new Thread(() -> {
							for(int j = 0; j < i; j++)
							{
								if(!dropItem(args[1]))
								{
									break;
								}
								if(delay != 0)
								{
									try
									{
										Thread.sleep(delay);
									}catch(InterruptedException e)
									{
										e.printStackTrace();
									}
								}
							}
						}).start();
					}catch(Exception e)
					{
						
					}
				}else
				{
					ChatUtils.message(
						"Usage: .random drop <type> <amount> [delay] drops a number of items one by one quickly.");
				}
				return;
			}
			if(args[0].equals("dummy"))
			{
				if(args.length == 2)
				{
					try
					{
						dummy = args[1];
					}catch(Exception e)
					{
						
					}
				}else
				{
					ChatUtils.message(
						"Usage: .random dummy <item> set the dummy item to be used when automatically enchanting.");
					ChatUtils.message("Dummy: " + dummy);
				}
				return;
			}
			if(args[0].equalsIgnoreCase("dropitem"))
			{
				if(args.length == 2)
				{
					try
					{
						dropItem = args[1];
					}catch(Exception e)
					{
						
					}
				}else
				{
					ChatUtils.message(
						"Usage: .random dropitem <item> set the item to be used when automatically dropping items.");
					ChatUtils.message("Drop item: " + dropItem);
				}
				return;
			}
			if(args[0].equalsIgnoreCase("dropdelay"))
			{
				if(args.length == 2)
				{
					try
					{
						dropDelay = Integer.parseInt(args[1]);
					}catch(Exception e)
					{
						
					}
				}else
				{
					ChatUtils.message(
						"Usage: .random dropdelay <miliseconds> set the delay to be used when automatically dropping items.");
					ChatUtils.message("Drop delay: " + dropDelay);
				}
				return;
			}
			if(args.length == 1 && args[0].equals("crack"))
			{
				new Thread(() -> {
					EVENTS.remove(PacketOutputListener.class, this);
					EVENTS.remove(PacketInputListener.class, this);
					stop();
					if(cont == null)
					{
						nextShelfCount = shelfs[index];
						if(!checkSetup())
							return;
						placeBlockLegit(MC.player.getBlockPos().up());
						for(int i = 0; i < 8; i++)
						{
							runOnceWait(() -> {
								
							}, 5);
							if(MC.currentScreen != null
								&& MC.currentScreen instanceof EnchantmentScreen)
							{
								break;
							}
						}
						if(MC.currentScreen != null
							&& MC.currentScreen instanceof EnchantmentScreen)
						{
							if(((IContainer)MC.currentScreen).findSlot(
								dummy.contains("minecraft:") ? dummy
									: "minecraft:" + dummy,
								i -> !i.hasEnchantments()) == -1)
							{
								ChatUtils.message("No dummy item.");
								return;
							}
							insertItem(dummy);
							enchant(0);
							flickerItemInTable();
							cont = new EnchCrackerController();
							ChatUtils.message(
								"Use " + nextShelfCount + " bookshelves.");
							EVENTS.remove(PacketInputListener.class, this);
							EVENTS.add(PacketInputListener.class, this);
							setTorches(nextShelfCount);
						}else
						{
							ChatUtils
								.message("Unable to open enchantment table.");
							return;
						}
						
					}else
					{
						ChatUtils
							.message("Use .random reset to stop the process.");
					}
				}).start();
			}
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean checkSetup()
	{
		// flipTorches = false;
		torchDirection = -1;
		
		BlockPos pos = MC.player.getBlockPos();
		if(!BlockUtils.getName(pos).equals("minecraft:enchanting_table"))
		{
			ChatUtils.message(
				"You must stand on a fully maxed enchantment table to use this command. The table layout should be like this:");
			ChatUtils.message("BBBBB");
			ChatUtils.message("B---B");
			ChatUtils.message("B-E-B");
			ChatUtils.message("B---B");
			ChatUtils.message("BB-BB");
			ChatUtils.message(
				"There must be no more than 15 bookshelves and the 15 must all be in one layer.");
			return false;
		}
		ArrayList<BlockPos> area =
			BlockUtils.getAllInBox(pos.south(2).west(2), pos.north(2).east(2));
		int bookshelves = 0;
		for(BlockPos p : area)
		{
			if(BlockUtils.getName(p).equals("minecraft:enchanting_table"))
			{
				if(!pos.equals(p))
				{
					ChatUtils.message(
						"There may only be one enchanting table in the workspace.");
					return false;
				}
			}else if(BlockUtils.getName(p).equals("minecraft:bookshelf"))
			{
				BlockPos offset = p.subtract(pos);
				if(Math.abs(offset.getZ()) != 2 && Math.abs(offset.getX()) != 2)
				{
					ChatUtils.message("Bookshelves must be in the outer ring.");
					return false;
				}
				bookshelves++;
				if(bookshelves > 15)
				{
					ChatUtils.message(
						"Too many bookshelves. There must be exactly 15 bookshelves.");
					return false;
				}
			}else if(BlockUtils.getName(p).equals("minecraft:air"))
			{
				BlockPos offset = p.subtract(pos);
				if(Math.abs(offset.getX()) == 2 && Math.abs(offset.getZ()) != 0)
				{
					ChatUtils.message("Air gap must be centered on a side.");
					return false;
				}
				if(Math.abs(offset.getZ()) == 2 && Math.abs(offset.getX()) != 0)
				{
					ChatUtils.message("Air gap must be centered on a side.");
					return false;
				}
				if(p.equals(pos.east().east()))
				{
					torchDirection = 1;
					// flipTorches = true;
				}
				if(p.equals(pos.west().west()))
				{
					torchDirection = 0;
				}
				if(p.equals(pos.north().north()))
				{
					torchDirection = 2;
				}
				if(p.equals(pos.south().south()))
				{
					torchDirection = 3;
				}
			}else
			{
				if(BlockUtils.getHardness(pos) >= 1)
				{
					ChatUtils.message("Hard block in core.");
					return false;
				}
			}
			
		}
		if(bookshelves < 15)
		{
			ChatUtils.message(
				"Too few bookshelves. There must be exactly 15 bookshelves.");
			return false;
		}
		if(torchDirection == -1)
		{
			ChatUtils.message("Could not find exterior air gap.");
			return false;
		}
		sides = torches();
		return true;
	}
	
	public void nextShelves()
	{
		index++;
		nextShelfCount = shelfs[index];
		ChatUtils.message("Use " + nextShelfCount + " bookshelves.");
		setTorches(nextShelfCount);
		flickerItemInTable();
	}
	
	public void setTorches(int i)
	{
		if(!checkSetup())
			return;
		switch(i)
		{
			case 15:
			setTorches(false, false, false, false, false, false, false);
			break;
			case 14:
			setTorches(true, false, false, false, false, false, false);
			break;
			case 13:
			setTorches(true, true, false, false, false, false, false);
			break;
			case 12:
			setTorches(true, true, true, false, false, false, false);
			break;
			case 11:
			setTorches(true, false, false, true, false, false, false);
			break;
			case 10:
			setTorches(true, true, false, true, false, false, false);
			break;
			case 9:
			setTorches(false, false, false, true, true, false, false);
			break;
			case 8:
			setTorches(true, false, false, true, true, false, false);
			break;
			case 7:
			setTorches(true, true, false, true, true, false, false);
			break;
			case 6:
			setTorches(true, true, true, true, true, false, false);
			break;
			case 5:
			setTorches(true, false, false, true, true, true, false);
			break;
			case 4:
			setTorches(true, true, false, true, true, true, false);
			break;
			case 3:
			setTorches(true, true, true, true, true, true, false);
			case 2:
			setTorches(true, false, false, true, true, true, true);
			case 1:
			setTorches(true, true, false, true, true, true, true);
			case 0:
			setTorches(true, true, true, true, true, true, true);
			break;
		}
	}
	
	public void setTorches(boolean... values)
	{
		for(int i = 0; i < Math.min(6, values.length - 1); i++)
		{
			setTorch(i + 1, values[i]);
		}
	}
	
	public BlockPos[] torches()
	{
		BlockPos[] torches = new BlockPos[4];
		torches[0] = MC.player.getBlockPos().west();
		torches[1] = MC.player.getBlockPos().east();
		torches[2] = MC.player.getBlockPos().north();
		torches[3] = MC.player.getBlockPos().south();
		if(torchDirection != 3)
		{
			torches[torchDirection] = torches[3];
		}
		return torches;
	}
	
	public void setTorch(int i, boolean shouldBe)
	{
		BlockPos pos = null;
		switch(i)
		{
			case 1:
			pos = (sides[0]);
			break;
			case 2:
			pos = (sides[1]);
			break;
			case 3:
			pos = (sides[2]);
			break;
			case 4:
			pos = (WurstClient.MC.player.getBlockPos().north().east());
			break;
			case 5:
			pos = (WurstClient.MC.player.getBlockPos().south().east());
			break;
			case 6:
			pos = (WurstClient.MC.player.getBlockPos().north().west());
			break;
			case 7:
		}
		boolean exists = BlockUtils.getName(pos).contains("torch");
		if(exists != shouldBe)
		{
			if(shouldBe)
			{
				swapToTorch();
				placeBlockLegit(pos);
			}else
			{
				breakBlockExtraLegit(pos);
				runOnceWait(() -> {
				}, 2);
			}
		}
	}
	
	public void swapToTorch()
	{
		// search blocks in hotbar
		int newSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// filter out non-block items
			ItemStack stack = MC.player.inventory.getStack(i);
			if(stack.isEmpty() || !Registry.ITEM.getId(stack.getItem())
				.toString().equals("minecraft:torch"))
				continue;
			
			newSlot = i;
			break;
		}
		
		// check if any blocks were found
		if(newSlot == -1)
		{
			ChatUtils.message("No torches in hotbar.");
			return;
		}
		
		MC.player.inventory.selectedSlot = newSlot;
	}
	
	public void restart()
	{
		index = 0;
		nextShelfCount = shelfs[index];
		if(cont != null)
			ChatUtils.message("Use " + nextShelfCount + " bookshelves.");
		WurstClient.INSTANCE.getEventManager().remove(UpdateListener.class,
			this);
		WurstClient.INSTANCE.getEventManager().add(UpdateListener.class, this);
		setTorches(15);
	}
	
	public void enchant(int slot)
	{
		runOnceWait(() -> {
			if(MC.currentScreen != null
				&& MC.currentScreen instanceof EnchantmentScreen)
			{
				IContainer container2 = (IContainer)MC.currentScreen;
				int find = container2.findSlot("minecraft:lapis_lazuli");
				if(find != -1)
					container2.click(find, SlotActionType.QUICK_MOVE);
				
			}
		}, 10);
		if(MC.currentScreen instanceof EnchantmentScreen)
		{
			IContainer container = (IContainer)MC.currentScreen;
			ButtonClickC2SPacket packet =
				new ButtonClickC2SPacket(container.getSyncId(), slot);
			MC.player.networkHandler.sendPacket(packet);
			runOnceWait(() -> {
				if(MC.currentScreen != null
					&& MC.currentScreen instanceof EnchantmentScreen)
				{
					IContainer container2 = (IContainer)MC.currentScreen;
					int find = container2.findSlot("minecraft:lapis_lazuli");
					if(find != -1)
						container2.click(find, SlotActionType.QUICK_MOVE);
					
				}
			}, 10);
		}
	}
	
	String dummy = "wooden_sword";
	String dropItem = "cobblestone";
	
	int torchDirection = 0;
	int dropDelay = 0;
	BlockPos[] sides;
	// boolean flipTorches = false;
	int nextShelfCount = 0;
	int last0 = 0;
	int last1 = 0;
	int last2 = 0;
	int index = 0;
	int[] shelfs = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3};
	AtomicBoolean breakingBlocks = new AtomicBoolean();
	
	public void dropItems(String name, int amount)
	{
		for(int j = 0; j < amount; j++)
		{
			dropItem(name);
		}
	}
	
	private boolean dropItem(String name)
	{
		for(int i = 9; i < 45; i++)
			if(Registry.ITEM.getId(MC.player.inventory.getStack(i).getItem())
				.toString().replace("minecraft:", "").equals(name))
			{
				IMC.getInteractionManager().windowClick_THROW1(i);
				// if(cont != null)
				// {
				// cont.itemDropped();
				// }
				return true;
			}
		for(int i = 0; i < 9; i++)
			if(Registry.ITEM.getId(MC.player.inventory.getStack(i).getItem())
				.toString().replace("minecraft:", "").equals(name))
			{
				IMC.getInteractionManager().windowClick_THROW1(i + 36);
				// if(cont != null)
				// {
				// cont.itemDropped();
				// }
				return true;
			}
		return false;
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(event.getPacket() instanceof ScreenHandlerPropertyUpdateS2CPacket)
		{
			if(cont == null || cont.found)
			{
				
			}else
			{
				ScreenHandlerPropertyUpdateS2CPacket packet =
					(ScreenHandlerPropertyUpdateS2CPacket)event.getPacket();
				if(packet.getPropertyId() == 0)
				{
					last0 = packet.getValue();
				}
				if(packet.getPropertyId() == 1)
				{
					last1 = packet.getValue();
				}
				if(packet.getPropertyId() == 2)
				{
					last2 = packet.getValue();
					if(last2 != 0)
					{
						
						if(cont != null && cont.singleSeedCracker != null
							&& !cont.singleSeedCracker.isRunning())
						{
							ChatUtils
								.message(last0 + " " + last1 + " " + last2);
							cont.crack(nextShelfCount, last0, last1, last2);
						}
					}else
					{
						if(0 == last1 && 0 == last2)
						{
						}else
						{
							ChatUtils.message(
								"Invalid enchanting result, try again with a different number of shelves.");
						}
					}
				}
			}
		}
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
			PlayerMoveC2SPacket.LookOnly packet =
				new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
					rotation.getPitch(), MC.player.isOnGround());
			MC.player.networkHandler.sendPacket(packet);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			
			return true;
		}
		
		return false;
	}
	
	private boolean breakBlockExtraLegit(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		if(BlockUtils.getName(pos).equals("minecraft:air"))
		{
			return false;
		}
		
		for(Direction side : Direction.values())
		{
			Vec3d hitVec = posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
			double distanceSqHitVec = eyesPos.squaredDistanceTo(hitVec);
			
			// check if hitVec is within range (4.25 blocks)
			if(distanceSqHitVec > 18.0625)
				continue;
			
			// check if side is facing towards player
			if(distanceSqHitVec >= distanceSqPosVec)
				continue;
			
			// check line of sight
			if(MC.world
				.raycast(new RaycastContext(eyesPos, hitVec,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			// face block
			WURST.getRotationFaker().faceVectorClient(hitVec);
			ClientPlayNetworkHandler netHandler = MC.player.networkHandler;
			
			netHandler.sendPacket(new PlayerActionC2SPacket(
				Action.START_DESTROY_BLOCK, pos, side));
			netHandler.sendPacket(new PlayerActionC2SPacket(
				Action.STOP_DESTROY_BLOCK, pos, side));
			
			return true;
		}
		
		return false;
	}
	
	public void flickerItemInTable()
	{
		runOnce(() -> {
			if(breakingBlocks.get())
			{
				runOnce(this::flickerItemInTable, 2);
			}
			if(MC.currentScreen != null
				&& MC.currentScreen instanceof EnchantmentScreen)
			{
				IContainer container = (IContainer)MC.currentScreen;
				container.click(0, SlotActionType.QUICK_MOVE);
				
			}
			runOnce(() -> {
				if(MC.currentScreen != null
					&& MC.currentScreen instanceof EnchantmentScreen)
				{
					IContainer container = (IContainer)MC.currentScreen;
					int slot =
						container.findSlot(
							dummy.contains("minecraft:") ? dummy
								: "minecraft:" + dummy,
							i -> !i.hasEnchantments());
					if(slot != -1)
						container.click(slot, SlotActionType.QUICK_MOVE);
					
				}
				
			}, 10);
		}, 10);
		
	}
	
	public void clearTable()
	{
		runOnceWait(() -> {
			if(MC.currentScreen != null
				&& MC.currentScreen instanceof EnchantmentScreen)
			{
				IContainer container = (IContainer)MC.currentScreen;
				container.click(0, SlotActionType.QUICK_MOVE);
				
			}
		}, 10);
		
	}
	
	public void insertDummyItem()
	{
	}
	
	public void insertItem(String name)
	{
		runOnceWait(() -> {
			if(MC.currentScreen != null
				&& MC.currentScreen instanceof EnchantmentScreen)
			{
				IContainer container = (IContainer)MC.currentScreen;
				int index = container.findSlot(
					name.contains("minecraft:") ? name : "minecraft:" + name,
					i -> !i.hasEnchantments());
				if(index != -1)
					container.click(index, SlotActionType.QUICK_MOVE);
				
			}
			
		}, 10);
		
	}
	
	public void runOnce(UpdateListener l, int delay)
	{
		
		AtomicInteger runs = new AtomicInteger(delay);
		WURST.getEventManager().add(UpdateListener.class, new UpdateListener()
		{
			
			public void onUpdate()
			{
				if(runs.getAndDecrement() == 0)
				{
					l.onUpdate();
					WURST.getEventManager().remove(UpdateListener.class, this);
				}
			}
			
		});
	}
	
	public void runOnceWait(UpdateListener l, int delay)
	{
		AtomicInteger runs = new AtomicInteger(delay);
		AtomicBoolean done = new AtomicBoolean();
		WURST.getEventManager().add(UpdateListener.class, new UpdateListener()
		{
			
			public void onUpdate()
			{
				if(runs.getAndDecrement() == 0)
				{
					l.onUpdate();
					WURST.getEventManager().remove(UpdateListener.class, this);
					done.set(true);
				}
			}
			
		});
		while(!done.get())
		{
			try
			{
				Thread.sleep(1);
			}catch(InterruptedException e)
			{
			}
		}
	}
	
	@Override
	public void onUpdate()
	{
		
	}
	
	public void stop()
	{
		cont = null;
		index = 0;
		nextShelfCount = shelfs[index];
		WurstClient.INSTANCE.getEventManager().remove(UpdateListener.class,
			this);
		WurstClient.INSTANCE.getEventManager().remove(PacketInputListener.class,
			this);
		WurstClient.INSTANCE.getEventManager()
			.remove(PacketOutputListener.class, this);
		setTorches(15);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof CreativeInventoryActionC2SPacket)
		{
			CreativeInventoryActionC2SPacket packet =
				(CreativeInventoryActionC2SPacket)event.getPacket();
			if(packet.getSlot() == -1)
			{
				if(cont != null)
				{
					cont.itemDropped();
				}
			}
			
		}
		if(event.getPacket() instanceof ClickSlotC2SPacket)
		{
			ClickSlotC2SPacket packet = (ClickSlotC2SPacket)event.getPacket();
			if(packet.getActionType().equals(SlotActionType.THROW))
			{
				if(packet.getSlot() != -999)
					if(cont != null)
					{
						cont.itemDropped();
					}
			}
			if(packet.getActionType().equals(SlotActionType.PICKUP))
			{
				if(packet.getSlot() == -999)
					if(cont != null)
					{
						cont.itemDropped();
					}
				if(MC.currentScreen != null
					&& MC.currentScreen instanceof AnvilScreen)
				{
					if(packet.getSlot() == 2 && !packet.getStack().isEmpty())
					{
						if(cont != null)
						{
							cont.floatChange();
						}
					}
				}
			}
			if(packet.getActionType().equals(SlotActionType.QUICK_MOVE))
			{
				if(MC.currentScreen != null
					&& MC.currentScreen instanceof AnvilScreen)
				{
					if(packet.getSlot() == 2
						&& ((IContainer)MC.currentScreen).getHandler().slots
							.get(0).getStack().isEmpty())
					{
						if(cont != null)
						{
							cont.floatChange();
						}
					}
				}
			}
			
		}
		if(event.getPacket() instanceof PlayerActionC2SPacket)
		{
			PlayerActionC2SPacket packet =
				(PlayerActionC2SPacket)event.getPacket();
			System.out.println(event.getPacket());
			System.out.println(packet.getAction());
			if(packet.getAction().equals(Action.DROP_ITEM)
				|| packet.getAction().equals(Action.DROP_ALL_ITEMS))
			{
				if(!MC.player.getMainHandStack().isEmpty())
				{
					if(cont != null)
					{
						cont.itemDropped();
					}
				}
			}
		}
		if(event.getPacket() instanceof ButtonClickC2SPacket)
		{
			System.out.println(event.getPacket());
			if(MC.currentScreen != null
				&& MC.currentScreen instanceof EnchantmentScreen)
			{
				if(cont != null)
				{
					cont.itemEnchanted();
				}
			}
		}
	}
	
}

/**
 * Enchantment cracker by Earthcomputer
 * https://github.com/Earthcomputer/EnchantmentCracker
 * Minor changes as necessary to fit it into Wurst
 */
class EnchCrackerController
{
	JavaSingleSeedCracker singleSeedCracker = new JavaSingleSeedCracker();
	long seed1 = -1;
	long seed2 = -1;
	long seed = -1;
	boolean found = false;
	Random random = new Random();
	
	public void reset()
	{
		singleSeedCracker.abortAndThen(() -> {
			singleSeedCracker.resetCracker();
		});
	}
	
	public void computeSeed()
	{
		long seed1High = ((long)seed1 << 16) & 0x0000_ffff_ffff_0000L;
		long seed2High = ((long)seed2 << 16) & 0x0000_ffff_ffff_0000L;
		found = false;
		for(int seed1Low = 0; seed1Low < 65536; seed1Low++)
		{
			if((((seed1High | seed1Low) * 0x5deece66dL + 0xb)
				& 0x0000_ffff_ffff_0000L) == seed2High)
			{
				seed = ((seed1High | seed1Low) * 0x5deece66dL + 0xb)
					& 0x0000_ffff_ffff_ffffL;
				ChatUtils.message(
					"Player seed calculated as " + String.format("%12X", seed));
				found = true;
				break;
			}
		}
		if(found)
		{
			random.setSeed(seed);
		}else
		{
			ChatUtils.message("No player seed found.");
			WurstClient.INSTANCE.getCmds().randomCmd.stop();
		}
		WurstClient.INSTANCE.getCmds().randomCmd.setTorches(15);
		WurstClient.INSTANCE.getEventManager().remove(
			PacketOutputListener.class,
			WurstClient.INSTANCE.getCmds().randomCmd);
		WurstClient.INSTANCE.getEventManager().add(PacketOutputListener.class,
			WurstClient.INSTANCE.getCmds().randomCmd);
	}
	
	public void crack(int bookshelves, int slot1, int slot2, int slot3)
	{
		if(singleSeedCracker.getPossibleSeeds() == 1 && bookshelves == 15)
			return;
		System.out.println("Books: " + bookshelves + " 1: " + slot1 + " 2: "
			+ slot2 + " 3: " + slot3);
		singleSeedCracker.abortAndThen(() -> {
			// First time is different because otherwise we have to store all
			// 2^32 initial seeds
			boolean firstTime = singleSeedCracker.isFirstTime();
			singleSeedCracker.setFirstTime(false);
			
			// Start brute-forcing thread
			Thread thread;
			if(firstTime)
			{
				thread = new Thread(() -> {
					singleSeedCracker.firstInput(bookshelves, slot1, slot2,
						slot3);
					
					int possibleSeeds = singleSeedCracker.getPossibleSeeds();
					try
					{
						
						ChatUtils.message(
							"Reduced possible seeds to " + possibleSeeds);
					}catch(Exception e)
					{
						
					}
					singleSeedCracker.setRunning(false);
					switch(possibleSeeds)
					{
						case 0:
						ChatUtils.message("No possible seeds.");
						break;
						case 1:
						ChatUtils.message(String.format("XP seed: %08X",
							singleSeedCracker.getSeed()));
						if(seed1 == -1)
						{
							seed1 = singleSeedCracker.getSeed();
							ChatUtils.message("Enchant and repeat at 15.");
							
							WurstClient.INSTANCE.getCmds().randomCmd.restart();
							WurstClient.INSTANCE.getCmds().randomCmd.enchant(0);
							WurstClient.INSTANCE.getCmds().randomCmd
								.flickerItemInTable();
							singleSeedCracker = new JavaSingleSeedCracker();
						}else
						{
							seed2 = singleSeedCracker.getSeed();
							computeSeed();
							
							WurstClient.INSTANCE.getCmds().randomCmd.index = 0;
						}
						break;
						default:
						ChatUtils.message(possibleSeeds + " Remaining seeds");
						WurstClient.INSTANCE.getCmds().randomCmd.nextShelves();
						break;
					}
				});
			}else
			{
				thread = new Thread(() -> {
					singleSeedCracker.addInput(bookshelves, slot1, slot2,
						slot3);
					int possibleSeeds = singleSeedCracker.getPossibleSeeds();
					try
					{
						ChatUtils.message(
							"Reduced possible seeds to " + possibleSeeds);
					}catch(Exception e)
					{
						
					}
					singleSeedCracker.setRunning(false);
					switch(possibleSeeds)
					{
						case 0:
						ChatUtils.message("No possible seeds.");
						
						break;
						case 1:
						ChatUtils.message(String.format("XP seed: %08X",
							singleSeedCracker.getSeed()));
						if(seed1 == -1)
						{
							seed1 = singleSeedCracker.getSeed();
							ChatUtils.message("Enchant and repeat at 15.");
							WurstClient.INSTANCE.getCmds().randomCmd.restart();
							WurstClient.INSTANCE.getCmds().randomCmd.enchant(0);
							WurstClient.INSTANCE.getCmds().randomCmd
								.flickerItemInTable();
							singleSeedCracker = new JavaSingleSeedCracker();
						}else
						{
							seed2 = singleSeedCracker.getSeed();
							computeSeed();
							
							WurstClient.INSTANCE.getCmds().randomCmd.index = 0;
							
						}
						break;
						default:
						ChatUtils.message(possibleSeeds + " Remaining seeds");
						WurstClient.INSTANCE.getCmds().randomCmd.nextShelves();
						break;
					}
				});
			}
			thread.setDaemon(true);
			singleSeedCracker.setRunning(true);
			thread.start();
			
			// Start progress bar thread
			if(firstTime)
			{
				thread = new Thread(() -> {
					while(singleSeedCracker.isRunning())
					{
						ChatUtils.message(singleSeedCracker.getSeedsSearched()
							+ " / " + 4294967296l);
						try
						{
							Thread.sleep(500);
						}catch(InterruptedException e)
						{
							Thread.currentThread().interrupt();
						}
					}
				});
			}else
			{
				thread = new Thread(() -> {
					while(singleSeedCracker.isRunning())
					{
						// need this check, as it's possible this line might be
						// hit before seedsSearched is set back to 0
						if(singleSeedCracker
							.getSeedsSearched() <= singleSeedCracker
								.getPossibleSeeds())
						{
							ChatUtils.message(
								singleSeedCracker.getSeedsSearched() + " / "
									+ singleSeedCracker.getPossibleSeeds());
							
						}
						try
						{
							Thread.sleep(500);
						}catch(InterruptedException e)
						{
							Thread.currentThread().interrupt();
						}
					}
				});
			}
			thread.setDaemon(true);
			thread.start();
		});
	}
	
	public SearchResults findEnchantments(
		ArrayList<Enchantments.EnchantmentInstance> wantedEnch,
		ArrayList<Enchantments.EnchantmentInstance> unwantedEnch,
		String itemToEnch, int depth)
	{
		int timesNeeded = -2;
		int bookshelvesNeeded = 0;
		int slot = 0;
		long seed = this.seed;
		int[] enchantLevels = new int[3];
		List<Enchantments.EnchantmentInstance> enchantments = null;
		System.out.println(wantedEnch);
		System.out.println(unwantedEnch);
		
		outerLoop: for(int i = -1; i <= depth; i++)
		{
			int xpSeed;
			if(i == -1)
			{
				// XP seed will be the current seed, because there is no dummy
				// enchant
				xpSeed = (int)(seed >>> 16);
			}else
			{
				// XP seed will be the current seed, advanced by one because of
				// the dummy enchant
				xpSeed = (int)(((seed * 0x5deece66dL + 0xb)
					& 0x0000_ffff_ffff_ffffL) >>> 16);
			}
			
			Random rand = new Random();
			for(bookshelvesNeeded =
				0; bookshelvesNeeded <= 15; bookshelvesNeeded++)
			{
				rand.setSeed(xpSeed);
				// Calculate all slot levels
				// Important they're done in a row like this because RNG is not
				// reset in between
				for(slot = 0; slot < 3; slot++)
				{
					int level = Enchantments.calcEnchantmentTableLevel(rand,
						slot, bookshelvesNeeded, itemToEnch);
					if(level < slot + 1)
					{
						level = 0;
					}
					enchantLevels[slot] = level;
				}
				
				slotLoop: for(slot = 0; slot < 3; slot++)
				{
					// Get enchantments (changes RNG seed)
					enchantments = Enchantments.getEnchantmentsInTable(rand,
						xpSeed, itemToEnch, slot, enchantLevels[slot],
						Versions.V1_16);
					
					if(enchantLevels[slot] == 0)
					{
						continue slotLoop;
					}else if(i == -1)
					{
						if(999 < enchantLevels[slot])
						{
							continue slotLoop;
						}
					}else if(999 < (enchantLevels[slot] + 1))
					{
						continue slotLoop;
					}
					
					// Does this list contain all the enchantments we want?
					for(Enchantments.EnchantmentInstance inst : wantedEnch)
					{
						boolean found = false;
						for(Enchantments.EnchantmentInstance inst2 : enchantments)
						{
							if(!inst.enchantment.equals(inst2.enchantment))
								continue;
							if(inst.level > inst2.level)
								continue slotLoop;
							found = true;
							break;
						}
						if(!found)
							continue slotLoop;
					}
					
					// Does this list contain none of the enchantments we don't
					// want?
					for(Enchantments.EnchantmentInstance inst : unwantedEnch)
					{
						for(Enchantments.EnchantmentInstance inst2 : enchantments)
						{
							if(!inst.enchantment.equals(inst2.enchantment))
								continue;
							continue slotLoop;
						}
					}
					
					timesNeeded = i;
					break outerLoop;
				}
			}
			
			// Simulate an item throw
			if(i != -1)
			{
				for(int j = 0; j < 4; j++)
				{
					seed = (seed * 0x5deece66dL + 0xb) & 0x0000_ffff_ffff_ffffL;
				}
			}
		}
		SearchResults results = new SearchResults();
		results.bookshelvesNeeded = bookshelvesNeeded;
		results.enchantments = enchantments;
		results.item = itemToEnch;
		results.slot = slot;
		results.timesNeeded = timesNeeded;
		return results;
	}
	
	public Enchantments.EnchantmentInstance getMaxed(String enchantment,
		String item)
	{
		return new Enchantments.EnchantmentInstance(enchantment,
			Enchantments.getMaxLevelInTable(enchantment, item));
	}
	
	static class SearchResults
	{
		int timesNeeded;
		String item;
		List<Enchantments.EnchantmentInstance> enchantments;
		int bookshelvesNeeded = 0;
		int slot = 0;
	}
	
	public void itemDropped()
	{
		for(int j = 0; j < 4; j++)
		{
			seed = (seed * 0x5deece66dL + 0xb) & 0x0000_ffff_ffff_ffffL;
		}
	}
	
	public void itemEnchanted()
	{
		seed = (seed * 0x5deece66dL + 0xb) & 0x0000_ffff_ffff_ffffL;
	}
	
	// thorns, cross bow fire, soul speed activate, unbreaking trigger, anvil
	// item removed
	public void floatChange()
	{
		seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
	}
	
	public void doubleChange()
	{
		seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
		seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
	}
	
	public void chorusFruit()
	{
		doubleChange();
		floatChange();
		doubleChange();
	}
	
	public void givenItem()
	{
		floatChange();
		floatChange();
	}
	
	public Vec3d chorusPosition()
	{
		ClientPlayerEntity p = WurstClient.MC.player;
		Random r = new Random();
		r.setSeed(seed ^ 0x5DEECE66DL);
		double g = p.getX() + (r.nextDouble() - 0.5D) * 16.0D;
		double h = MathHelper.clamp(p.getY() + (double)(r.nextInt(16) - 8),
			0.0D, (double)(WurstClient.MC.world.getDimensionHeight() - 1));
		double j = p.getZ() + (r.nextDouble() - 0.5D) * 16.0D;
		return new Vec3d(g, h, j);
	}
	
	public boolean willAnvilBreak()
	{
		random.setSeed(seed ^ 0x5DEECE66DL);
		return random.nextFloat() < 0.12F;
	}
	
	public int findAnvilBreak(boolean willBreak)
	{
		long seed = this.seed;
		random.setSeed(seed ^ 0x5DEECE66DL);
		if(random.nextFloat() < 0.12F == willBreak)
		{
			return 0;
		}
		for(int i = 0; i < 128; i++)
		{
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			random.setSeed(seed ^ 0x5DEECE66DL);
			if(random.nextFloat() < 0.12F == willBreak)
			{
				return i + 1;
			}
		}
		return -1;
	}
	
}

class JavaSingleSeedCracker
{
	private final IntArray possibleSeeds = new IntArray(true);
	private final AtomicLong seedsSearched = new AtomicLong(0);
	private final AtomicBoolean abortRequested = new AtomicBoolean(false);
	private boolean firstTime = true;
	private AtomicBoolean running = new AtomicBoolean(false);
	
	public void setRunning(boolean running)
	{
		this.running.set(running);
	}
	
	public boolean isRunning()
	{
		return running.get();
	}
	
	public void abortAndThen(Runnable r)
	{
		if(isRunning())
		{
			if(!isAbortRequested())
			{
				requestAbort();
			}
			while(isRunning())
			{
				try
				{
					Thread.sleep(100);
				}catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
			firstTime = true;
		}
		r.run();
	}
	
	public void setFirstTime(boolean firstTime)
	{
		this.firstTime = firstTime;
	}
	
	public boolean isFirstTime()
	{
		return firstTime;
	}
	
	// Level generators
	private static int getGenericEnchantability(SimpleRandom rand,
		int bookshelves)
	{
		int first = rand.nextInt(8);
		int second = rand.nextInt(bookshelves + 1);
		return first + 1 + (bookshelves >> 1) + second;
	}
	
	private static int getLevelsSlot1(SimpleRandom rand, int bookshelves)
	{
		int enchantability = getGenericEnchantability(rand, bookshelves) / 3;
		return enchantability < 1 ? 1 : enchantability;
	}
	
	private static int getLevelsSlot2(SimpleRandom rand, int bookshelves)
	{
		return getGenericEnchantability(rand, bookshelves) * 2 / 3 + 1;
	}
	
	private static int getLevelsSlot3(SimpleRandom rand, int bookshelves)
	{
		int enchantability = getGenericEnchantability(rand, bookshelves);
		int twiceBookshelves = bookshelves * 2;
		return enchantability < twiceBookshelves ? twiceBookshelves
			: enchantability;
	}
	
	public void resetCracker()
	{
		abortRequested.set(true);
		setFirstTime(true);
		possibleSeeds.clear();
	}
	
	public void firstInput(int bookshelves, int slot1, int slot2, int slot3)
	{
		abortRequested.set(false);
		final int threadCount = Runtime.getRuntime().availableProcessors() - 1; // always
																				// leave
																				// one
																				// for
																				// OS
		final int blockSize = Integer.MAX_VALUE / 20 / threadCount - 1;
		final AtomicInteger seed = new AtomicInteger(Integer.MIN_VALUE);
		ArrayList<Thread> threads = new ArrayList<>();
		
		final int twoShelves = bookshelves * 2;
		final int halfShelves = bookshelves / 2 + 1;
		final int shelvesPlusOne = bookshelves + 1;
		
		final int firstEarly = slot1 * 3 + 2;
		final int secondEarly = slot2 * 3 / 2;
		final int secondSubOne = slot2 - 1;
		
		seedsSearched.set(0);
		possibleSeeds.clear();
		
		System.out.println("Cracking first input using " + (threadCount + 1)
			+ " threads (including main thread)");
		long startTime = System.nanoTime();
		
		for(int i = 0; i < threadCount; i++)
		{
			Thread t = new Thread(() -> {
				final int[] myList = new int[1000000];
				int pos = 0;
				final SimpleRandom myRNG = new SimpleRandom();
				
				while(true)
				{
					if(abortRequested.get())
						return;
					
					int curSeed = seed.get();
					final int last = curSeed + blockSize;
					if(last < curSeed)
						break; // overflow
					if(seed.compareAndSet(curSeed, curSeed + blockSize))
					{
						for(; curSeed < last; curSeed++)
						{
							myRNG.setSeed(curSeed);
							
							int ench1r1 = myRNG.nextInt(8) + halfShelves;
							if(ench1r1 > firstEarly)
								continue;
							int ench1 =
								(ench1r1 + myRNG.nextInt(shelvesPlusOne)) / 3;
							if(ench1 < 1)
							{
								if(slot1 != 1)
									continue;
							}
							if(ench1 != slot1)
								continue;
							
							int ench2r1 = myRNG.nextInt(8) + halfShelves;
							if(ench2r1 > secondEarly)
								continue;
							int ench2 =
								(ench2r1 + myRNG.nextInt(shelvesPlusOne)) * 2
									/ 3;
							if(ench2 != secondSubOne)
								continue;
							
							int ench3 = (myRNG.nextInt(8) + halfShelves
								+ myRNG.nextInt(shelvesPlusOne));
							if(Math.max(ench3, twoShelves) != slot3)
								continue;
							
							myList[pos++] = curSeed;
							if(pos == myList.length)
							{
								synchronized(possibleSeeds)
								{
									possibleSeeds.addAll(myList, myList.length);
								}
								pos = 0;
							}
						}
					}
				}
				synchronized(possibleSeeds)
				{
					possibleSeeds.addAll(myList, pos);
				}
			});
			threads.add(t);
			t.start();
		}
		
		while(true)
		{
			if(abortRequested.get())
			{
				while(threads.size() > 0)
				{
					try
					{
						threads.remove(0).join();
					}catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
				abortRequested.set(false);
				return;
			}
			int cur = seed.get();
			if(cur + blockSize < cur)
				break;
			seedsSearched.set((long)cur - (long)Integer.MIN_VALUE);
			try
			{
				Thread.sleep(1);
			}catch(InterruptedException ignored)
			{
			}
		}
		while(threads.size() > 0)
		{
			try
			{
				threads.remove(0).join();
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		SimpleRandom myRNG = new SimpleRandom();
		int[] lastFew = new int[blockSize];
		int lastPos = 0;
		for(int s = seed.get(); s != Integer.MIN_VALUE; s++)
		{
			myRNG.setSeed(s);
			if(getLevelsSlot1(myRNG, bookshelves) == slot1)
			{
				if(getLevelsSlot2(myRNG, bookshelves) == slot2)
				{
					if(getLevelsSlot3(myRNG, bookshelves) == slot3)
					{
						lastFew[lastPos++] = s;
					}
				}
			}
		}
		
		possibleSeeds.addAll(lastFew, lastPos);
		abortRequested.set(false);
		
		System.out.println("Done in "
			+ ((double)((System.nanoTime() - startTime) / 10000000) / 100.0)
			+ " seconds");
	}
	
	public void addInput(int bookshelves, int slot1, int slot2, int slot3)
	{
		SimpleRandom rand = new SimpleRandom();
		IntArray nextPossibleSeeds = new IntArray();
		seedsSearched.set(0);
		
		for(int i = 0, e = possibleSeeds.size(); i < e; i++)
		{
			// Occasionally update seeds searched for GUI
			if(i % 250000 == 0)
			{
				if(abortRequested.get())
				{
					abortRequested.set(false);
					return;
				}
				seedsSearched.set(i);
			}
			
			// Test the seed with the new information
			int s = possibleSeeds.get(i);
			rand.setSeed(s);
			if(getLevelsSlot1(rand, bookshelves) == slot1)
			{
				if(getLevelsSlot2(rand, bookshelves) == slot2)
				{
					if(getLevelsSlot3(rand, bookshelves) == slot3)
					{
						nextPossibleSeeds.add(s);
					}
				}
			}
		}
		possibleSeeds.clear();
		possibleSeeds.addAll(nextPossibleSeeds);
	}
	
	public int getPossibleSeeds()
	{
		return possibleSeeds.size();
	}
	
	public int getSeed()
	{
		return possibleSeeds.get(0);
	}
	
	public void requestAbort()
	{
		abortRequested.set(true);
	}
	
	public boolean isAbortRequested()
	{
		return abortRequested.get();
	}
	
	public long getSeedsSearched()
	{
		return seedsSearched.get();
	}
	
}

class IntArray
{
	// IntArray that consists of pooled lists to avoid memory hogging when
	// copying to a new array
	// The old implementation that used a single large array effectively needed
	// double the array size for any allocation
	
	private static final int BLOCKSIZE = 1000000;
	
	private int[][] lists;
	private int size;
	
	private void addToList(int[] toAdd, int start, int len)
	{
		int curBlock = size / BLOCKSIZE;
		int avail = BLOCKSIZE - (size % BLOCKSIZE);
		int addPos = start;
		len += start;
		while(addPos < len)
		{
			int rem = len - addPos;
			if(rem <= avail)
			{
				System.arraycopy(toAdd, addPos, lists[curBlock],
					size % BLOCKSIZE, rem);
				size += rem;
				if(size % BLOCKSIZE == 0 && lists[curBlock + 1] == null)
					lists[curBlock + 1] = new int[BLOCKSIZE];
				addPos += rem;
			}else
			{
				System.arraycopy(toAdd, addPos, lists[curBlock],
					size % BLOCKSIZE, avail);
				curBlock++;
				if(lists[curBlock] == null)
					lists[curBlock] = new int[BLOCKSIZE];
				addPos += avail;
				size += avail;
				avail = BLOCKSIZE;
			}
		}
	}
	
	public IntArray()
	{
		this(false);
	}
	
	public IntArray(boolean isMainData)
	{
		lists = new int[250][]; // not set to 101 just in case something changes
		lists[0] = new int[BLOCKSIZE];
		if(isMainData && System.getProperty("sun.arch.data.model").equals("32"))
		{
			System.out
				.println("32-bit java detected, pre-allocating IntArray.");
			for(int a = 1; a <= 101; a++)
				lists[a] = new int[BLOCKSIZE]; // maximum possible seeds is
												// 100.x mil in 1.16
		}
	}
	
	public void clear()
	{
		size = 0;
	}
	
	public void add(int i)
	{
		int id = size / BLOCKSIZE;
		int pos = size % BLOCKSIZE;
		lists[id][pos] = i;
		size++;
		if(size % BLOCKSIZE == 0 && lists[size / BLOCKSIZE] == null)
			lists[size / BLOCKSIZE] = new int[BLOCKSIZE];
	}
	
	public void addAll(int[] values, int amt)
	{
		int pos = 0;
		while(pos < amt)
		{
			addToList(values, pos, Math.min(BLOCKSIZE, amt - pos));
			pos += BLOCKSIZE;
		}
	}
	
	public void addAll(IntArray values)
	{
		int remSize = values.size;
		int curBlock = 0;
		while(remSize > 0)
		{
			addToList(values.lists[curBlock], 0, Math.min(BLOCKSIZE, remSize));
			remSize -= BLOCKSIZE;
			curBlock++;
		}
	}
	
	public int size()
	{
		return size;
	}
	
	public int get(int i)
	{
		return lists[i / BLOCKSIZE][i % BLOCKSIZE];
	}
}

class SimpleRandom
{
	// more efficient implementation of java's random class for this specific
	// use-case
	private static long multiplier = 0x5DEECE66DL;
	private static long mask = (1L << 48) - 1;
	private long seed = 0;
	
	public void setSeed(long seed)
	{
		this.seed = (seed ^ multiplier) & mask;
	}
	
	// Always next(31) - inlined
	private int next()
	{
		seed = (seed * multiplier + 0xBL) & mask;
		return (int)(seed >>> 17);
	}
	
	public int nextInt(int bound)
	{
		int r = next();
		int m = bound - 1;
		if((bound & m) == 0) // i.e., bound is a power of 2
			r = (int)((bound * (long)r) >> 31);
		else
		{
			int u = r;
			while(u - (r = u % bound) + m < 0)
				u = next();
		}
		return r;
	}
}

class Items
{
	
	// @formatter:off
	public static final String
	// 1.8
	LEATHER_HELMET = "leather_helmet",
		LEATHER_CHESTPLATE = "leather_chestplate",
		LEATHER_LEGGINGS = "leather_leggings", LEATHER_BOOTS = "leather_boots",
		IRON_HELMET = "iron_helmet", IRON_CHESTPLATE = "iron_chestplate",
		IRON_LEGGINGS = "iron_leggings", IRON_BOOTS = "iron_boots",
		CHAINMAIL_HELMET = "chainmail_helmet",
		CHAINMAIL_CHESTPLATE = "chainmail_chestplate",
		CHAINMAIL_LEGGINGS = "chainmail_leggings",
		CHAINMAIL_BOOTS = "chainmail_boots", GOLDEN_HELMET = "golden_helmet",
		GOLDEN_CHESTPLATE = "golden_chestplate",
		GOLDEN_LEGGINGS = "golden_leggings", GOLDEN_BOOTS = "golden_boots",
		DIAMOND_HELMET = "diamond_helmet",
		DIAMOND_CHESTPLATE = "diamond_chestplate",
		DIAMOND_LEGGINGS = "diamond_leggings", DIAMOND_BOOTS = "diamond_boots",
		WOODEN_SWORD = "wooden_sword", STONE_SWORD = "stone_sword",
		IRON_SWORD = "iron_sword", GOLDEN_SWORD = "golden_sword",
		DIAMOND_SWORD = "diamond_sword", WOODEN_PICKAXE = "wooden_pickaxe",
		STONE_PICKAXE = "stone_pickaxe", IRON_PICKAXE = "iron_pickaxe",
		GOLDEN_PICKAXE = "golden_pickaxe", DIAMOND_PICKAXE = "diamond_pickaxe",
		WOODEN_AXE = "wooden_axe", STONE_AXE = "stone_axe",
		IRON_AXE = "iron_axe", GOLDEN_AXE = "golden_axe",
		DIAMOND_AXE = "diamond_axe", WOODEN_SHOVEL = "wooden_shovel",
		STONE_SHOVEL = "stone_shovel", IRON_SHOVEL = "iron_shovel",
		GOLDEN_SHOVEL = "golden_shovel", DIAMOND_SHOVEL = "diamond_shovel",
		WOODEN_HOE = "wooden_hoe", STONE_HOE = "stone_hoe",
		IRON_HOE = "iron_hoe", GOLDEN_HOE = "golden_hoe",
		DIAMOND_HOE = "diamond_hoe", CARROT_ON_A_STICK = "carrot_on_a_stick",
		FISHING_ROD = "fishing_rod", FLINT_AND_STEEL = "flint_and_steel",
		SHEARS = "shears", BOW = "bow", BOOK = "book", PUMPKIN = "pumpkin",
		SKULL = "skull",
		// 1.9
		ELYTRA = "elytra", SHIELD = "shield",
		// 1.13
		TRIDENT = "trident", TURTLE_HELMET = "turtle_helmet",
		// 1.14
		CROSSBOW = "crossbow",
		// 1.16
		NETHERITE_HELMET = "netherite_helmet",
		NETHERITE_CHESTPLATE = "netherite_chestplate",
		NETHERITE_LEGGINGS = "netherite_leggings",
		NETHERITE_BOOTS = "netherite_boots",
		NETHERITE_SWORD = "netherite_sword",
		NETHERITE_PICKAXE = "netherite_pickaxe",
		NETHERITE_AXE = "netherite_axe", NETHERITE_SHOVEL = "netherite_shovel",
		NETHERITE_HOE = "netherite_hoe";
	// @formatter:on
	
	public static boolean isArmor(String item)
	{
		if(item.endsWith("_helmet") || item.endsWith("_chestplate")
			|| item.endsWith("_leggings") || item.endsWith("_boots"))
		{
			// @formatter:off
			return LEATHER_HELMET.equals(item)
				|| LEATHER_CHESTPLATE.equals(item)
				|| LEATHER_LEGGINGS.equals(item) || LEATHER_BOOTS.equals(item)
				|| IRON_HELMET.equals(item) || IRON_CHESTPLATE.equals(item)
				|| IRON_LEGGINGS.equals(item) || IRON_BOOTS.equals(item)
				|| CHAINMAIL_HELMET.equals(item)
				|| CHAINMAIL_CHESTPLATE.equals(item)
				|| CHAINMAIL_LEGGINGS.equals(item)
				|| CHAINMAIL_BOOTS.equals(item) || GOLDEN_HELMET.equals(item)
				|| GOLDEN_CHESTPLATE.equals(item)
				|| GOLDEN_LEGGINGS.equals(item) || GOLDEN_BOOTS.equals(item)
				|| DIAMOND_HELMET.equals(item)
				|| DIAMOND_CHESTPLATE.equals(item)
				|| DIAMOND_LEGGINGS.equals(item) || DIAMOND_BOOTS.equals(item)
				|| TURTLE_HELMET.equals(item) || NETHERITE_HELMET.equals(item)
				|| NETHERITE_CHESTPLATE.equals(item)
				|| NETHERITE_LEGGINGS.equals(item)
				|| NETHERITE_BOOTS.equals(item);
			// @formatter:on
		}
		return false;
	}
	
	public static boolean isHelmet(String item)
	{
		return isArmor(item) && item.endsWith("_helmet");
	}
	
	public static boolean isChestplate(String item)
	{
		return isArmor(item) && item.endsWith("_chestplate");
	}
	
	public static boolean isLeggings(String item)
	{
		return isArmor(item) && item.endsWith("_leggings");
	}
	
	public static boolean isBoots(String item)
	{
		return isArmor(item) && item.endsWith("_boots");
	}
	
	public static boolean isSword(String item)
	{
		if(item.endsWith("_sword"))
		{
			// @formatter:off
			return WOODEN_SWORD.equals(item) || STONE_SWORD.equals(item)
				|| IRON_SWORD.equals(item) || GOLDEN_SWORD.equals(item)
				|| DIAMOND_SWORD.equals(item) || NETHERITE_SWORD.equals(item);
			// @formatter:on
		}
		return false;
	}
	
	public static boolean isAxe(String item)
	{
		if(item.endsWith("_axe"))
		{
			// @formatter:off
			return WOODEN_AXE.equals(item) || STONE_AXE.equals(item)
				|| IRON_AXE.equals(item) || GOLDEN_AXE.equals(item)
				|| DIAMOND_AXE.equals(item) || NETHERITE_AXE.equals(item);
			// @formatter:on
		}
		return false;
	}
	
	public static boolean isTool(String item)
	{
		if(isAxe(item))
		{
			return true;
		}
		if(item.endsWith("_pickaxe") || item.endsWith("_shovel")
			|| item.endsWith("_hoe"))
		{
			// @formatter:off
			return WOODEN_PICKAXE.equals(item) || STONE_PICKAXE.equals(item)
				|| IRON_PICKAXE.equals(item) || GOLDEN_PICKAXE.equals(item)
				|| DIAMOND_PICKAXE.equals(item)
				|| NETHERITE_PICKAXE.equals(item) || WOODEN_SHOVEL.equals(item)
				|| STONE_SHOVEL.equals(item) || IRON_SHOVEL.equals(item)
				|| GOLDEN_SHOVEL.equals(item) || DIAMOND_SHOVEL.equals(item)
				|| NETHERITE_SHOVEL.equals(item) || WOODEN_HOE.equals(item)
				|| STONE_HOE.equals(item) || IRON_HOE.equals(item)
				|| GOLDEN_HOE.equals(item) || DIAMOND_HOE.equals(item)
				|| NETHERITE_HOE.equals(item);
			// @formatter:on
		}
		return false;
	}
	
	public static boolean hasDurability(String item)
	{
		// @formatter:off
		return isArmor(item) || isTool(item) || isSword(item)
			|| BOW.equals(item) || CARROT_ON_A_STICK.equals(item)
			|| ELYTRA.equals(item) || FISHING_ROD.equals(item)
			|| FLINT_AND_STEEL.equals(item) || SHEARS.equals(item)
			|| SHIELD.equals(item) || TRIDENT.equals(item)
			|| CROSSBOW.equals(item);
		// @formatter:on
	}
	
	public static int getEnchantability(String item)
	{
		if(isArmor(item))
		{
			if(item.startsWith("leather_"))
			{
				return 15;
			}
			if(item.startsWith("iron_"))
			{
				return 9;
			}
			if(item.startsWith("chainmail_"))
			{
				return 12;
			}
			if(item.startsWith("golden_"))
			{
				return 25;
			}
			if(item.startsWith("diamond_"))
			{
				return 10;
			}
			if(item.startsWith("turtle_"))
			{
				return 9;
			}
			if(item.startsWith("netherite_"))
			{
				return 15;
			}
		}
		if(isSword(item) || isTool(item))
		{
			if(item.startsWith("wooden_"))
			{
				return 15;
			}
			if(item.startsWith("stone_"))
			{
				return 5;
			}
			if(item.startsWith("iron_"))
			{
				return 14;
			}
			if(item.startsWith("golden_"))
			{
				return 22;
			}
			if(item.startsWith("diamond_"))
			{
				return 10;
			}
			if(item.startsWith("netherite_"))
			{
				return 15;
			}
		}
		if(BOW.equals(item))
		{
			return 1;
		}
		if(FISHING_ROD.equals(item))
		{
			return 1;
		}
		if(TRIDENT.equals(item))
		{
			return 1;
		}
		if(CROSSBOW.equals(item))
		{
			return 1;
		}
		if(BOOK.equals(item))
		{
			return 1;
		}
		return 0;
	}
	
	public static Versions getIntroducedVersion(String item)
	{
		switch(item)
		{
			case ELYTRA:
			case SHIELD:
			return Versions.V1_9;
			case TRIDENT:
			case TURTLE_HELMET:
			return Versions.V1_13;
			case CROSSBOW:
			return Versions.V1_14;
			case NETHERITE_HELMET:
			case NETHERITE_CHESTPLATE:
			case NETHERITE_LEGGINGS:
			case NETHERITE_BOOTS:
			case NETHERITE_SWORD:
			case NETHERITE_PICKAXE:
			case NETHERITE_AXE:
			case NETHERITE_SHOVEL:
			case NETHERITE_HOE:
			return Versions.V1_16;
			default:
			return Versions.V1_8;
		}
	}
	
}

class Materials
{
	
	public static final int NETHERITE = 0;
	public static final int DIAMOND = 1;
	public static final int GOLD = 2;
	public static final int IRON = 3;
	public static final int CHAIN = 4;
	public static final int STONE = 5;
	public static final int LEATHER = 6;
	
	public static Versions getIntroducedVersion(int material)
	{
		if(material == NETHERITE)
			return Versions.V1_16;
		return Versions.V1_8;
	}
	
}

enum Versions
{
	V1_8("1.8 - 1.8.9"),
	V1_9("1.9 - 1.10.2"),
	V1_11("1.11"),
	V1_11_1("1.11.1 - 1.12.2"),
	V1_13("1.13 - 1.13.2"),
	V1_14("1.14 - 1.14.2"),
	V1_14_3("1.14.3 - 1.15.2"),
	V1_16("1.16 - 1.16.5");
	
	public final String name;
	
	Versions(String name)
	{
		this.name = name;
	}
	
	public boolean before(Versions other)
	{
		return ordinal() < other.ordinal();
	}
	
	public boolean after(Versions other)
	{
		return ordinal() > other.ordinal();
	}
	
	public static Versions latest()
	{
		return values()[values().length - 1];
	}
	
	public String toString()
	{
		return name;
	}
}

class Enchantments
{
	
	// @formatter:off
	public static final String
	// 1.8
	PROTECTION = "protection", FIRE_PROTECTION = "fire_protection",
		FEATHER_FALLING = "feather_falling",
		BLAST_PROTECTION = "blast_protection",
		PROJECTILE_PROTECTION = "projectile_protection",
		RESPIRATION = "respiration", AQUA_AFFINITY = "aqua_affinity",
		THORNS = "thorns", DEPTH_STRIDER = "depth_strider",
		SHARPNESS = "sharpness", SMITE = "smite",
		BANE_OF_ARTHROPODS = "bane_of_arthropods", KNOCKBACK = "knockback",
		FIRE_ASPECT = "fire_aspect", LOOTING = "looting",
		EFFICIENCY = "efficiency", SILK_TOUCH = "silk_touch",
		UNBREAKING = "unbreaking", FORTUNE = "fortune", POWER = "power",
		PUNCH = "punch", FLAME = "flame", INFINITY = "infinity",
		LUCK_OF_THE_SEA = "luck_of_the_sea", LURE = "lure",
		// 1.9
		FROST_WALKER = "frost_walker", MENDING = "mending",
		// 1.11
		BINDING_CURSE = "binding_curse", VANISHING_CURSE = "vanishing_curse",
		// 1.11.1
		SWEEPING = "sweeping",
		// 1.13
		LOYALTY = "loyalty", IMPALING = "impaling", RIPTIDE = "riptide",
		CHANNELING = "channeling",
		// 1.14
		MULTISHOT = "multishot", QUICK_CHARGE = "quick_charge",
		PIERCING = "piercing";
	// @formatter:on
	
	// @formatter:off
	public static final LinkedHashSet<String> ALL_ENCHANTMENTS =
		new LinkedHashSet<>();
	// @formatter:on
	
	private static final List<CompatibilityFunction> COMPATIBILITY_FUNCTIONS =
		new ArrayList<>();
	
	static
	{
		// Add in registry order
		Collections.addAll(ALL_ENCHANTMENTS, PROTECTION, FIRE_PROTECTION,
			FEATHER_FALLING, BLAST_PROTECTION, PROJECTILE_PROTECTION,
			RESPIRATION, AQUA_AFFINITY, THORNS, DEPTH_STRIDER, FROST_WALKER,
			BINDING_CURSE, SHARPNESS, SMITE, BANE_OF_ARTHROPODS, KNOCKBACK,
			FIRE_ASPECT, LOOTING, SWEEPING, EFFICIENCY, SILK_TOUCH, UNBREAKING,
			FORTUNE, POWER, PUNCH, FLAME, INFINITY, LUCK_OF_THE_SEA, LURE,
			LOYALTY, IMPALING, RIPTIDE, CHANNELING, MULTISHOT, QUICK_CHARGE,
			PIERCING, MENDING, VANISHING_CURSE);
		
		// every enchantment with itself
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> !enchA.equals(enchB));
		
		// infinity with mending
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version.before(Versions.V1_11_1)
				|| !enchA.equals(INFINITY) || !enchB.equals(MENDING));
		// sharpness with smite
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(SHARPNESS) || !enchB.equals(SMITE));
		// sharpness with bane of arthropods
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> !enchA.equals(SHARPNESS)
				|| !enchB.equals(BANE_OF_ARTHROPODS));
		// smite with bane of arthropods
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> !enchA.equals(SMITE)
				|| !enchB.equals(BANE_OF_ARTHROPODS));
		// depth strider with frost walker
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> !enchA.equals(DEPTH_STRIDER)
				|| !enchB.equals(FROST_WALKER));
		// silk touch with looting
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(SILK_TOUCH) || !enchB.equals(LOOTING));
		// silk touch with fortune
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(SILK_TOUCH) || !enchB.equals(FORTUNE));
		// silk touch with luck of the sea
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> !enchA.equals(SILK_TOUCH)
				|| !enchB.equals(LUCK_OF_THE_SEA));
		// riptide with loyalty
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(RIPTIDE) || !enchB.equals(LOYALTY));
		// riptide with channeling
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(RIPTIDE) || !enchB.equals(CHANNELING));
		// multishot with piercing
		COMPATIBILITY_FUNCTIONS.add((enchA, enchB,
			version) -> !enchA.equals(MULTISHOT) || !enchB.equals(PIERCING));
		
		// protection with blast protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(PROTECTION)
				|| !enchB.equals(BLAST_PROTECTION));
		// protection with fire protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(PROTECTION) || !enchB.equals(FIRE_PROTECTION));
		// protection with projectile protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(PROTECTION)
				|| !enchB.equals(PROJECTILE_PROTECTION));
		// blast protection with fire protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(BLAST_PROTECTION)
				|| !enchB.equals(FIRE_PROTECTION));
		// blast protection with projectile protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(BLAST_PROTECTION)
				|| !enchB.equals(PROJECTILE_PROTECTION));
		// fire protection with projectile protection
		COMPATIBILITY_FUNCTIONS
			.add((enchA, enchB, version) -> version == Versions.V1_14
				|| !enchA.equals(FIRE_PROTECTION)
				|| !enchB.equals(PROJECTILE_PROTECTION));
		
		for(Field field : Enchantments.class.getDeclaredFields())
		{
			if(field.getModifiers() == (Modifier.PUBLIC | Modifier.STATIC
				| Modifier.FINAL))
			{
				if(field.getType() == String.class)
				{
					String enchantmentName;
					try
					{
						enchantmentName = (String)field.get(null);
					}catch(Exception e)
					{
						throw new Error(e);
					}
					ALL_ENCHANTMENTS.add(enchantmentName);
				}
			}
		}
	}
	
	public static int levelsToXP(int startLevel, int numLevels)
	{
		int amt = 0;
		int endLevel = startLevel - numLevels;
		for(int level = startLevel; level > endLevel; level--)
		{
			if(level > 30)
				amt += (9 * (level - 1)) - 158;
			else if(level > 15)
				amt += (5 * (level - 1)) - 38;
			else
				amt += (2 * (level - 1)) + 7;
		}
		return amt;
	}
	
	public static boolean canApply(String enchantment, String item,
		boolean primary)
	{
		if(Items.BOOK.equals(item))
		{
			return true;
		}
		
		switch(enchantment)
		{
			case PROTECTION:
			case FIRE_PROTECTION:
			case BLAST_PROTECTION:
			case PROJECTILE_PROTECTION:
			return Items.isArmor(item);
			case THORNS:
			return primary ? Items.isChestplate(item) : Items.isArmor(item);
			case FEATHER_FALLING:
			case DEPTH_STRIDER:
			case FROST_WALKER:
			return Items.isBoots(item);
			case RESPIRATION:
			case AQUA_AFFINITY:
			return Items.isHelmet(item);
			case BINDING_CURSE:
			return Items.isArmor(item) || Items.PUMPKIN.equals(item)
				|| Items.ELYTRA.equals(item) || Items.SKULL.equals(item);
			case SHARPNESS:
			case SMITE:
			case BANE_OF_ARTHROPODS:
			return Items.isSword(item) || !primary && Items.isAxe(item);
			case KNOCKBACK:
			case FIRE_ASPECT:
			case LOOTING:
			case SWEEPING:
			return Items.isSword(item);
			case EFFICIENCY:
			return Items.isTool(item) || !primary && Items.SHEARS.equals(item);
			case SILK_TOUCH:
			case FORTUNE:
			return Items.isTool(item);
			case POWER:
			case PUNCH:
			case FLAME:
			case INFINITY:
			return Items.BOW.equals(item);
			case LUCK_OF_THE_SEA:
			case LURE:
			return Items.FISHING_ROD.equals(item);
			case UNBREAKING:
			case MENDING:
			return Items.hasDurability(item);
			case VANISHING_CURSE:
			return Items.hasDurability(item) || Items.PUMPKIN.equals(item)
				|| Items.SKULL.equals(item);
			case LOYALTY:
			case IMPALING:
			case RIPTIDE:
			case CHANNELING:
			return Items.TRIDENT.equals(item);
			case MULTISHOT:
			case QUICK_CHARGE:
			case PIERCING:
			return Items.CROSSBOW.equals(item);
			default:
			throw new IllegalArgumentException(
				"Unknown enchantment: " + enchantment);
		}
	}
	
	public static boolean isTreasure(String enchantment)
	{
		return FROST_WALKER.equals(enchantment) || MENDING.equals(enchantment)
			|| BINDING_CURSE.equals(enchantment)
			|| VANISHING_CURSE.equals(enchantment);
	}
	
	public static int getMaxLevel(String enchantment)
	{
		switch(enchantment)
		{
			case SHARPNESS:
			case SMITE:
			case BANE_OF_ARTHROPODS:
			case EFFICIENCY:
			case POWER:
			case IMPALING:
			return 5;
			case PROTECTION:
			case FIRE_PROTECTION:
			case BLAST_PROTECTION:
			case PROJECTILE_PROTECTION:
			case FEATHER_FALLING:
			case PIERCING:
			return 4;
			case THORNS:
			case DEPTH_STRIDER:
			case RESPIRATION:
			case LOOTING:
			case SWEEPING:
			case FORTUNE:
			case LUCK_OF_THE_SEA:
			case LURE:
			case UNBREAKING:
			case LOYALTY:
			case RIPTIDE:
			case QUICK_CHARGE:
			return 3;
			case FROST_WALKER:
			case KNOCKBACK:
			case FIRE_ASPECT:
			case PUNCH:
			return 2;
			case AQUA_AFFINITY:
			case BINDING_CURSE:
			case SILK_TOUCH:
			case FLAME:
			case INFINITY:
			case MENDING:
			case VANISHING_CURSE:
			case CHANNELING:
			case MULTISHOT:
			return 1;
			default:
			throw new IllegalArgumentException(
				"Unknown enchantment: " + enchantment);
		}
	}
	
	public static int getMinEnchantability(String enchantment, int level)
	{
		switch(enchantment)
		{
			case PROTECTION:
			return 1 + (level - 1) * 11;
			case FIRE_PROTECTION:
			return 10 + (level - 1) * 8;
			case FEATHER_FALLING:
			return 5 + (level - 1) * 6;
			case BLAST_PROTECTION:
			return 5 + (level - 1) * 8;
			case PROJECTILE_PROTECTION:
			return 3 + (level - 1) * 6;
			case RESPIRATION:
			return level * 10;
			case AQUA_AFFINITY:
			return 1;
			case THORNS:
			return 10 + (level - 1) * 20;
			case DEPTH_STRIDER:
			return level * 10;
			case FROST_WALKER:
			return level * 10;
			case BINDING_CURSE:
			return 25;
			case SHARPNESS:
			return 1 + (level - 1) * 11;
			case SMITE:
			return 5 + (level - 1) * 8;
			case BANE_OF_ARTHROPODS:
			return 5 + (level - 1) * 8;
			case KNOCKBACK:
			return 5 + (level - 1) * 20;
			case FIRE_ASPECT:
			return 10 + (level - 1) * 20;
			case LOOTING:
			return 15 + (level - 1) * 9;
			case SWEEPING:
			return 5 + (level - 1) * 9;
			case EFFICIENCY:
			return 1 + (level - 1) * 10;
			case SILK_TOUCH:
			return 15;
			case UNBREAKING:
			return 5 + (level - 1) * 8;
			case FORTUNE:
			return 15 + (level - 1) * 9;
			case POWER:
			return 1 + (level - 1) * 10;
			case PUNCH:
			return 12 + (level - 1) * 20;
			case FLAME:
			return 20;
			case INFINITY:
			return 20;
			case LUCK_OF_THE_SEA:
			return 15 + (level - 1) * 9;
			case LURE:
			return 15 + (level - 1) * 9;
			case MENDING:
			return 25;
			case VANISHING_CURSE:
			return 25;
			case LOYALTY:
			return 5 + level * 7;
			case IMPALING:
			return 1 + (level - 1) * 8;
			case RIPTIDE:
			return 10 + level * 7;
			case CHANNELING:
			return 25;
			case MULTISHOT:
			return 20;
			case QUICK_CHARGE:
			return 12 + (level - 1) * 20;
			case PIERCING:
			return 1 + (level - 1) * 10;
			default:
			throw new IllegalArgumentException(
				"Unknown enchantment: " + enchantment);
		}
	}
	
	public static int getMaxEnchantability(String enchantment, int level)
	{
		switch(enchantment)
		{
			case PROTECTION:
			return 1 + level * 11;
			case FIRE_PROTECTION:
			return 10 + level * 8;
			case FEATHER_FALLING:
			return 5 + level * 6;
			case BLAST_PROTECTION:
			return 5 + level * 8;
			case PROJECTILE_PROTECTION:
			return 3 + level * 6;
			case RESPIRATION:
			return 30 + level * 10;
			case AQUA_AFFINITY:
			return 41;
			case THORNS:
			return 40 + level * 20;
			case DEPTH_STRIDER:
			return 15 + level * 10;
			case FROST_WALKER:
			return 15 + level * 10;
			case BINDING_CURSE:
			return 50;
			case SHARPNESS:
			return 21 + (level - 1) * 11;
			case SMITE:
			return 25 + (level - 1) * 8;
			case BANE_OF_ARTHROPODS:
			return 25 + (level - 1) * 8;
			case KNOCKBACK:
			return 55 + (level - 1) * 20;
			case FIRE_ASPECT:
			return 40 + level * 20;
			case LOOTING:
			return 65 + (level - 1) * 9;
			case SWEEPING:
			return 20 + (level - 1) * 9;
			case EFFICIENCY:
			return 50 + level * 10;
			case SILK_TOUCH:
			return 65;
			case UNBREAKING:
			return 55 + (level - 1) * 8;
			case FORTUNE:
			return 65 + (level - 1) * 9;
			case POWER:
			return 16 + (level - 1) * 10;
			case PUNCH:
			return 37 + (level - 1) * 20;
			case FLAME:
			return 50;
			case INFINITY:
			return 50;
			case LUCK_OF_THE_SEA:
			return 65 + (level - 1) * 9;
			case LURE:
			return 65 + (level - 1) * 9;
			case MENDING:
			return 75;
			case VANISHING_CURSE:
			return 50;
			case LOYALTY:
			return 50;
			case IMPALING:
			return 21 + (level - 1) * 8;
			case RIPTIDE:
			return 50;
			case CHANNELING:
			return 50;
			case MULTISHOT:
			return 50;
			case QUICK_CHARGE:
			return 50;
			case PIERCING:
			return 50;
			default:
			throw new IllegalArgumentException(
				"Unknown enchantment: " + enchantment);
		}
	}
	
	public static int getWeight(String enchantment, Versions version)
	{
		switch(enchantment)
		{
			case PROTECTION:
			case SHARPNESS:
			case EFFICIENCY:
			case POWER:
			case PIERCING:
			return version == Versions.V1_14 ? 30 : 10;
			case FIRE_PROTECTION:
			case FEATHER_FALLING:
			case PROJECTILE_PROTECTION:
			case SMITE:
			case BANE_OF_ARTHROPODS:
			case KNOCKBACK:
			case UNBREAKING:
			case LOYALTY:
			case QUICK_CHARGE:
			return version == Versions.V1_14 ? 10 : 5;
			case BLAST_PROTECTION:
			case RESPIRATION:
			case AQUA_AFFINITY:
			case DEPTH_STRIDER:
			case FROST_WALKER:
			case FIRE_ASPECT:
			case LOOTING:
			case SWEEPING:
			case FORTUNE:
			case PUNCH:
			case FLAME:
			case LUCK_OF_THE_SEA:
			case LURE:
			case MENDING:
			case IMPALING:
			case RIPTIDE:
			case MULTISHOT:
			return version == Versions.V1_14 ? 3 : 2;
			case THORNS:
			case BINDING_CURSE:
			case SILK_TOUCH:
			case INFINITY:
			case VANISHING_CURSE:
			case CHANNELING:
			return 1;
			default:
			throw new IllegalArgumentException(
				"Unknown enchantment: " + enchantment);
		}
	}
	
	public static Versions getIntroducedVersion(String enchantment)
	{
		switch(enchantment)
		{
			case FROST_WALKER:
			case MENDING:
			return Versions.V1_9;
			case BINDING_CURSE:
			case VANISHING_CURSE:
			return Versions.V1_11;
			case SWEEPING:
			return Versions.V1_11_1;
			case LOYALTY:
			case IMPALING:
			case RIPTIDE:
			case CHANNELING:
			return Versions.V1_13;
			case MULTISHOT:
			case QUICK_CHARGE:
			case PIERCING:
			return Versions.V1_14;
			default:
			return Versions.V1_8;
		}
	}
	
	public static int getMaxLevelInTable(String enchantment, String item)
	{
		// Get the max level on enchantment tables by maximizing the random
		// values
		int enchantability = Items.getEnchantability(item);
		int maxLevel;
		if(enchantability == 0 || isTreasure(enchantment)
			|| !canApply(enchantment, item, true))
		{
			return 0;
		}else
		{
			int level = 30 + 1 + enchantability / 4 + enchantability / 4;
			level += Math.round(level * 0.15f);
			for(maxLevel = getMaxLevel(enchantment); maxLevel >= 1; maxLevel--)
			{
				if(level >= getMinEnchantability(enchantment, maxLevel))
				{
					return maxLevel;
				}
			}
			return 0;
		}
	}
	
	public static boolean areCompatible(String enchA, String enchB,
		Versions version)
	{
		for(CompatibilityFunction func : COMPATIBILITY_FUNCTIONS)
		{
			if(!func.compatible(enchA, enchB, version))
				return false;
			if(!func.compatible(enchB, enchA, version))
				return false;
		}
		return true;
	}
	
	public static int calcEnchantmentTableLevel(Random rand, int slot,
		int bookshelves, String item)
	{
		if(Items.getEnchantability(item) == 0)
		{
			return 0;
		}
		
		int level = rand.nextInt(8) + 1 + (bookshelves >> 1)
			+ rand.nextInt(bookshelves + 1);
		
		switch(slot)
		{
			case 0:
			return Math.max(level / 3, 1);
			case 1:
			return level * 2 / 3 + 1;
			case 2:
			return Math.max(level, bookshelves * 2);
			default:
			throw new IllegalArgumentException();
		}
	}
	
	public static List<EnchantmentInstance> getEnchantmentsInTable(Random rand,
		int xpSeed, String item, int slot, int levels, Versions version)
	{
		rand.setSeed(xpSeed + slot);
		
		List<EnchantmentInstance> list =
			addRandomEnchantments(rand, item, levels, false, version);
		if(Items.BOOK.equals(item) && list.size() > 1)
		{
			list.remove(rand.nextInt(list.size()));
		}
		
		return list;
	}
	
	public static List<EnchantmentInstance> getHighestAllowedEnchantments(
		int level, String item, boolean treasure, Versions version)
	{
		List<EnchantmentInstance> allowedEnchantments = new ArrayList<>();
		
		if(version.before(Items.getIntroducedVersion(item)))
			return allowedEnchantments;
		
		for(String enchantment : ALL_ENCHANTMENTS)
		{
			if(version.before(getIntroducedVersion(enchantment)))
				continue;
			
			if((treasure || !isTreasure(enchantment))
				&& canApply(enchantment, item, true))
			{
				for(int enchLvl =
					getMaxLevel(enchantment); enchLvl >= 1; enchLvl--)
				{
					if(level >= getMinEnchantability(enchantment, enchLvl)
						&& level <= getMaxEnchantability(enchantment, enchLvl))
					{
						allowedEnchantments
							.add(new EnchantmentInstance(enchantment, enchLvl));
						break;
					}
				}
			}
		}
		return allowedEnchantments;
	}
	
	public static List<EnchantmentInstance> addRandomEnchantments(Random rand,
		String item, int level, boolean treasure, Versions version)
	{
		int enchantability = Items.getEnchantability(item);
		List<EnchantmentInstance> enchantments = new ArrayList<>();
		
		if(enchantability > 0)
		{
			// Modify the enchantment level randomly and according to
			// enchantability
			level = level + 1 + rand.nextInt(enchantability / 4 + 1)
				+ rand.nextInt(enchantability / 4 + 1);
			float percentChange =
				(rand.nextFloat() + rand.nextFloat() - 1) * 0.15f;
			level += Math.round(level * percentChange);
			if(level < 1)
			{
				level = 1;
			}
			
			// Get a list of allowed enchantments with their max allowed levels
			List<EnchantmentInstance> allowedEnchantments =
				getHighestAllowedEnchantments(level, item, treasure, version);
			
			// allowedEnchantments.forEach(ench -> System.out.println("Allowed:
			// " + ench));
			
			if(!allowedEnchantments.isEmpty())
			{
				// Get first enchantment
				EnchantmentInstance enchantmentInstance =
					weightedRandom(rand, allowedEnchantments,
						it -> getWeight(it.enchantment, version));
				enchantments.add(enchantmentInstance);
				
				// Get optional extra enchantments
				while(rand.nextInt(50) <= level)
				{
					// 1.14 enchantment nerf
					if(version == Versions.V1_14)
					{
						level = level * 4 / 5 + 1;
						allowedEnchantments = getHighestAllowedEnchantments(
							level, item, treasure, version);
					}
					
					// Remove incompatible enchantments from allowed list with
					// last enchantment
					for(EnchantmentInstance ench : enchantments)
					{
						String enchantment = ench.enchantment;
						allowedEnchantments
							.removeIf(it -> !areCompatible(it.enchantment,
								enchantment, version));
					}
					
					if(allowedEnchantments.isEmpty())
					{
						// no enchantments left
						break;
					}
					
					// Get extra enchantment
					enchantmentInstance =
						weightedRandom(rand, allowedEnchantments,
							it -> getWeight(it.enchantment, version));
					enchantments.add(enchantmentInstance);
					
					// Make it less likely for another enchantment to happen
					level /= 2;
				}
			}
		}
		
		return enchantments;
	}
	
	public static class EnchantmentInstance
	{
		public final String enchantment;
		public final int level;
		
		public EnchantmentInstance(String enchantment, int level)
		{
			this.enchantment = enchantment;
			this.level = level;
		}
		
		@Override
		public int hashCode()
		{
			return enchantment.hashCode() + 31 * level;
		}
		
		@Override
		public boolean equals(Object other)
		{
			return other instanceof EnchantmentInstance
				&& equals((EnchantmentInstance)other);
		}
		
		public boolean equals(EnchantmentInstance other)
		{
			return enchantment.equals(other.enchantment)
				&& level == other.level;
		}
		
		@Override
		public String toString()
		{
			if(level == 1 && getMaxLevel(enchantment) == 1)
			{
				return enchantment;
			}
			String lvlName;
			switch(level)
			{
				case 1:
				lvlName = "I";
				break;
				case 2:
				lvlName = "II";
				break;
				case 3:
				lvlName = "III";
				break;
				case 4:
				lvlName = "IV";
				break;
				case 5:
				lvlName = "V";
				break;
				default:
				lvlName = String.valueOf(level);
				break;
			}
			return enchantment + " " + lvlName;
		}
	}
	
	private static <T> T weightedRandom(Random rand, List<T> list,
		ToIntFunction<T> weightExtractor)
	{
		int weight = list.stream().mapToInt(weightExtractor).sum();
		if(weight <= 0)
		{
			return null;
		}
		weight = rand.nextInt(weight);
		for(T t : list)
		{
			weight -= weightExtractor.applyAsInt(t);
			if(weight < 0)
			{
				return t;
			}
		}
		return null;
	}
	
	@FunctionalInterface
	private static interface CompatibilityFunction
	{
		boolean compatible(String enchA, String enchB, Versions version);
	}
	
}

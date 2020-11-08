/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved. This source code is subject to
 * the terms of the GNU General Public License, version 3. If a copy of the GPL was not distributed
 * with this file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.util.ChatUtils;

public final class ConnectCmd extends Command implements UpdateListener {
	
	public static PrivateChat privateChat;
	public static List<String> messages;
	
	public static boolean active = false;
	public static int dimension = 0;
	
	public ConnectCmd() {
		super("connect", "Connect to a private chat server.", ".connect <ip> <port>");
	}
	
	@Override
	public void call(String[] args) throws CmdException {
		
		if (args.length < 2) throw new CmdSyntaxError();
		if (args[0].equals("find")) {
			if (privateChat == null) {
				ChatUtils.error("No private chat connected.");
				return;
			}
			if (privateChat.s == null) {
				ChatUtils.error("Private chat not yet connected.");
				return;
			}
			Vec3d pos = privateChat.players.get(args[1]);
			if (pos == null) {
				ChatUtils.error("That player could not be found.");
			} else {
				ChatUtils.message(args[1] + " was found at " + (int) pos.x + ", " + (int) pos.y + ", " + (int) pos.z + " distance: " + (int) MC.player.getPos().distanceTo(pos));
			}
			return;
			
		}
		try {
			int port = Integer.parseInt(args[1]);
			
			if (privateChat != null) {
				privateChat.disconnect();
			}
			
			privateChat = new PrivateChat(args[0], port);
			
			if (messages == null) {
				messages = Collections.synchronizedList(new ArrayList<>());
				EVENTS.add(UpdateListener.class, this);
			}
			
		} catch (Exception e) {
			throw new CmdSyntaxError();
		}
		
	}
	
	public static class PrivateChat extends Thread {
		
		Socket s;
		DataInputStream in;
		DataOutputStream out;
		public Map<String, Vec3d> players = new HashMap<>();
		public Map<String, Integer> playersDim = new HashMap<>();
		
		public PrivateChat(String string, int port) {
			new Thread(() -> {
				try {
					s = new Socket(string, port);
					in = new DataInputStream(s.getInputStream());
					out = new DataOutputStream(s.getOutputStream());
					try {
						out.writeInt(0);
						out.writeUTF(MC.player.getName().getString());
						ConnectCmd.messages.add("^Connected to " + string + ":" + port);
						this.start();
					} catch (Exception e1) {
						disconnect();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					ConnectCmd.messages.add("^Could not connect to " + string + ":" + port);
					privateChat = null;
				}
				
			}).start();
		}
		
		public void run() {
			try {
				while (true) {
					int message = in.readInt();
					switch (message) {
						case 0: {
							String name = in.readUTF();
							ConnectCmd.messages.add("§7" + name + " has joined the chat.");
							break;
						}
						case 1: {
							String name = in.readUTF();
							double x = in.readDouble();
							double y = in.readDouble();
							double z = in.readDouble();
							int d = in.readInt();
							synchronized (players) {
								players.put(name, new Vec3d(x, y, z));
								playersDim.put(name, d);
							}
							break;
						}
						case 2: {
							String chat = in.readUTF();
							ConnectCmd.messages.add(chat);
							break;
						}
						case 3: {
							String name = in.readUTF();
							ConnectCmd.messages.add("§7" + name + " has left the chat.");
							players.remove(name);
							playersDim.remove(name);
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				disconnect();
			}
		}
		
		public void chat(String message) {
			try {
				out.writeInt(2);
				out.writeUTF(message);
			} catch (Exception e1) {
				disconnect();
			}
		}
		
		public void move(double x, double y, double z, int d) {
			try {
				out.writeInt(1);
				out.writeDouble(x);
				out.writeDouble(y);
				out.writeDouble(z);
				out.writeInt(d);
			} catch (Exception e1) {
				disconnect();
			}
		}
		
		public void disconnect() {
			try {
				s.close();
			} catch (IOException e1) {
			}
			privateChat = null;
			messages.add("^Message client disconnected.");
		}
	}
	
	long lastUpdate = System.currentTimeMillis();
	
	public void onUpdate() {
		synchronized (messages) {
			for (String s : messages) {
				if (s.startsWith("^")) {
					ChatUtils.message(s.substring(1));
				} else {
					ChatUtils.rawMessage(s);
				}
			}
			messages.clear();
		}
		if (privateChat != null && privateChat.s != null) {
			if (System.currentTimeMillis() - lastUpdate > 100) {
				lastUpdate = System.currentTimeMillis();
				dimension = dimension(MC.player.world.getDimension());
				privateChat.move(MC.player.getX(), MC.player.getY(), MC.player.getZ(), dimension);
			}
		}
	}
	
	public static int dimension(DimensionType type) {
		if (type.isPiglinSafe() || type.isUltrawarm()) {
			return 2;
		}
		if (type.hasFixedTime()) {
			return 1;
		}
		return 0;
	}
	
	public static void message(String message) {
		if (privateChat == null) {
			ChatUtils.error("No private chat connected.");
			return;
		}
		if (privateChat.s == null) {
			ChatUtils.error("Private chat not yet connected.");
			return;
		}
		privateChat.chat(message);
	}
}

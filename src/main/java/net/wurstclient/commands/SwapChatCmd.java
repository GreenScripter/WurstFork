/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved. This source code is subject to
 * the terms of the GNU General Public License, version 3. If a copy of the GPL was not distributed
 * with this file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class SwapChatCmd extends Command {
	
	public SwapChatCmd() {
		super("swapchat", "Swap between private and server chat.", ".swapchat [private]");
	}
	
	@Override
	public void call(String[] args) throws CmdException {
		if (args.length > 0) {
			if (args[0].equals("true")) {
				ConnectCmd.active = true;
			} else {
				ConnectCmd.active = false;
			}
		} else {
			ConnectCmd.active = !ConnectCmd.active;
		}
		ChatUtils.message("Private chat " + (ConnectCmd.active ? "enabled." : "disabled."));
	}
	
}

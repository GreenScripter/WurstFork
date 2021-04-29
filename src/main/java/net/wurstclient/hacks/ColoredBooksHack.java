/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;

public final class ColoredBooksHack extends Hack implements PacketOutputListener
{
	public ColoredBooksHack()
	{
		super("ColoredBooks",
			"Replaces the & symbol in books with §, the format character. \n&& will be replaced with & instead.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof BookUpdateC2SPacket)
		{
			String temptag = UUID.randomUUID().toString();
			BookUpdateC2SPacket packet = (BookUpdateC2SPacket)event.getPacket();
			PacketByteBuf buff = new PacketByteBuf(Unpooled.buffer());
			try
			{
				packet.write(buff);
				ItemStack is = buff.readItemStack();
				boolean signed = buff.readBoolean();
				int v = buff.readVarInt();
				is.getTag().put("title",
					StringTag.of(
						is.getTag().getString("title").replace("&&", temptag)
							.replace("&", "§").replace(temptag, "&")));
				ListTag tag = is.getTag().getList("pages", 8);
				List<String> pages = new ArrayList<>();
				for(Tag t : tag)
				{
					String s = t.asString();
					s = s.replace("&&", temptag).replace("&", "§")
						.replace(temptag, "&");
					pages.add(s);
				}
				tag.clear();
				for(String s : pages)
				{
					tag.add(StringTag.of(s));
				}
				
				buff.clear();
				buff.writeItemStack(is);
				buff.writeBoolean(signed);
				buff.writeVarInt(v);
				
				packet.read(buff);
			}catch(IOException e)
			{
				e.printStackTrace();
			}
			
		}
	}
}

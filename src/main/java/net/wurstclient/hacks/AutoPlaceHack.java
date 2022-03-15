/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;

@SearchTags({"auto use", "auto place"})
public final class AutoPlaceHack extends Hack implements UpdateListener
{
	public AutoPlaceHack()
	{
		super("AutoPlace", "Automatically holds the use key.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		MC.options.useKey.setPressed(((IKeyBinding) MC.options.useKey).isActallyPressed());
	}
	
	@Override
	public void onUpdate()
	{
		MC.options.useKey.setPressed(true);

	}
}

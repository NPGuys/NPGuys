/*
* NPGuys - Bukkit plugin for better NPC interaction
* Copyright (C) 2014 Adam Gotlib <Goldob>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package goldob.npguys.action;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import goldob.npguys.NPGuys;
import goldob.npguys.exception.FailedToLoadException;
import goldob.npguys.exception.InvalidCommandException;

public class TakePermission extends Action {
	private String permission;
	
	public TakePermission(String type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void perform(NPC npc, Player player) {
		player.addAttachment(NPGuys.getPlugin(), permission, false);
		player.recalculatePermissions();
	}

	@Override
	public void load(ConfigurationSection data) throws FailedToLoadException {
		if (data.contains("permission") && data.get("permission") instanceof String) {
			permission = data.getString("permission");
		}
		else {
			throw new FailedToLoadException("Permission missing!");
		}
	}

	@Override
	public void fromCommand(String[] data) throws InvalidCommandException {
		if (data.length < 1) {
			throw new InvalidCommandException("Permission missing!");
		}
		if (data.length > 1) {
			throw new InvalidCommandException("Too long command syntax!");
		}
		permission = data[0];
	}
	
	@Override
	public void save(ConfigurationSection data) {
		super.save(data);
		data.set("permission", permission);
	}
	
	@Override
	public String getDescription() {
		return "Takes back a permission from the player.";
	}

	@Override
	public String getUsage() {
		return "[permission]";
	}

	@Override
	public String getData() {
		return permission;
	}
}

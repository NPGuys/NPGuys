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

import goldob.npguys.exception.FailedToLoadException;
import goldob.npguys.exception.InvalidCommandException;


public abstract class Action {
	String type;
	
	public Action(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	public abstract void perform(NPC npc, Player player);
	
	public abstract void load(ConfigurationSection data) throws FailedToLoadException;
	
	public abstract void fromCommand(String[] data) throws InvalidCommandException;
	
	public void save(ConfigurationSection data) {
		data.set("type", getType());
	}
	
	public abstract String getDescription();
	
	public abstract String getUsage();
	
	public abstract String getData();
}

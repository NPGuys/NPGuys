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

package goldob.npguys;

import java.util.ArrayList;
import java.util.List;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

import org.bukkit.entity.Player;

import goldob.npguys.conversation.Conversation;
import goldob.npguys.conversation.ConversationManager;
import goldob.npguys.conversation.NPCMessage;
import goldob.npguys.conversation.PlayerMessage;
import goldob.npguys.exception.MessageNotFoundException;
import goldob.npguys.exception.NPGuyNotFoundException;
import goldob.npguys.requirement.Requirement;


public class NPGuy extends Trait {
	private String uid;
	private List<Conversation> heldConversations;
	
	public NPGuy() {
		super("npguy");
		heldConversations = new ArrayList<Conversation>();
	}
	
	@Override
	public void load(DataKey key) {
		uid = key.getString("uid");
		reload();
	}
	
	@Override
	public void save(DataKey key) {
		key.setString("uid", uid);
	}
	
	public List<PlayerMessage> getPossibleResponses(NPCMessage message, Player player) {
		List<PlayerMessage> responseList = new ArrayList<PlayerMessage>();
		
		for(String responseName : message.getPossibleResponses()) {
			try {
				PlayerMessage response = DialogueManager.getPlayerMessage(uid, responseName);
				if (areRequirementsMet(player, response)) {
					responseList.add(response);
				}
			} catch (MessageNotFoundException | NPGuyNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if(responseList.isEmpty()) {
			responseList.add(DialogueManager.getExitMessage());
		}
		
		return responseList;
	}

	private boolean areRequirementsMet(Player player, PlayerMessage response) {
		for (Requirement requirement : response.getRequirements()) {
			if (requirement.isMet(getNPC(), player) == requirement.isReversed()) {
				return false;
			}
		}
		return true;
	}
	
	public List<Conversation> getHeldConversations() {
		return heldConversations;
	}
	
	public String getUID() {
		return uid;
	}

	public void setUID(String uid) {
		this.uid = uid;
		reload();
	}
	
	public boolean isActive() {
		try {
			return DialogueManager.isActive(uid);
		} catch (NPGuyNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void reload() {
		ConversationManager.endConversations(this);
	}
}

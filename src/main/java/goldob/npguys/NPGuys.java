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

import net.citizensnpcs.api.trait.TraitInfo;

import org.bukkit.plugin.java.JavaPlugin;

import goldob.npguys.action.AbandonConversation;
import goldob.npguys.action.ForceCommand;
import goldob.npguys.action.GivePermission;
import goldob.npguys.action.RunCommand;
import goldob.npguys.action.TakePermission;
import goldob.npguys.action.quest.BeginQuest;
import goldob.npguys.action.quest.CompleteObjectives;
import goldob.npguys.action.quest.CompleteQuest;
import goldob.npguys.commands.NPGuysCommands;
import goldob.npguys.conversation.ConversationManager;
import goldob.npguys.editor.DialogueEditor;
import goldob.npguys.requirement.MinimumLevel;
import goldob.npguys.requirement.RequiredPermission;
import goldob.npguys.requirement.RequiredSkill;
import goldob.npguys.requirement.heroes.RequiredHeroClass;
import goldob.npguys.requirement.quest.ActiveObjectives;
import goldob.npguys.requirement.quest.CompletedObjectives;
import goldob.npguys.requirement.quest.FinishedQuest;
import goldob.npguys.requirement.quest.PerformedQuest;
import goldob.npguys.requirement.vault.MinimumMoney;
import goldob.npguys.ui.impl.BossUI;
import goldob.npguys.ui.impl.ScoreboardUI;

public class NPGuys extends JavaPlugin {
	private static NPGuys plugin = null;
	
	@Override
	public void onEnable() {
		super.onEnable();
		
		plugin = this;
		
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		
		ElementManager.init(this);
		DialogueManager.init(this);
		ConversationManager.init(this);
		
		DialogueEditor.init(this);
		
		ElementManager.registerUI("SCOREBOARD", ScoreboardUI.class);
		ElementManager.registerUI("BOSS_HEALTHBAR", BossUI.class);
		
		ElementManager.registerAction("RUN_COMMAND", RunCommand.class);
		ElementManager.registerAction("FORCE_COMMAND", ForceCommand.class);
		ElementManager.registerAction("GIVE_PERMISSION", GivePermission.class);
		ElementManager.registerAction("TAKE_PERMISSION", TakePermission.class);
		ElementManager.registerAction("BEGIN_QUEST", BeginQuest.class);
		ElementManager.registerAction("FINISH_QUEST", CompleteQuest.class);
		ElementManager.registerAction("COMPLETE_OBJECTIVES", CompleteObjectives.class);	
		ElementManager.registerAction("ABANDON_CONVERSATION", AbandonConversation.class);
		
		ElementManager.registerRequirement("PERMISSION", RequiredPermission.class);
		ElementManager.registerRequirement("MIN_LEVEL", MinimumLevel.class);
		ElementManager.registerRequirement("PERFORMED_QUEST", PerformedQuest.class);
		ElementManager.registerRequirement("FINISHED_QUEST", FinishedQuest.class);
		ElementManager.registerRequirement("ACTIVE_OBJECTIVES", ActiveObjectives.class);
		ElementManager.registerRequirement("COMPLETED_OBJECTIVES", CompletedObjectives.class);
		ElementManager.registerRequirement("HEROCLASS", RequiredHeroClass.class);
		ElementManager.registerRequirement("SKILL", RequiredSkill.class);
		ElementManager.registerRequirement("MIN_MONEY", MinimumMoney.class);
		
		getCommand("npguy").setExecutor(DialogueEditor.getCommandHandler());
		getCommand("npg").setExecutor(DialogueEditor.getCommandHandler());
		getCommand("dialogue").setExecutor(DialogueEditor.getCommandHandler());
		getCommand("npguys").setExecutor(new NPGuysCommands());
		
		ElementManager.getCitizens().getTraitFactory().registerTrait(TraitInfo.create(NPGuy.class).withName("npguy"));
	}
	
	public static NPGuys getPlugin() {
		return plugin;
	}
	
	@Override
	public void onDisable() {
		DialogueManager.saveAll();
		ConversationManager.endAll();
	}
	
}

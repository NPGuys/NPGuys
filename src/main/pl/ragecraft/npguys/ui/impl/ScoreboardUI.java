package pl.ragecraft.npguys.ui.impl;

import java.util.List;

import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import pl.ragecraft.npguys.NPGuys;
import pl.ragecraft.npguys.conversation.Conversation;
import pl.ragecraft.npguys.conversation.PlayerMessage;
import pl.ragecraft.npguys.ui.ConversationUI;

public class ScoreboardUI extends ConversationUI {
	private static String headline;
	private static boolean useScrollback;
	private static long npcDelay;
	private static String playerMessageFormat;
	private static String npcMessageFormat;
	
	private boolean ignoreEvents;
	private BukkitTask task;
	
	public ScoreboardUI(Conversation conversation) {
		super(conversation);
		ignoreEvents = true;
		task = null;
	}
	
	@Override
	public void init(ConfigurationSection config) {
		headline = config.getString("headline");
		useScrollback = config.getBoolean("use-scrollback");
		npcDelay = config.getLong("npc.delay");
		playerMessageFormat = config.getString("player.message_format");
		npcMessageFormat = config.getString("npc.message_format");
	}
	
	@Override
	public void openView() {
		ignoreEvents = true;
		
		final Conversation conversation = getConversation();
		String playerMsg = playerMessageFormat;
		playerMsg = playerMsg.replaceAll("%msg", conversation.getDisplayedMessage().getMessage());
		playerMsg = playerMsg.replaceAll("%player", conversation.getPlayer().getName());
		playerMsg = playerMsg.replaceAll("%npc", conversation.getNPGuy().getNPC().getName());
		playerMsg = playerMsg.replace('&', '�');
		conversation.getPlayer().sendMessage(playerMsg);
		
		String npcMsg = npcMessageFormat;
		npcMsg = npcMsg.replaceAll("%msg", conversation.getDisplayedMessage().getNPCMessage().getMessage());
		npcMsg = npcMsg.replaceAll("%player", conversation.getPlayer().getName());
		npcMsg = npcMsg.replaceAll("%npc", conversation.getNPGuy().getNPC().getName());
		npcMsg = npcMsg.replace('&', '�');
		final String final_npcMsg = npcMsg;
		
		task = Bukkit.getScheduler().runTaskLater(NPGuys.getPlugin(), new Runnable() {
			@Override
			public void run() {
				ignoreEvents = false;
				conversation.getPlayer().sendMessage(final_npcMsg);
				updateView();
			}
		}, npcDelay);
	}
	
	@Override
	public void updateView() {
		Scoreboard view = Bukkit.getScoreboardManager().getNewScoreboard();
		Conversation conversation = getConversation();
		view.registerNewObjective(ChatColor.UNDERLINE+headline, "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
		
		List<PlayerMessage> possibleResponses = conversation.getPossibleResponses();
		int id = possibleResponses.size();
		
		for(PlayerMessage response : possibleResponses) {
			StringBuilder line = new StringBuilder();
			if(possibleResponses.size()-id+1 == conversation.getChoosenResponse()) {
				line.append(" > ");
			}
			else {
				line.append("   ");
			}
			if (response.getShortcut().length() <= 13) {
				line.append(response.getShortcut());
			}
			else {
				line.append(response.getShortcut().substring(0, 13));
			}
			view.getObjective(DisplaySlot.SIDEBAR).getScore(Bukkit.getOfflinePlayer(line.toString())).setScore(id);
			id--;
		}
		conversation.getPlayer().setScoreboard(view);
	}

	@Override
	public void closeView() {
		task.cancel();
		getConversation().getPlayer().getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
	}
	
	private void changeResponse(boolean reversed) {
		Conversation conversation = getConversation();
		int choosenResponse = conversation.getChoosenResponse();
		if (!reversed) {
			if (choosenResponse < conversation.getPossibleResponses().size()) {
				choosenResponse++;
			} else {
				choosenResponse = 1;
			}
		}
		else {
			if (choosenResponse > 1) {
				choosenResponse--;
			} else {
				choosenResponse = conversation.getPossibleResponses().size();
			}
		}
		conversation.changeResponse(choosenResponse);
	}
	
	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent event) {
		if(ignoreEvents || !checkPlayer(event.getPlayer())) 
			return;
		
		Conversation conversation = getConversation();
		if (useScrollback) {
			int oldSlot = event.getPreviousSlot();
			int newSlot = event.getNewSlot();
			
			Location playerLoc = conversation.getPlayer().getEyeLocation();
			Location npcLoc = conversation.getNPGuy().getNPC().getBukkitEntity().getEyeLocation();
			// Checks if player looks at npc face (we don't want to talk to dirt)
			Vector toCenter = new Vector(npcLoc.getX()-playerLoc.getX(), npcLoc.getY()-playerLoc.getY(), npcLoc.getZ()-playerLoc.getZ());
			Vector direction = playerLoc.getDirection();
			if (direction.angle(toCenter) < Math.atan(0.5/playerLoc.distance(npcLoc))) {
				if (newSlot == oldSlot+1 || (newSlot == 0 && oldSlot == 8)) {
					changeResponse(false);
				}
				if (newSlot == oldSlot-1 || (newSlot == 8 && oldSlot == 0)) {
					changeResponse(true);
				}
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onRightClick(NPCRightClickEvent event) {
		if(ignoreEvents || !checkPlayer(event.getClicker()) || !checkNPC(event.getNPC())) 
			return;
		
		changeResponse(false);
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onLeftClick(NPCLeftClickEvent event) {
		if(ignoreEvents || !checkPlayer(event.getClicker()) || !checkNPC(event.getNPC()))
			return;
		
		getConversation().continueConversation();
		event.setCancelled(true);
	}
}
package pl.ragecraft.npguys;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import pl.ragecraft.npguys.action.Action;
import pl.ragecraft.npguys.conversation.Conversation;
import pl.ragecraft.npguys.conversation.ConversationManager;
import pl.ragecraft.npguys.conversation.NPCMessage;
import pl.ragecraft.npguys.conversation.PlayerMessage;
import pl.ragecraft.npguys.requirement.Requirement;


public class NPGuyManager {
	private static NPGuys plugin = null;
	private static File npcs;
	private static Map<String, NPGuyData> npguys = new HashMap<String, NPGuyData>();
	private static Map<String, Class<? extends Action>> actions = new HashMap<String, Class<? extends Action>>();
	private static Map<String, Class<? extends Requirement>> requirements = new HashMap<String, Class<? extends Requirement>>();
	
	public static void init(final NPGuys plugin) {
		NPGuyManager.plugin = plugin;
		
		npcs = new File(plugin.getDataFolder(), "npc");
		if (!npcs.exists()) {
			npcs.mkdir();
		}
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				for (File npc : npcs.listFiles()) {
					load(npc.getName().replaceAll(".yml", ""), YamlConfiguration.loadConfiguration(npc));
				}
				plugin.getLogger().log(Level.INFO, "Data loaded...");
			}
		}, 1);
		
		plugin.getServer().getPluginManager().registerEvents(new EventListener(), plugin);
	}
	
	private static void load(String npguy, YamlConfiguration data) {
		NPGuyData toLoad = new NPGuyData();
		//TODO Handle exceptions
		toLoad.name = npguy;
		toLoad.welcomeMessage = data.getString("welcome_message");
		for (String messageUid : data.getConfigurationSection("dialogues").getKeys(false)) {
			String shortcut = data.getString("dialogues."+messageUid+".shortcut");
			String message = data.getString("dialogues."+messageUid+".message");
			
			List<Requirement> loadedRequirements = new ArrayList<Requirement>();
			if (data.getConfigurationSection("dialogues."+messageUid+".requirements") == null) {
				data.getConfigurationSection("dialogues."+messageUid).createSection("requirements");
			}
			for (String key : data.getConfigurationSection("dialogues."+messageUid+".requirements").getKeys(false)) {
				ConfigurationSection requirement = data.getConfigurationSection("dialogues."+messageUid+".requirements."+key);
				
				String type = requirement.getString("type");
				if (requirements.containsKey(type)) {
					try {
						Requirement loadedRequirement = newRequirement(type);
						loadedRequirement.load(requirement);
						if(requirement.contains("reversed")) {
							loadedRequirement.setReversed(requirement.getBoolean("reversed"));
						}
						loadedRequirements.add(loadedRequirement);
					} catch (FailedToLoadException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RequirementNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					//TODO Handle exception
				}
			}
			
			List<Action> loadedActions = new ArrayList<Action>();
			if (data.getConfigurationSection("dialogues."+messageUid+".actions") == null) {
				data.getConfigurationSection("dialogues."+messageUid).createSection("actions");
			}
			for (String key : data.getConfigurationSection("dialogues."+messageUid+".actions").getKeys(false)) {
				ConfigurationSection action = data.getConfigurationSection("dialogues."+messageUid+".actions."+key);
				
				String type = action.getString("type");
				if (actions.containsKey(type)) {
					try {
						Action loadedAction = newAction(type);
						loadedAction.load(action);
						loadedActions.add(loadedAction);
					} catch (FailedToLoadException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ActionNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					//TODO Handle exception
				}
			}
			
			String npcResponse_message = data.getString("dialogues."+messageUid+".npc_response.message");
			List<String> possibleResponses = data.getStringList("dialogues."+messageUid+".npc_response.possible_responses");
			NPCMessage npcResponse = new NPCMessage(npcResponse_message, possibleResponses);
			
			PlayerMessage loadedMessage = new PlayerMessage(shortcut, message, npcResponse, loadedRequirements, loadedActions);
			toLoad.dialogues.put(messageUid, loadedMessage);
		}
		npguys.put(npguy, toLoad);
	}
	
	private static void save(String npguy, YamlConfiguration data) {
		NPGuyData toSave = npguys.get(npguy);
		data.set("welcome_message", toSave.welcomeMessage);
		for (String uid : toSave.dialogues.keySet()) {
			ConfigurationSection savedMessage = data.createSection("dialogues."+uid);
			PlayerMessage messageToSave = toSave.dialogues.get(uid);
			
			savedMessage.set("shortcut", messageToSave.getShortcut());
			savedMessage.set("message", messageToSave.getMessage());
			
			savedMessage.createSection("requirements");
			int i = 0;
			for (Requirement requirement : messageToSave.getRequirements()) {
				ConfigurationSection savedRequirement = savedMessage.createSection("requirements."+String.valueOf(i));
				requirement.save(savedRequirement);
				i++;
			}
			
			savedMessage.createSection("actions");
			i = 0;
			for (Action action : messageToSave.getActions()) {
				ConfigurationSection savedAction = savedMessage.createSection("actions."+String.valueOf(i));
				action.save(savedAction);
				i++;
			}
			
			ConfigurationSection savedResponse = savedMessage.createSection("npc_response");
			savedResponse.set("message", messageToSave.getNPCMessage().getMessage());
			savedResponse.set("possible_responses", messageToSave.getNPCMessage().getPossibleResponses());
		}
	}
	
	public static void saveAll() {
		for (File file : npcs.listFiles()) {
			file.delete();
		}
		for(String toSave : npguys.keySet()) {
			File output = new File(npcs, toSave+".yml");
			if(output.exists()) {
				output.delete();
			}
			try {
				output.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			YamlConfiguration data = YamlConfiguration.loadConfiguration(output);
			save(toSave, data);
			
			try {
				data.save(output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		plugin.getServer().getLogger().log(Level.INFO, "Dialogues saved.");
	}
	
	public static NPGuyData getData(String npguy) throws NPGuyNotFoundException {
		if (!(npguys.containsKey(npguy))) {
			throw new NPGuyNotFoundException(npguy);
		}
		else {
			return npguys.get(npguy);
		}
	}
	
	public static void removeData(String npguy) throws NPGuyNotFoundException {
		if (!(npguys.containsKey(npguy))) {
			throw new NPGuyNotFoundException(npguy);
		}
		else {
			npguys.remove(npguy);
		}
	}
	
	public static void putData(String npguy, NPGuyData data) throws NPGuyExistsException {
		if ((npguys.containsKey(npguy))) {
			throw new NPGuyExistsException(npguy);
		}
		else {
			npguys.put(npguy, data);
		}
	}
	
	public static PlayerMessage getWelcomeMessage(String npguy) {
		try {
			return getPlayerMessage(npguy, npguys.get(npguy).welcomeMessage);
		} catch (MessageNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PlayerMessage getPlayerMessage(String npguy, String uid) throws MessageNotFoundException {
		return npguys.get(npguy).dialogues.get(uid);
	}
	
	public static Action newAction(String name) throws ActionNotFoundException {
		try {
			if(actions.containsKey(name)) {
				return actions.get(name).getConstructor(String.class).newInstance(name);
			}
			else {
				throw new ActionNotFoundException(name);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void registerAction(String name, Class<? extends Action> clazz) {
		actions.put(name.toUpperCase(), clazz);
	}
	
	public static Requirement newRequirement(String name) throws RequirementNotFoundException {
		try {
			if (requirements.containsKey(name)) {
				return requirements.get(name).getConstructor(String.class).newInstance(name);
			}
			else {
				throw new RequirementNotFoundException(name);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void registerRequirement(String name, Class<? extends Requirement> clazz) {
		requirements.put(name.toUpperCase(), clazz);
	}
	
	public static PlayerMessage getDefaultMessage() {
		List<Action> actions = new ArrayList<Action>();
		List<Requirement> requirements = new ArrayList<Requirement>();
		List<String> possibleResponses = new ArrayList<String>();
		NPCMessage response = new NPCMessage(plugin.getConfig().getString("dialogues.default.npc_response.message"), possibleResponses);
		return new PlayerMessage(plugin.getConfig().getString("dialogues.default.shortcut"), plugin.getConfig().getString("dialogues.default.message"), response, requirements, actions);
	}
	
	public static PlayerMessage getExitMessage() {
		List<Action> actions = new ArrayList<Action>();
		try {
			actions.add(newAction("ABANDON_CONVERSATION"));
		} catch (ActionNotFoundException e) {
			e.printStackTrace();
		}
		List<Requirement> requirements = new ArrayList<Requirement>();
		List<String> possibleResponses = new ArrayList<String>();
		NPCMessage response = new NPCMessage(plugin.getConfig().getString("dialogues.exit.npc_response.message"), possibleResponses);
		return new PlayerMessage(plugin.getConfig().getString("dialogues.exit.shortcut"), plugin.getConfig().getString("dialogues.exit.message"), response, requirements, actions);
	}
	
	private static class EventListener implements Listener {
		@SuppressWarnings("unused")
		@EventHandler
		public void onSlotChange(PlayerItemHeldEvent event) {
			if (plugin.getConfig().getBoolean("conversation.use-scroll")) {
				Conversation conversation = ConversationManager.getConversationByCaller(event.getPlayer());
				if (conversation != null) {
					int oldSlot = event.getPreviousSlot();
					int newSlot = event.getNewSlot();
					
					Location playerLoc = conversation.getPlayer().getEyeLocation();
					Location npcLoc = conversation.getNPC().getNPC().getBukkitEntity().getEyeLocation();
					Vector toCenter = new Vector(npcLoc.getX()-playerLoc.getX(), npcLoc.getY()-playerLoc.getY(), npcLoc.getZ()-playerLoc.getZ());
					Vector direction = playerLoc.getDirection();
					if (direction.angle(toCenter) < Math.atan(0.5/playerLoc.distance(npcLoc))) {
						if (newSlot == oldSlot+1 || (newSlot == 0 && oldSlot == 8)) {
							conversation.changeResponse(false);
						}
						if (newSlot == oldSlot-1 || (newSlot == 8 && oldSlot == 0)) {
						conversation.changeResponse(true);
						}
						event.setCancelled(true);
					}
				}
			}
		}
		
		@SuppressWarnings("unused")
		@EventHandler
		public void onRightClick(NPCRightClickEvent event) {
				Player player = event.getClicker();
				NPC npc = event.getNPC();
				if (player.getLocation().distance(npc.getBukkitEntity().getLocation()) > plugin.getConfig().getDouble("conversation.distance")) {
					return;
				}
				if (!npc.hasTrait(NPGuy.class)) {
					return;
				}
				Conversation conversation = ConversationManager.getConversationByCaller(player);
				if (conversation == null) {
					ConversationManager.beginConversation(player, npc.getTrait(NPGuy.class));
					return;
				}
				if (!plugin.getConfig().getBoolean("conversation.use-scroll")) {
					if (conversation.getNPC().getNPC().getId() == event.getNPC().getId()) {
						conversation.changeResponse(false);
					}
					else {
						ConversationManager.beginConversation(player, npc.getTrait(NPGuy.class));
					}
				}
		}
		
		@SuppressWarnings("unused")
		@EventHandler
		public void onLeftClick(NPCLeftClickEvent event) {
				Player player = event.getClicker();
				NPC npc = event.getNPC();
				if (!npc.hasTrait(NPGuy.class)) {
					return;
				}
				Conversation conversation = ConversationManager.getConversationByCaller(player);
				if (conversation != null) {
					if (conversation.getNPC().getNPC().getId() == npc.getId()) {
						conversation.continueConversation();
					}
				}
		}
		
		@SuppressWarnings("unused")
		@EventHandler
		 public void onPlayerMove(PlayerMoveEvent event) {
			Conversation conversation = ConversationManager.getConversationByCaller(event.getPlayer());
			if (conversation != null) {
				if (event.getPlayer().getLocation().distance(conversation.getNPC().getNPC().getBukkitEntity().getLocation()) > plugin.getConfig().getDouble("conversation.distance")) {
					ConversationManager.endConversation(event.getPlayer());
				}
			}
		}
	}
}

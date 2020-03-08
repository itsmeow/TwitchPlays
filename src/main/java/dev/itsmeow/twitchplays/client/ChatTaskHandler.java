package dev.itsmeow.twitchplays.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.TwitchPlays.Options;
import dev.itsmeow.twitchplays.client.task.Task;
import dev.itsmeow.twitchplays.client.task.TaskRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class ChatTaskHandler extends ListenerAdapter {

    protected static final ChatTaskHandler INSTANCE = new ChatTaskHandler();

    // Bot
    protected static PircBotX chat;
    protected static Thread botThread;

    // Session
    private static boolean sessionRunning;
    private static String sessionChannel = "";

    // Turning
    public static int turnTime;
    public static final int TURN_TIME = 10;

    // Camera
    public static float oriYaw;
    public static float oriPitch;
    public static float targetYaw;
    public static float targetPitch;

    // Democracy
    private static boolean countingVotes;
    private static int democracyTimer = Options.DEMOCRACY_TIMER * 20;
    private static HashMap<String, Integer> democracyVotes = new HashMap<String, Integer>();
    private static ArrayList<String> voters = new ArrayList<String>();

    // Tasks
    private static int tasksExecuted = 0;
    private static ArrayList<Task> tasks = new ArrayList<Task>();
    private static ArrayList<Task> instaTasks = new ArrayList<Task>();
    private static HashMap<Class<? extends Task>, Integer> taskCallTime = new HashMap<Class<? extends Task>, Integer>();

    // Misc
    private static long clock;
    private static ChatMode chatMode;

    /**
     * ######## BOT CONTROL ########
     */

    public static void initBot() {
        Configuration configuration = new Configuration.Builder()
        .setName("TwitchPlaysController")
        .addServer("irc.chat.twitch.tv", 6667)
        .setServerPassword(Options.Twitch.TOKEN)
        .addListener(INSTANCE)
        .buildConfiguration();
        chat = new PircBotX(configuration);
        botThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chat.startBot();
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(IrcException e) {
                    e.printStackTrace();
                }
            }
        });
        botThread.start();
    }

    public static void endSession() {
        if(sessionRunning) {
            sessionRunning = false;
            if(!tasks.isEmpty()) {
                tasks.get(0).terminate();
            }
            tasks.clear();
            instaTasks.clear();
            taskCallTime.clear();
            tasksExecuted = 0;
            if(chat.isConnected()) {
                chat.sendRaw().rawLine("PART #" + sessionChannel);
            }
            sessionChannel = "";
        }
    }

    public static void startSession(String streamer) {
        if(!sessionRunning) {
            if(!chat.isConnected()) {
                if(Options.Twitch.TOKEN.isEmpty()) {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("twitchplays.chat.token").setStyle(new Style().setColor(TextFormatting.RED).setItalic(true).setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://twitchapps.com/tmi/"))));
                    return;
                } else {
                    try {
                        chat.startBot();
                    } catch(IOException | IrcException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
            sessionRunning = true;

            oriPitch = targetPitch = TickHandlerClient.prevCamPitch = TickHandlerClient.camPitch = 90F;

            tasks.clear();
            instaTasks.clear();
            taskCallTime.clear();

            tasksExecuted = 0;

            sessionChannel = streamer.toLowerCase();
            chat.sendIRC().joinChannel("#" + sessionChannel);
        }
    }

    /*
     * ######## GETTERS ########
     */

    public static boolean isSessionRunning() {
        return sessionRunning;
    }

    public static String getSessionChannel() {
        return sessionChannel;
    }

    public static ChatMode getChatMode() {
        return chatMode;
    }

    public static boolean modsOnly() {
        return chatMode == ChatMode.MODS_ONLY;
    }

    public static void setChatMode(ChatMode mode) {
        ChatTaskHandler.chatMode = mode;
    }

    public static int getTasksExecuted() {
        return ChatTaskHandler.tasksExecuted;
    }

    public static ArrayList<Task> getTasks() {
        return ChatTaskHandler.tasks;
    }

    @Nullable
    public static Task getCurrentTask() {
        if(!tasks.isEmpty()) {
            return tasks.get(0);
        }
        return null;
    }

    /*
     * ######## MINECRAFT EVENTS ########
     */

    @SubscribeEvent
    public static void onWorldTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            if(mc.world != null) {
                TickHandlerClient.updateCameraAngles();

                if(clock != mc.world.getWorldTime() || !mc.world.getGameRules().getBoolean("doDaylightCycle")) {
                    TickHandlerClient.updateMinicam();
                    clock = mc.world.getWorldTime();
                    if(!tasks.isEmpty()) {
                        Task task = tasks.get(0);
                        if(!mc.player.isEntityAlive() && !task.canWorkDead()) {
                            if(!tasks.isEmpty()) {
                                tasks.remove(0);
                            }
                        } else {
                            if(task.timeActive == 0) {
                                task.world = mc.world;
                                task.player = mc.player;
                                taskCallTime.put(task.getClass(), (int) mc.world.getWorldTime());
                                task.init();
                                tasksExecuted++;
                            }
                            task.tick();
                            if(task.timeActive >= task.maxActiveTime()) {
                                task.terminate();
                                if(!tasks.isEmpty()) {
                                    tasks.remove(0);
                                }
                            }
                        }
                    }
                    for(int i = instaTasks.size() - 1; i >= 0; i--) {
                        Task task = instaTasks.get(i);
                        if(task.timeActive == 0) {
                            task.world = mc.world;
                            task.player = mc.player;
                            taskCallTime.put(task.getClass(), (int) mc.world.getWorldTime());
                            task.init();
                        }
                        task.tick();
                        if(task.timeActive >= task.maxActiveTime()) {
                            task.terminate();
                            if(!instaTasks.isEmpty()) {
                                instaTasks.remove(i);
                            }
                        }
                    }
                    if(turnTime > 0) {
                        TickHandlerClient.camYaw += (targetYaw - oriYaw) * (1F / (float) TURN_TIME);
                        TickHandlerClient.camPitch += (targetPitch - oriPitch) * (1F / (float) TURN_TIME);
                        turnTime--;
                        if(turnTime == 0) {
                            TickHandlerClient.camYaw = targetYaw;
                            TickHandlerClient.camPitch = targetPitch;
                        }
                    }
                }
                if(Options.USE_DEMOCRACY) {
                    if(democracyTimer > 0) {
                        democracyTimer--;
                        if(democracyTimer == 0) {
                            countingVotes = true;

                            boolean flag = false;
                            while(!flag) {
                                int votes = -1;
                                String task = "";
                                for(Map.Entry<String, Integer> e : democracyVotes.entrySet()) {
                                    if(e.getValue() > votes) {
                                        votes = e.getValue();
                                        task = e.getKey();
                                    }
                                }
                                if(votes != -1) {
                                    flag = parseChat(mc.world, mc.player, task, null, false, true);
                                    democracyVotes.remove(task);
                                } else {
                                    flag = true;
                                }
                            }

                            countingVotes = false;

                            democracyTimer = Options.DEMOCRACY_TIMER * 20;
                            democracyVotes.clear();
                            voters.clear();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChatEvent(ClientChatReceivedEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if(Options.USE_GAME_CHAT && sessionRunning && mc.world != null && event.getMessage() instanceof TextComponentTranslation) {
            TextComponentTranslation msg = (TextComponentTranslation) event.getMessage();
            if(msg.getKey().equals("chat.type.text") && msg.getFormatArgs().length > 1) {
                String name = "";
                boolean isPlayer = false;
                if(msg.getFormatArgs()[0] instanceof TextComponentString) {
                    name = ((TextComponentString) msg.getFormatArgs()[0]).getUnformattedText();
                    isPlayer = name.equals(mc.player.getGameProfile().getName());
                }
                String s = "";
                for(int i = 1; i < msg.getFormatArgs().length; i++) {
                    if(msg.getFormatArgs()[i] instanceof TextComponentString) {
                        s = s + ((TextComponentString) msg.getFormatArgs()[i]).getUnformattedText();
                    } else if(msg.getFormatArgs()[i] instanceof TextComponentTranslation) {
                        s = s + ((TextComponentTranslation) msg.getFormatArgs()[i]).getUnformattedText();
                    } else {
                        s = s + msg.getFormatArgs()[i].toString();
                    }
                }
                parseChat(mc.world, mc.player, s, name, isPlayer);
            }
        }
    }

    /*
     * ######## BOT LISTENERS ########
     */

    @Override
    public void onGenericMessage(final GenericMessageEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.world != null) {
            mc.addScheduledTask(() -> {
                String msg = event.getMessage();
                if(msg != null && !msg.isEmpty()) {
                    if(event.getUser() != null) {
                        String name = event.getUser().getLogin();
                        boolean isOp = Lists.newArrayList(Options.Twitch.MODERATORS).contains(name);
                        boolean isTask = parseChat(Minecraft.getMinecraft().world, Minecraft.getMinecraft().player, msg, name, isOp);
                        TextComponentString message = new TextComponentString("");
                        message.getStyle().setColor(TextFormatting.DARK_PURPLE);
                        message.appendText((isOp ? "<@" : "<") + name + "> ");
                        TextComponentString text = new TextComponentString(msg);
                        text.getStyle().setColor(isTask ? TextFormatting.GRAY : TextFormatting.WHITE);
                        message.appendSibling(text);
                        Minecraft.getMinecraft().player.sendMessage(message);
                    }
                }

            });

        }
    }

    @Override
    public void onJoin(final JoinEvent event) throws Exception {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if(event.getUser() == chat.getUserBot()) {
                if(sessionRunning && Minecraft.getMinecraft().player != null) {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("twitchplays.chat.connected").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                }
            }
        });
    }

    @Override
    public void onPart(final PartEvent event) throws Exception {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if(event.getUser() == chat.getUserBot()) {
                if(Minecraft.getMinecraft().player != null) {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation("twitchplays.chat.disconnected").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                }
            }
        });
    }

    /*
     * ######## UTIL ########
     */

    public static void addVote(String user, String task) {
        if(!voters.contains(user)) {
            voters.add(user);
            Integer votes = democracyVotes.get(task);
            if(votes == null) {
                votes = 0;
            }
            democracyVotes.put(task, votes + 1);
        }
    }

    /**
     * @return True if task is added
     */
    public static boolean parseChat(WorldClient world, EntityPlayerSP player, String s, String user, boolean isOp) {
        if(chatMode == ChatMode.MODS_ONLY && !isOp) {
            return false;
        }
        if(Options.USE_DEMOCRACY) {
            boolean isTask = parseChat(world, player, s, user, isOp, false);
            if(isTask) {
                String[] args = s.split(" ");
                String task = "";
                for(int i = 0; i < args.length; i++) {
                    if(!args[i].toLowerCase().trim().isEmpty()) {
                        task = task + args[i].toLowerCase().trim() + " ";
                        task = task.trim();
                    }
                }

                addVote(user, task);
            }

            return isTask;
        }
        return parseChat(world, player, s, user, isOp, true);
    }

    /**
     * @return True if task is added
     */
    public static boolean parseChat(WorldClient world, EntityPlayerSP player, String s, String user, boolean isOp, boolean add) {
        String[] args = s.split(" ");
        ArrayList<String> actualArgs = new ArrayList<String>();
        for(int i = 0; i < args.length; i++) {
            if(!args[i].toLowerCase().trim().isEmpty()) {
                actualArgs.add(args[i].toLowerCase().trim());
            }
        }
        int count = 1;
        if(actualArgs.isEmpty()) {
            return false;
        }
        String arg0 = actualArgs.get(0);
        try {
            int count1 = Integer.parseInt(arg0.substring(arg0.length() - 1));
            if(count1 > Options.INPUT_MAX) {
                return false;
            }
            count = count1;
            arg0 = arg0.substring(0, arg0.length() - 1);
            actualArgs.remove(0);
            actualArgs.add(0, arg0);
        } catch(NumberFormatException e) {
        }
        if(!actualArgs.isEmpty() && TaskRegistry.hasTask(actualArgs.get(0))) {
            String[] newArgs = actualArgs.toArray(new String[actualArgs.size()]);
            boolean flag = false;
            for(int i = 0; i < count; i++) {
                Task task = TaskRegistry.createTask(world, player, newArgs);
                int lastCall = Integer.MAX_VALUE;
                if(task != null && taskCallTime.containsKey(task.getClass())) {
                    lastCall = (int) world.getWorldTime() - taskCallTime.get(task.getClass());
                }
                if(task != null && task.canBeAdded(ImmutableList.copyOf(tasks), lastCall) && (task.requiresOp(newArgs) && isOp || !task.requiresOp(newArgs))) {
                    if(add || task.requiresOp(newArgs) && !countingVotes) {
                        task.setCommander(user);
                        if(task.bypassOrder(newArgs)) {
                            instaTasks.add(task);
                        } else {
                            tasks.add(task);
                        }
                    }
                    flag = true;
                    if(task.requiresOp(newArgs) && countingVotes) {
                        flag = false;
                    }
                }
            }
            return flag;
        }
        return false;
    }

    public static enum ChatMode {
        ALL,
        MODS_ONLY;
    }

}

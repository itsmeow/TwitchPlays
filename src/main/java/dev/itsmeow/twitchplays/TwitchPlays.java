package dev.itsmeow.twitchplays;

import java.util.HashMap;
import java.util.Map;

import dev.itsmeow.twitchplays.client.TickHandlerClient;
import dev.itsmeow.twitchplays.client.command.CommandTwitchPlays;
import dev.itsmeow.twitchplays.client.task.TaskRegistry;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TwitchPlays.MOD_ID, name = "TwitchPlays", version = TwitchPlays.VERSION, clientSideOnly = true)
@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TwitchPlays {

    public static final String MOD_ID = "twitchplays";
    public static final String VERSION = "@VERSION@";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TickHandlerClient.initialize();
        ClientCommandHandler.instance.registerCommand(new CommandTwitchPlays());
    }

    @Config(modid = MOD_ID)
    public static class Options {

        @Config.LangKey("twitchplays.config.minicam_enabled")
        public static boolean MINICAM_ENABLED = true;

        @Config.LangKey("twitchplays.config.minicam_scale")
        @Config.RangeInt(min = 5, max = 90)
        @Config.SlidingOption
        public static int MINICAM_SCALE = 30;

        @Config.LangKey("twitchplays.config.minicam_distance")
        @Config.RangeInt(min = 5, max = 500)
        @Config.SlidingOption
        public static int MINICAM_DISTANCE = 100;

        @Config.LangKey("twitchplays.config.use_democracy")
        public static boolean USE_DEMOCRACY = false;

        @Config.LangKey("twitchplays.config.democracy_timer")
        @Config.RangeInt(min = 1)
        public static int DEMOCRACY_TIMER = 10;

        @Config.LangKey("twitchplays.config.use_game_chat")
        public static boolean USE_GAME_CHAT = false;

        @Config.LangKey("twitchplays.config.auto_connect_channel")
        @Config.RequiresWorldRestart
        public static String AUTO_CONNECT_CHANNEL = "";

        @Config.LangKey("twitchplays.config.input_max")
        @Config.RangeInt(min = 1, max = 9)
        @Config.SlidingOption
        public static int INPUT_MAX = 1;

        @Config.LangKey("twitchplays.config.anti_mine_down")
        public static boolean ANTI_MINE_DOWN = false;

        @Config.LangKey("twitchplays.config.tasks_enabled")
        public static Map<String, Boolean> TASKS_ENABLED = new HashMap<String, Boolean>();
        static {
            for(String id : TaskRegistry.TASK_COMMAND_TO_ID_REGISTRY.values()) {
                TASKS_ENABLED.put(id, true);
            }
        }

        @Config.LangKey("twitchplays.config.category.twitch")
        public static final TwitchOptions Twitch = new TwitchOptions();

        public static class TwitchOptions {

            @Config.LangKey("twitchplays.config.twitch.token")
            @Config.RequiresMcRestart
            public String TOKEN = "";

            @Config.LangKey("twitchplays.config.twitch.moderators")
            public String[] MODERATORS = {"ist_meow"};

        }

    }

    @SubscribeEvent
    public static void onConfigUpdate(OnConfigChangedEvent event) {
        if(event.getModID().equals(TwitchPlays.MOD_ID)) {
            syncConfig();
        }
    }

    public static void syncConfig() {
        ConfigManager.sync(TwitchPlays.MOD_ID, Config.Type.INSTANCE);
    }

}

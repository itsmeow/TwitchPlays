package dev.itsmeow.twitchplays.client.task;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.itsmeow.twitchplays.TwitchPlays.Options;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskRegistry
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final HashMap<String, Class<? extends Task>> TASK_REGISTRY = new HashMap<String, Class<? extends Task>>();
    public static final HashMap<String, Class<? extends Task>> TASK_ID_REGISTRY = new HashMap<String, Class<? extends Task>>();
    public static final HashMap<String, String> TASK_COMMAND_TO_ID_REGISTRY = new HashMap<String, String>();

    public static void registerTask(String command, String id, Class<? extends Task> taskClz)
    {
        if(TASK_REGISTRY.containsKey(command))
        {
            LOGGER.warn("Already contains command: " + command);
        }
        else
        {
            TASK_REGISTRY.put(command, taskClz);
            TASK_ID_REGISTRY.put(id, taskClz);
            TASK_COMMAND_TO_ID_REGISTRY.put(command, id);
        }
    }

    public static boolean hasTask(String taskName)
    {  
        if(TASK_REGISTRY.containsKey(taskName)) {
            return Options.TASKS_ENABLED.get(TASK_COMMAND_TO_ID_REGISTRY.get(taskName));
        } else {
            return false;
        }
    }

    public static Task createTask(WorldClient world, EntityPlayerSP player, String...args)
    {
        Class<?> clz = TASK_REGISTRY.get(args[0]);

        try
        {
            Task task = (Task)clz.getConstructor(WorldClient.class, EntityPlayerSP.class).newInstance(world, player);
            if(task.parse(args))
            {
                return task;
            }
        }
        catch(Exception e)
        {
        }

        return null;
    }

    static
    {
        registerTask("forward", "move", TaskMovement.class);
        registerTask("fwd", "move", TaskMovement.class);
        registerTask("f", "move", TaskMovement.class);
        registerTask("w", "move", TaskMovement.class);

        registerTask("back", "move", TaskMovement.class);
        registerTask("bck", "move", TaskMovement.class);
        registerTask("b", "move", TaskMovement.class);
        registerTask("s", "move", TaskMovement.class);

        registerTask("left", "move", TaskMovement.class);
        registerTask("l", "move", TaskMovement.class);
        registerTask("a", "move", TaskMovement.class);

        registerTask("right", "move", TaskMovement.class);
        registerTask("r", "move", TaskMovement.class);
        registerTask("d", "move", TaskMovement.class);

        registerTask("look", "look", TaskLook.class);//look <up,down,left,right>
        registerTask("jump", "jump", TaskJump.class);//jump [forward/backward/left/right]
        registerTask("swim", "jump", TaskJump.class);
        
        registerTask("fly", "fly", TaskFly.class);//fly [up/down]

        registerTask("mine", "mine", TaskMineBlock.class);
        registerTask("hit", "hit", TaskHit.class);
        registerTask("punch", "hit", TaskHit.class);

        registerTask("hotbar", "hotbar", TaskHotbar.class);//hotbar <1-9,<,>,next,prev>

        registerTask("equip", "equip", TaskEquip.class);//equip <itemname> [meta](minecraft:stick...etc)
        registerTask("hold", "equip", TaskEquip.class);

        registerTask("respawn", "respawn", TaskRespawn.class);

        registerTask("drop", "drop", TaskDrop.class);
        registerTask("q", "drop", TaskDrop.class);

        registerTask("sneak", "sneak", TaskToggleSneak.class);
        registerTask("crouch", "sneak", TaskToggleSneak.class);

        registerTask("uncrouch", "sneak", TaskUnSneak.class);
        registerTask("unsneak", "sneak", TaskUnSneak.class);

        registerTask("closegui", "closegui", TaskCloseGui.class);

        registerTask("place", "place", TaskPlaceBlock.class);

        // TODO
        //registerTask("craft", "craft", TaskCraft.class);// craft <itemname> [meta](default 2x2 grid. Be within range [4x4x4] of a crafting table to use a 3x3 grid)

        registerTask("interact", "interact", TaskInteract.class);
        registerTask("use", "interact", TaskInteract.class);
        registerTask("inventory", "inventory", TaskOpenInventory.class);
        registerTask("e", "inventory", TaskOpenInventory.class);

        registerTask("pause", "pause", TaskOpenPauseMenu.class);
        
        // too powerful...
        //registerTask("exitgame", "exitgame", TaskExitGame.class);
        
        // mod/op only tasks
        registerTask("cleartasks", "cleartasks", TaskClearTasks.class);
        registerTask("endtask", "endtask", TaskEndTask.class);

        registerTask("endsession", "endsession", TaskEndSession.class);

        registerTask("togglethirdperson", "togglethirdperson", TaskToggleThirdPerson.class);

        registerTask("twitchinput", "twitchinput", TaskToggleTwitchInput.class);// twitchinput [mods/all]

        registerTask("command", "command", TaskCommand.class);// command <normal ingame command>
    }

    //TODO mount/dismount, craft, smelt, interact, mine/attack/place/interact, etc, democracy/anarchy
}

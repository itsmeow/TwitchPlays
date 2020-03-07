package dev.itsmeow.twitchplays.client.task;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public abstract class Task {
    public int timeActive;

    public WorldClient world;
    public EntityPlayerSP player;

    private String commander;

    public Task(WorldClient world, EntityPlayerSP player) {
        this.world = world;
        this.player = player;
    }

    /**
     * @param args - This includes the first arg as the command name.
     * @return false if arg count isn't something generally accepted and then the task won't be created.
     */
    public abstract boolean parse(String... args);

    /**
     * Called when the task finally starts ticking.
     */
    public void init() {
    }

    /**
     * Called when the task ends.
     */
    public void terminate() {
    }

    /**
     * You have to check both tasks list if it hasn't been called yet, and time since last called (can be negative if world clock was changed).
     * Integer.MAX_VALUE if it has never been called.
     */
    public boolean canBeAdded(ImmutableList<Task> tasks, int timeSinceLastTriggered) {
        return true;
    }

    public boolean requiresOp(String... args) {
        return false;
    }

    public boolean bypassOrder(String... args) {
        return false;
    }

    public abstract int maxActiveTime();

    protected abstract void update();

    public abstract String getName();
    
    public String getDisplayName() {
        return getName();
    }

    public void tick() {
        update();
        timeActive++;
    }

    public boolean canWorkDead() {
        return false;
    }

    public void setCommander(String s) {
        commander = s;
    }

    public String getCommander() {
        return commander;
    }
}

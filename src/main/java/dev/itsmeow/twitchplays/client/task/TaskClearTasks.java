package dev.itsmeow.twitchplays.client.task;

import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskClearTasks extends Task {

    public TaskClearTasks(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        if(!ChatTaskHandler.getTasks().isEmpty()) {
            ChatTaskHandler.getCurrentTask().terminate();
        }
        ChatTaskHandler.getTasks().clear();
    }

    @Override
    public boolean requiresOp(String... args) {
        return true;
    }

    @Override
    public boolean bypassOrder(String... args) {
        return true;
    }

    @Override
    public boolean parse(String... args) {
        return args.length == 1;
    }

    @Override
    public int maxActiveTime() {
        return 0;
    }

    @Override
    protected void update() {

    }

    @Override
    public boolean canWorkDead() {
        return true;
    }

    @Override
    public String getName() {
        return "cleartasks";
    }
}

package dev.itsmeow.twitchplays.client.task;

import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import dev.itsmeow.twitchplays.client.ChatTaskHandler.ChatMode;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskToggleTwitchInput extends Task {
    public boolean setOp;

    public TaskToggleTwitchInput(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        ChatTaskHandler.setChatMode(setOp ? ChatMode.MODS_ONLY : ChatMode.ALL);
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
        if(args.length == 2) {
            if(args[1].equals("op") || args[1].equals("ops") || args[1].equals("mod") || args[1].equals("mods")) {
                setOp = true;
                return true;
            } else if(args[1].equals("all")) {
                setOp = false;
                return true;
            }
        } else if(args.length == 1) {
            setOp = ChatTaskHandler.getChatMode() != ChatMode.MODS_ONLY;
        }
        return false;
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
        return "twitchinput";
    }
}

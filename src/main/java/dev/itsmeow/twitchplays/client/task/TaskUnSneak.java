package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskUnSneak extends Task {
    public TaskUnSneak(WorldClient world, EntityPlayerSP playerSP) {
        super(world, playerSP);
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
    public boolean bypassOrder(String... args) {
        return true;
    }

    @Override
    public void init() {
        Minecraft.getMinecraft().gameSettings.keyBindSneak.pressed = false;
    }

    @Override
    public String getName() {
        return "sneak";
    }
    
    @Override
    public String getDisplayName() {
        return "unsneak";
    }

    @Override
    protected void update() {
        
    }
}

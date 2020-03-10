package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskToggleSneak extends Task {
    public TaskToggleSneak(WorldClient world, EntityPlayerSP playerSP) {
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
        Minecraft.getMinecraft().gameSettings.keyBindSneak.pressed = !Minecraft.getMinecraft().gameSettings.keyBindSneak.pressed;
    }

    @Override
    protected void update() {

    }

    @Override
    public String getName()
    {
        return "sneak";
    }
}

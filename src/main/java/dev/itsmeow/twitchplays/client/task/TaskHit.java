package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskHit extends Task {

    public TaskHit(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.setIngameFocus();
        mc.inGameHasFocus = true;
        mc.gameSettings.keyBindAttack.pressed = true;
        mc.clickMouse();
    }

    @Override
    public void terminate() {
        Minecraft.getMinecraft().gameSettings.keyBindAttack.pressed = false;
    }

    @Override
    public boolean parse(String... args) {
        return args.length == 1;
    }

    @Override
    public int maxActiveTime() {
        return 1;
    }

    @Override
    protected void update() {
    }

    @Override
    public String getName() {
        return "mine";
    }
}

package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.WorldClient;

public class TaskOpenInventory extends Task {
    public TaskOpenInventory(WorldClient world, EntityPlayerSP playerSP) {
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
        Minecraft.getMinecraft().displayGuiScreen(new GuiInventory(Minecraft.getMinecraft().player));
    }

    @Override
    protected void update() {

    }

    @Override
    public String getName() {
        return "inventory";
    }
}

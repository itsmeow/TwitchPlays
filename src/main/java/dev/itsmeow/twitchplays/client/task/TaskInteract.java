package dev.itsmeow.twitchplays.client.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;

public class TaskInteract extends Task {
    public BlockPos pos;

    public TaskInteract(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.setIngameFocus();
        mc.inGameHasFocus = true;
        Minecraft.getMinecraft().rightClickMouse();
    }

    @Override
    public void terminate() {
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
        return "interact";
    }
}

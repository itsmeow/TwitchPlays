package dev.itsmeow.twitchplays.client.task;

import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class TaskEndSession extends Task {

    public TaskEndSession(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        player.sendMessage(new TextComponentTranslation("twitchplays.command.ended", ChatTaskHandler.getSessionChannel()).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
        ChatTaskHandler.endSession();
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
        return "endsession";
    }
}

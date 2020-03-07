package dev.itsmeow.twitchplays.client.task;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TaskMovement extends Task {
    public MoveType moveType;

    public double startX;
    public double startY;
    public double startZ;

    public TaskMovement(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 1) {
            return (moveType = MoveType.getType(args[0])) != null;
        }
        return false;
    }

    @Override
    public void init() {
        startX = player.posX;
        startY = player.posY;
        startZ = player.posZ;
    }

    @Override
    public int maxActiveTime() {
        return 5;
    }

    @Override
    public void update() {
        if(player.rotationYaw < 0) {
            player.rotationYaw -= (player.rotationYaw - 45F) % 90F + 45F;
        } else {
            player.rotationYaw -= (player.rotationYaw + 45F) % 90F - 45F;
        }
        if(player.getDistance(startX, player.posY, startZ) > 0.975D) {
            this.timeActive = 5; // terminate task
        }

    }

    @Override
    public String getName() {
        return moveType.getPrimaryName();
    }

    @SubscribeEvent
    public static void onInputUpdate(InputUpdateEvent event) {
        Task currTask = ChatTaskHandler.getCurrentTask();
        if(currTask instanceof TaskMovement) {
            TaskMovement moveTask = (TaskMovement) currTask;
            if(moveTask.moveType.isForwardBackwardAxis()) {
                event.getMovementInput().moveForward = 0.0F;
            } else {
                event.getMovementInput().moveStrafe = 0.0F;
            }

            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            if(!settings.keyBindForward.isKeyDown() && moveTask.moveType == MoveType.FOWARD) {
                ++event.getMovementInput().moveForward;
                event.getMovementInput().forwardKeyDown = true;
            }
            if(!settings.keyBindBack.isKeyDown() && moveTask.moveType == MoveType.BACK) {
                --event.getMovementInput().moveForward;
                event.getMovementInput().backKeyDown = true;
            }

            if(!settings.keyBindLeft.isKeyDown() && moveTask.moveType == MoveType.LEFT) {
                ++event.getMovementInput().moveStrafe;
                event.getMovementInput().leftKeyDown = true;
            }

            if(!settings.keyBindRight.isKeyDown() && moveTask.moveType == MoveType.RIGHT) {
                --event.getMovementInput().moveStrafe;
                event.getMovementInput().rightKeyDown = true;
            }
        }
    }

    public static enum MoveType {
        FOWARD(true, "forward", "fwd", "f", "w"),
        BACK(true, "backward", "back", "bck", "b", "s"),
        LEFT(false, "left", "l", "a"),
        RIGHT(false, "right", "r", "d");

        private List<String> validNames;
        private String primaryName;
        private boolean isForwardBack;

        private MoveType(boolean isForwardBack, String... names) {
            this.validNames = Lists.newArrayList(names);
            this.primaryName = names[0];
            this.isForwardBack = isForwardBack;
        }

        public String getPrimaryName() {
            return this.primaryName;
        }

        public ImmutableList<String> getNames() {
            return ImmutableList.copyOf(this.validNames);
        }

        public boolean isForwardBackwardAxis() {
            return this.isForwardBack;
        }

        public boolean isValid(String name) {
            return validNames.contains(name);
        }

        @Nullable
        public static MoveType getType(String name) {
            for(MoveType type : MoveType.values()) {
                if(type.isValid(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}

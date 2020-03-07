package dev.itsmeow.twitchplays.client.task;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.TwitchPlays.Options;
import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import dev.itsmeow.twitchplays.client.TickHandlerClient;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.MathHelper;

public class TaskCamera extends Task {
    public CameraMoveType moveType;

    public int camDist;
    public int camSize;

    public TaskCamera(WorldClient world, EntityPlayerSP player) {
        super(world, player);
        camDist = -1;
        camSize = -1;
    }

    @Override
    public void init() {
        if(moveType != null) {
            switch(moveType) {
            case UP: {
                ChatTaskHandler.targetPitch += 90F;
                break;
            }
            case DOWN: {
                ChatTaskHandler.targetPitch -= 90F;
                break;
            }
            case LEFT: {
                ChatTaskHandler.targetYaw -= 90F;
                break;
            }
            case RIGHT: {
                ChatTaskHandler.targetYaw += 90F;
                break;
            }
            }
            ChatTaskHandler.targetPitch = MathHelper.clamp(ChatTaskHandler.targetPitch, -90F, 90F);
            ChatTaskHandler.turnTime = ChatTaskHandler.TURN_TIME;
            ChatTaskHandler.oriYaw = TickHandlerClient.camYaw;
            ChatTaskHandler.oriPitch = TickHandlerClient.camPitch;
        } else {
            if(camSize != -1) {
                Options.MINICAM_SCALE = camSize;
                TwitchPlays.syncConfig();
            }
            if(camDist != -1) {
                Options.MINICAM_DISTANCE = camDist;
                TwitchPlays.syncConfig();
            }
        }
    }

    @Override
    public boolean requiresOp(String... args) {
        return args.length == 3 && args[1].equals("size");
    }

    @Override
    public boolean bypassOrder(String... args) {
        return args.length == 3 && (args[1].equals("dist") || args[1].equals("distance") || args[1].equals("size"));
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 2) {
            return (moveType = CameraMoveType.getType(args[1])) != null;
        } else if(args.length == 3) {
            if((args[1].equals("dist") || args[1].equals("distance"))) {
                try {
                    camDist = MathHelper.clamp(Integer.parseInt(args[2]), 5, 500);
                    return true;
                } catch(NumberFormatException e) {
                    return false;
                }
            } else if(args[1].equals("size")) {
                try {
                    camSize = MathHelper.clamp(Integer.parseInt(args[2]), 5, 90);
                    return true;
                } catch(NumberFormatException e) {
                    return false;
                }
            }
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
        return "camera " + moveType != null ? moveType.getPrimaryName() : "up";
    }

    public static enum CameraMoveType {
        UP(true, "up", "u"),
        DOWN(true, "down", "d"),
        LEFT(false, "left", "l"),
        RIGHT(false, "right", "r");

        private List<String> validNames;
        private String primaryName;
        private boolean isPitch;

        private CameraMoveType(boolean isPitch, String... names) {
            this.validNames = Lists.newArrayList(names);
            this.primaryName = names[0];
            this.isPitch = isPitch;
        }

        public String getPrimaryName() {
            return this.primaryName;
        }

        public ImmutableList<String> getNames() {
            return ImmutableList.copyOf(this.validNames);
        }

        public boolean isPitch() {
            return this.isPitch;
        }

        public boolean isValid(String name) {
            return validNames.contains(name);
        }

        @Nullable
        public static CameraMoveType getType(String name) {
            for(CameraMoveType type : CameraMoveType.values()) {
                if(type.isValid(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}

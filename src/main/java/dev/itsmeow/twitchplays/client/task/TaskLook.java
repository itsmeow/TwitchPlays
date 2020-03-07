package dev.itsmeow.twitchplays.client.task;

import dev.itsmeow.twitchplays.client.task.TaskCamera.CameraMoveType;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.MathHelper;

public class TaskLook extends Task {
    public CameraMoveType moveType;

    public float targetYaw;
    public float targetPitch;

    public float oriYaw;
    public float oriPitch;

    public TaskLook(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        while(player.rotationYaw < 360F) {
            player.prevRotationYaw += 360F;
            player.rotationYaw += 360F;
        }
        targetYaw = oriYaw = player.rotationYaw;
        targetPitch = oriPitch = player.rotationPitch;

        if(moveType.isPitch()) {
            player.rotationPitch += 90F;
            targetPitch = MathHelper.clamp(player.rotationPitch - ((player.rotationPitch + 11.25F) % 22.5F - 11.25F) + (moveType == CameraMoveType.UP ? -22.5F : 22.5F) - 90F, -90F, 90F);
            player.rotationPitch -= 90F;
        } else {
            targetYaw = player.rotationYaw - ((player.rotationYaw + 45F) % 90F - 45F) + (moveType == CameraMoveType.LEFT ? -90F : 90F);
        }
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 2) {
            return (moveType = CameraMoveType.getType(args[1])) != null;
        }
        return false;
    }

    @Override
    public int maxActiveTime() {
        return 10;
    }

    @Override
    protected void update() {
        player.rotationYaw += (targetYaw - oriYaw) * (1F / (float) maxActiveTime());
        player.rotationPitch += (targetPitch - oriPitch) * (1F / (float) maxActiveTime());
        player.rotationPitch = MathHelper.clamp(player.rotationPitch, -90F, 90F);
    }

    @Override
    public String getName() {
        return "look " + moveType.getPrimaryName();
    }

    @Override
    public void terminate() {
        player.rotationYaw = targetYaw;
        player.rotationPitch = targetPitch;
    }
}

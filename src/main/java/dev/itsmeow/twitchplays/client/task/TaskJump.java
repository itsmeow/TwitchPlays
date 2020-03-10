package dev.itsmeow.twitchplays.client.task;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TaskJump extends Task {
    public JumpType jumpType;

    public double startX;
    public double startY;
    public double startZ;

    public TaskJump(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 1) {
            jumpType = JumpType.UP;
            return true;
        } else if(args.length == 2) {
            return (jumpType = JumpType.getType(args[1])) != null && jumpType != JumpType.UP;
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
        return (player.isInWater() && player.isEntityAlive() && timeActive < 600) ? timeActive + 2 : jumpType == JumpType.UP ? 10 : 20;
    }

    @Override
    public void update() {
        if(player.rotationYaw < 0) {
            player.rotationYaw -= (player.rotationYaw - 45F) % 90F + 45F;
        } else {
            player.rotationYaw -= (player.rotationYaw + 45F) % 90F - 45F;
        }
        if(!player.isInWater() && player.getDistance(startX, player.posY, startZ) > 0.975D) {
            this.timeActive = 21; // terminate task
        }
    }

    @Override
    public String getName() {
        return "jump" + (jumpType != JumpType.UP ? " " + jumpType.getPrimaryName() : "");
    }

    @SubscribeEvent
    public static void onInputUpdate(InputUpdateEvent event) {
        Task currTask = ChatTaskHandler.getCurrentTask();
        if(currTask instanceof TaskJump) {
            TaskJump jumpTask = (TaskJump) currTask;
            if(jumpTask.jumpType.isForwardBackwardAxis()) {
                event.getMovementInput().moveForward = 0.0F;
            } else if(jumpTask.jumpType != JumpType.UP) {
                event.getMovementInput().moveStrafe = 0.0F;
            }

            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            if(!settings.keyBindForward.isKeyDown() && jumpTask.jumpType == JumpType.FOWARD) {
                ++event.getMovementInput().moveForward;
                event.getMovementInput().forwardKeyDown = true;
            }
            if(!settings.keyBindBack.isKeyDown() && jumpTask.jumpType == JumpType.BACK) {
                --event.getMovementInput().moveForward;
                event.getMovementInput().backKeyDown = true;
            }

            if(!settings.keyBindLeft.isKeyDown() && jumpTask.jumpType == JumpType.LEFT) {
                ++event.getMovementInput().moveStrafe;
                event.getMovementInput().leftKeyDown = true;
            }

            if(!settings.keyBindRight.isKeyDown() && jumpTask.jumpType == JumpType.RIGHT) {
                --event.getMovementInput().moveStrafe;
                event.getMovementInput().rightKeyDown = true;
            }
            if(!Minecraft.getMinecraft().gameSettings.keyBindJump.isKeyDown()) {
                event.getMovementInput().jump = true;
            }
        }
        if(currTask instanceof TaskPlaceBlock) {
            if(Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = Minecraft.getMinecraft().objectMouseOver.getBlockPos();

                IBlockState state = Minecraft.getMinecraft().world.getBlockState(pos);

                EnumFacing side = Minecraft.getMinecraft().objectMouseOver.sideHit;

                if(state == Blocks.SNOW_LAYER && Minecraft.getMinecraft().world.getBlockState(pos).getValue(BlockSnow.LAYERS) == 8) {
                    side = EnumFacing.UP;
                } else if(state != Blocks.VINE && state != Blocks.TALLGRASS && state != Blocks.DEADBUSH && !state.getBlock().isReplaceable(Minecraft.getMinecraft().world, pos)) {
                    pos = pos.offset(side);
                }
                if(!Minecraft.getMinecraft().world.checkNoEntityCollision(new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) {
                    event.getMovementInput().jump = true;
                }
            }
            
        }
    }

    public static enum JumpType {
        UP(false, "up"),
        FOWARD(true, "forward", "fwd", "f", "w"),
        BACK(true, "backward", "back", "bck", "b", "s"),
        LEFT(false, "left", "l", "a"),
        RIGHT(false, "right", "r", "d");

        private List<String> validNames;
        private String primaryName;
        private boolean isForwardBack;

        private JumpType(boolean isForwardBack, String... names) {
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
        public static JumpType getType(String name) {
            for(JumpType type : JumpType.values()) {
                if(type.isValid(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}

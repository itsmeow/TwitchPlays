package dev.itsmeow.twitchplays.client.task;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import dev.itsmeow.twitchplays.TwitchPlays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TaskFly extends Task {
    public FlyType flyType;

    public double startX;
    public double startY;
    public double startZ;

    public TaskFly(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public boolean parse(String... args) {
        if(args.length == 1) {
            flyType = FlyType.TOGGLE;
            return true;
        } else if(args.length == 2) {
            return (flyType = FlyType.getType(args[1])) != null && flyType != FlyType.TOGGLE;
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
        return flyType == FlyType.TOGGLE ? 4 : 5;
    }

    @Override
    public void update() {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        if(flyType == FlyType.TOGGLE) {
            settings.keyBindJump.pressed = !settings.keyBindJump.pressed;
        } else if(flyType == FlyType.UP) {
            settings.keyBindJump.pressed = true;
        } else if(flyType == FlyType.DOWN) {
            settings.keyBindSneak.pressed = true;
        }
    }

    @Override
    public String getName() {
        return "jump" + (flyType != FlyType.UP ? " " + flyType.getNames().get(0) : "");
    }

    @SubscribeEvent
    public static void onInputUpdate(InputUpdateEvent event) {
        /*Task currTask = ChatTaskHandler.getCurrentTask();
        if(currTask instanceof TaskFly) {
            TaskFly flyTask = (TaskFly) currTask;
            GameSettings settings = Minecraft.getMinecraft().gameSettings;
            if(flyTask.flyType == FlyType.TOGGLE) {
                event.getMovementInput().jump = !event.getMovementInput().jump;
            }
            
        }*/
    }

    @Override
    public void terminate() {
        if(flyType == FlyType.UP) {
            Minecraft.getMinecraft().gameSettings.keyBindJump.pressed = false;
        } else if(flyType == FlyType.DOWN) {
            Minecraft.getMinecraft().gameSettings.keyBindSneak.pressed = false;
        } else if(flyType == FlyType.TOGGLE) {
            Minecraft.getMinecraft().gameSettings.keyBindJump.pressed = false;
        }
    }

    public static enum FlyType {
        TOGGLE(false, "on", "off"),
        UP(true, "up"),
        DOWN(true, "down");

        private List<String> validNames;
        private boolean isMovement;

        private FlyType(boolean isMovement, String... names) {
            this.validNames = Lists.newArrayList(names);
            this.isMovement = isMovement;
        }

        public ImmutableList<String> getNames() {
            return ImmutableList.copyOf(this.validNames);
        }

        public boolean isMovement() {
            return this.isMovement;
        }

        public boolean isValid(String name) {
            return validNames.contains(name);
        }

        @Nullable
        public static FlyType getType(String name) {
            for(FlyType type : FlyType.values()) {
                if(type.isValid(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}

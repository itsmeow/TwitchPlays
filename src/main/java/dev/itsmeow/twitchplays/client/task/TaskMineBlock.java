package dev.itsmeow.twitchplays.client.task;

import dev.itsmeow.twitchplays.TwitchPlays.Options;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public class TaskMineBlock extends Task {
    public IBlockState state;
    public BlockPos pos;

    public boolean terminate;

    public TaskMineBlock(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.setIngameFocus();
        mc.inGameHasFocus = true;
        mc.gameSettings.keyBindAttack.pressed = true;
        if(mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            pos = mc.objectMouseOver.getBlockPos();
            state = mc.world.getBlockState(pos);
            if(state.getBlockHardness(mc.world, pos) < 0.0F || mc.world.isAirBlock(pos)) {
                terminate = true;
            }
        }
        if(player.rotationPitch == 90F && Options.ANTI_MINE_DOWN) {
            timeActive = 50000;
        }
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
        return terminate || timeActive > (20 * 60) ? 0 : timeActive + 2;
    }

    @Override
    protected void update() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.clickMouse();

        if(!mc.gameSettings.keyBindAttack.pressed || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK || !mc.objectMouseOver.getBlockPos().equals(pos) || mc.world.getBlockState(pos) != state) {
            terminate = true;
        }
    }

    @Override
    public String getName() {
        return "mine";
    }
}

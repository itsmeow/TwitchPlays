package dev.itsmeow.twitchplays.client.task;

import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public class TaskPlaceBlock extends Task {
    public BlockPos pos;

    public TaskPlaceBlock(WorldClient world, EntityPlayerSP player) {
        super(world, player);
    }

    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.setIngameFocus();
        mc.inGameHasFocus = true;
        if(mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            pos = mc.objectMouseOver.getBlockPos();

            IBlockState state = mc.world.getBlockState(pos);

            EnumFacing side = mc.objectMouseOver.sideHit;

            if(state == Blocks.SNOW_LAYER && mc.world.getBlockState(pos).getValue(BlockSnow.LAYERS) == 8) {
                side = EnumFacing.UP;
            } else if(state != Blocks.VINE && state != Blocks.TALLGRASS && state != Blocks.DEADBUSH && !state.getBlock().isReplaceable(mc.world, pos)) {
                pos = pos.offset(side);
            }
        } else {
            this.timeActive = 10;
        }
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
        return 10;
    }

    @Override
    protected void update() {
        Minecraft mc = Minecraft.getMinecraft();
        if(pos != null && mc.world.checkNoEntityCollision(new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))) {
            boolean sneak = mc.player.isSneaking();
            mc.player.setSneaking(true);
            mc.rightClickMouse();
            mc.player.setSneaking(sneak);
            timeActive = 5000;
            return;
        }
        // jumping handled in TaskJump
    }

    @Override
    public String getName() {
        return "place";
    }
}

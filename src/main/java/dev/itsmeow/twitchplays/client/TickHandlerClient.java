package dev.itsmeow.twitchplays.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.TwitchPlays.Options;
import dev.itsmeow.twitchplays.client.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TickHandlerClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Field thirdPersonDistanceField = ObfuscationReflectionHelper.findField(EntityRenderer.class, "field_78490_B");

    // Init
    private static WorldClient worldInstance;

    // Minicam
    public static boolean showMinicam;
    private static boolean updateMinicam;
    private static Framebuffer minicam;
    private static EntityPlayerSP playerInstance;
    public static float prevCamYaw;
    public static float prevCamPitch;
    public static float camYaw;
    public static float camPitch;

    public static void initialize() {
        ChatTaskHandler.initBot();
        if(Options.MINICAM_ENABLED) {
            if(OpenGlHelper.isFramebufferEnabled()) {
                minicam = new Framebuffer(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, true);
                showMinicam = true;
            } else {
                LOGGER.warn("Your system does not support Frame buffers, TwitchPlays minicam will be disabled!");
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            renderMinicam(event);
        } else {
            Minecraft mc = Minecraft.getMinecraft();

            renderMinicamOnScreen(event);

            renderTaskQueue(event);

            if(mc.world != worldInstance) {
                if(worldInstance == null && !Options.AUTO_CONNECT_CHANNEL.isEmpty() && !ChatTaskHandler.isSessionRunning()) {
                    String name = Options.AUTO_CONNECT_CHANNEL;
                    mc.player.sendMessage(new TextComponentTranslation("twitchplays.command.started", name).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                    ChatTaskHandler.startSession(name);
                }
                worldInstance = mc.world;
                if(worldInstance == null) {
                    ChatTaskHandler.endSession();
                }
            }
        }
    }

    public static void renderMinicam(TickEvent.RenderTickEvent event) {
        if(minicam == null || !ChatTaskHandler.isSessionRunning() || !updateMinicam || !showMinicam || !Options.MINICAM_ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if((playerInstance == null || playerInstance.world != mc.world) && mc.world != null) {
            playerInstance = new EntityPlayerSP(mc, mc.world, mc.player.connection, mc.player.getStatFileWriter(), mc.player.getRecipeBook());
            ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, playerInstance, mc.player.getGameProfile(), "field_146106_i");
        }

        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        minicam.bindFramebuffer(true);

        GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        GlStateManager.color(1.0F, 0.0F, 0.0F);

        Entity viewEntity = mc.getRenderViewEntity();
        EntityPlayerSP mcPlayer = mc.player;

        mc.setRenderViewEntity(playerInstance);
        mc.player = playerInstance;

        NBTTagCompound tag = new NBTTagCompound();
        mcPlayer.writeToNBT(tag);
        playerInstance.readFromNBT(tag);

        playerInstance.posX = mcPlayer.posX;
        playerInstance.posY = mcPlayer.posY + 0.01D;
        playerInstance.posZ = mcPlayer.posZ;

        playerInstance.prevPosX = mcPlayer.prevPosX;
        playerInstance.prevPosY = mcPlayer.prevPosY + 0.01D;
        playerInstance.prevPosZ = mcPlayer.prevPosZ;

        playerInstance.lastTickPosX = mcPlayer.lastTickPosX;
        playerInstance.lastTickPosY = mcPlayer.lastTickPosY + 0.01D;
        playerInstance.lastTickPosZ = mcPlayer.lastTickPosZ;

        playerInstance.prevRenderYawOffset = mcPlayer.prevRenderYawOffset;
        playerInstance.renderYawOffset = mcPlayer.renderYawOffset;

        playerInstance.prevRotationYaw = mcPlayer.prevRotationYaw;
        playerInstance.prevRotationYawHead = mcPlayer.prevRotationYawHead;
        playerInstance.prevRotationPitch = mcPlayer.prevRotationPitch;

        playerInstance.rotationYaw = mcPlayer.rotationYaw;
        playerInstance.rotationYawHead = mcPlayer.rotationYawHead;
        playerInstance.rotationPitch = mcPlayer.rotationPitch;

        playerInstance.deathTime = mcPlayer.deathTime;
        playerInstance.hurtTime = mcPlayer.hurtTime;

        playerInstance.swingProgress = mcPlayer.swingProgress;
        playerInstance.swingProgressInt = mcPlayer.swingProgressInt;
        playerInstance.isSwingInProgress = mcPlayer.isSwingInProgress;

        playerInstance.prevLimbSwingAmount = mcPlayer.prevLimbSwingAmount;
        playerInstance.limbSwingAmount = mcPlayer.limbSwingAmount;
        playerInstance.limbSwing = mcPlayer.limbSwing;

        playerInstance.ticksExisted = mcPlayer.ticksExisted;

        playerInstance.renderOffsetX = mcPlayer.renderOffsetX;
        playerInstance.renderOffsetY = mcPlayer.renderOffsetY;
        playerInstance.renderOffsetZ = mcPlayer.renderOffsetZ;
        playerInstance.height = mcPlayer.height;

        playerInstance.setInvisible(mcPlayer.isInvisible());

        boolean hideGui = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;

        int tp = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 1;

        float tpDist = mc.entityRenderer.thirdPersonDistance;
        try {
            setFinalField(thirdPersonDistanceField, mc.entityRenderer, ((float) Options.MINICAM_DISTANCE) * 0.1F);
        } catch(Exception e) {
            e.printStackTrace();
        }

        float tpDistPrev = mc.entityRenderer.thirdPersonDistancePrev;
        mc.entityRenderer.thirdPersonDistancePrev = ((float) Options.MINICAM_DISTANCE) * 0.1F;

        playerInstance.prevRotationYaw = prevCamYaw;
        playerInstance.rotationYaw = camYaw;
        playerInstance.prevRotationPitch = prevCamPitch;
        playerInstance.rotationPitch = camPitch;

        mc.entityRenderer.renderWorld(event.renderTickTime, 0L);

        try {
            setFinalField(thirdPersonDistanceField, mc.entityRenderer, tpDist);
        } catch(Exception e) {
            e.printStackTrace();
        }
        mc.entityRenderer.thirdPersonDistancePrev = tpDistPrev;

        mc.gameSettings.thirdPersonView = tp;

        mc.gameSettings.hideGUI = hideGui;

        mc.player = mcPlayer;

        mc.setRenderViewEntity(viewEntity);

        Particle.interpPosX = mcPlayer.lastTickPosX + (mcPlayer.posX - mcPlayer.lastTickPosX) * (double) event.renderTickTime;
        Particle.interpPosY = mcPlayer.lastTickPosY + (mcPlayer.posY - mcPlayer.lastTickPosY) * (double) event.renderTickTime;
        Particle.interpPosZ = mcPlayer.lastTickPosZ + (mcPlayer.posZ - mcPlayer.lastTickPosZ) * (double) event.renderTickTime;

        minicam.unbindFramebuffer();

        GlStateManager.popMatrix();

        GlStateManager.color(1F, 1F, 1F, 1F);

        GlStateManager.disableLighting();

        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        mc.getFramebuffer().bindFramebuffer(true);

        GlStateManager.enableTexture2D();

        updateMinicam = false;
    }

    private static void setFinalField(Field field, Object object, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(object, newValue);
    }

    public static void renderMinicamOnScreen(TickEvent.RenderTickEvent event) {
        if(minicam == null || !ChatTaskHandler.isSessionRunning() || !(Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat) || !showMinicam || !Options.MINICAM_ENABLED) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution reso = new ScaledResolution(mc);

        int width1 = reso.getScaledWidth() - 5 - (int) ((float) reso.getScaledWidth() * ((float) Options.MINICAM_SCALE) / 100F);
        int height1 = 5;
        int width2 = width1 + (int) ((float) reso.getScaledWidth() * ((float) Options.MINICAM_SCALE) / 100F);
        int height2 = height1 + (int) ((float) reso.getScaledHeight() * ((float) Options.MINICAM_SCALE) / 100F);

        GlStateManager.pushMatrix();
        {
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            minicam.bindFramebufferTexture();

            GlStateManager.translate(0.0F, 0.0F, -909.999847412109261313162F);
            Tessellator t = Tessellator.getInstance();
            GlStateManager.disableTexture2D();
            GlStateManager.color(0.0F, 0.0F, 0.0F, 0.8F);
            BufferBuilder r = t.getBuffer();
            r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            r.pos(width1 + 1, height2 - 1, -90.0D).tex(0.0D, 0.0D).endVertex();
            r.pos(width2 - 1, height2 - 1, -90.0D).tex(1.0D, 0.0D).endVertex();
            r.pos(width2 - 1, height1 + 1, -90.0D).tex(1.0D, 1.0D).endVertex();
            r.pos(width1 + 1, height1 + 1, -90.0D).tex(0.0D, 1.0D).endVertex();
            t.draw();
            Vec3d fog = mc.world.getFogColor(event.renderTickTime);
            GlStateManager.color((float) fog.x, (float) fog.y, (float) fog.z, 1.0F);
            r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            r.pos(width1 + 2, height2 - 2, -90.0D).tex(0.0D, 0.0D).endVertex();
            r.pos(width2 - 2, height2 - 2, -90.0D).tex(1.0D, 0.0D).endVertex();
            r.pos(width2 - 2, height1 + 2, -90.0D).tex(1.0D, 1.0D).endVertex();
            r.pos(width1 + 2, height1 + 2, -90.0D).tex(0.0D, 1.0D).endVertex();
            t.draw();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableTexture2D();
            r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            r.pos(width1 + 2, height2 - 2, -90.0D).tex(0.0D, 0.0D).endVertex();
            r.pos(width2 - 2, height2 - 2, -90.0D).tex(1.0D, 0.0D).endVertex();
            r.pos(width2 - 2, height1 + 2, -90.0D).tex(1.0D, 1.0D).endVertex();
            r.pos(width1 + 2, height1 + 2, -90.0D).tex(0.0D, 1.0D).endVertex();
            t.draw();
            GlStateManager.enableAlpha();
        }
        GlStateManager.popMatrix();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        GlStateManager.pushMatrix();
        {
            float scale = 0.5F;
            GlStateManager.scale(scale, scale, scale);
            StringBuilder sb = new StringBuilder();
            sb.append((int) Math.floor(mc.player.posX));
            sb.append(" ");
            sb.append((int) Math.floor(mc.player.getEntityBoundingBox().minY));
            sb.append(" ");
            sb.append((int) Math.floor(mc.player.posZ));

            mc.fontRenderer.drawString(sb.toString(), (int) ((width2 - 3) / scale - mc.fontRenderer.getStringWidth(sb.toString())), (int) ((height1 + 3) / scale), 0xffffff, false);

            mc.fontRenderer.drawString("Tasks executed: " + ChatTaskHandler.getTasksExecuted(), (int) ((width1 + 3) / scale), (int) ((height1 + 3) / scale), 0xffffff, false);
            if(ChatTaskHandler.modsOnly()) {
                GlStateManager.translate(0.0F, mc.fontRenderer.FONT_HEIGHT, 0.0F);
                mc.fontRenderer.drawString("Mod only input", (int) ((width1 + 3) / scale), (int) ((height1 + 4) / scale), 0xffffff, false);
            }
            if(mc.player.isSneaking()) {
                GlStateManager.translate(0.0F, mc.fontRenderer.FONT_HEIGHT, 0.0F);
                mc.fontRenderer.drawString("Sneaking", (int) ((width1 + 3) / scale), (int) ((height1 + 4) / scale), 0xffffff, false);
            }
        }
        GlStateManager.popMatrix();

    }

    public static void renderTaskQueue(TickEvent.RenderTickEvent event) {
        if(!ChatTaskHandler.isSessionRunning() || !(Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution reso = new ScaledResolution(mc);

        boolean hasMinicam = minicam != null && showMinicam && Options.MINICAM_ENABLED;
        for(int i = 0; i < ChatTaskHandler.getTasks().size(); i++) {
            Task task = ChatTaskHandler.getTasks().get(i);
            if((hasMinicam ? (8 + (int) ((float) reso.getScaledHeight() * ((float) Options.MINICAM_SCALE) / 100F)) : 5) + ((i + 2) * mc.fontRenderer.FONT_HEIGHT + 1) > reso.getScaledHeight()) {
                int line = mc.fontRenderer.getStringWidth(Integer.toString(ChatTaskHandler.getTasks().size() - i) + " more...");
                mc.fontRenderer.drawString(Integer.toString(ChatTaskHandler.getTasks().size() - i) + " more...", reso.getScaledWidth() - 6 - line, (hasMinicam ? (7 + (int) ((float) reso.getScaledHeight() * ((float) Options.MINICAM_SCALE) / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), 0xbbbbbb, false);
                break;
            }
            int taskNameWidth = mc.fontRenderer.getStringWidth(task.getDisplayName());
            mc.fontRenderer.drawString(task.getDisplayName(), reso.getScaledWidth() - 6 - taskNameWidth, (hasMinicam ? (7 + (int) ((float) reso.getScaledHeight() * ((float) Options.MINICAM_SCALE) / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            if(task.getCommander() != null) {
                String name = task.getCommander();
                int taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                int startX = reso.getScaledWidth() - 5 - (int) ((float) reso.getScaledWidth() * ((float) Options.MINICAM_SCALE) / 100F) + 1;

                while(startX + taskCommanderWidth >= reso.getScaledWidth() - 6 - taskNameWidth) {
                    name = name.substring(0, name.length() - 1);
                    if(name.isEmpty()) {
                        break;
                    }
                    taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                }
                mc.fontRenderer.drawString(name, startX, (hasMinicam ? (7 + (int) ((float) reso.getScaledHeight() * ((float) Options.MINICAM_SCALE) / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            }
        }
    }

    public static void updateMinicam() {
        TickHandlerClient.updateMinicam = true;
    }

    public static void updateCameraAngles() {
        TickHandlerClient.prevCamYaw = TickHandlerClient.camYaw;
        TickHandlerClient.prevCamPitch = TickHandlerClient.camPitch;
    }

}

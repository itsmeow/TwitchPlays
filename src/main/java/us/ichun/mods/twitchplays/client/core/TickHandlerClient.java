package us.ichun.mods.twitchplays.client.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ichun.client.render.RendererHelper;
import ichun.common.core.util.ObfHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import us.ichun.mods.twitchplays.client.task.Task;
import us.ichun.mods.twitchplays.client.task.TaskRegistry;
import us.ichun.mods.twitchplays.common.TwitchPlays;

public class TickHandlerClient extends ListenerAdapter
{
    public PircBotX chat;
    public Thread botThread;

    public TickHandlerClient()
    {
        Configuration configuration = new Configuration.Builder()
        .setName("TwitchPlaysController")
        .addServer("irc.chat.twitch.tv", 6667)
        .setServerPassword(TwitchPlays.config.getString("token"))
        .addListener(this)
        .buildConfiguration();
        chat = new PircBotX(configuration);
        botThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chat.startBot();
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(IrcException e) {
                    e.printStackTrace();
                }
            }
        });
        botThread.start();

        if(TwitchPlays.config.getInt("minicam") == 1)
        {
            if(OpenGlHelper.isFramebufferEnabled())
            {
                minicam = RendererHelper.createFrameBuffer("TwitchPlays", true);
                showMinicam = true;
            }
            else
            {
                TwitchPlays.console("Your system does not support Frame buffers, TwitchPlays minicam will be disabled.!", true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START)
        {
            renderMinicam(event);
        }
        else
        {
            Minecraft mc = Minecraft.getMinecraft();

            renderMinicamOnScreen(event);

            renderTaskQueue(event);


            if(mc.theWorld != worldInstance)
            {
                if(worldInstance == null && TwitchPlays.config.getInt("autoConnect") == 1 && !init)
                {
                    mc.thePlayer.addChatMessage(new ChatComponentTranslation("twitchplays.command.started", TwitchPlays.config.getString("autoConnectName")).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true)));
                    startSession(TwitchPlays.config.getString("autoConnectName"));
                }
                worldInstance = mc.theWorld;
                if(worldInstance == null)
                {
                    endSession();
                }
            }
        }
    }

    public void endSession()
    {
        if(init)
        {
            init = false;

            chat.sendRaw().rawLine("PART #" + chatOwner);
            chatOwner = "";
        }
    }

    public void startSession(String streamer)
    {
        if(!init)
        {
            init = true;

            oriPitch = targetPitch = prevCamPitch = camPitch = 90F;

            tasks.clear();
            instaTasks.clear();
            taskCallTime.clear();

            tasksExecuted = 0;

            chatOwner = streamer.toLowerCase();

            if(ObfHelper.obfuscation || !ObfHelper.obfuscation && TwitchPlays.config.getInt("twitchChatHook") == 1)
            {
                chat.sendIRC().joinChannel("#" + chatOwner);
                
            }

        }
    }

    public void renderMinicam(TickEvent.RenderTickEvent event)
    {
        if(minicam == null || !init || !updateMinicam || !showMinicam)
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if((playerInstance == null || playerInstance.worldObj != mc.theWorld) && mc.theWorld != null)
        {
            playerInstance = new EntityClientPlayerMP(mc, mc.theWorld, new Session(mc.thePlayer.getCommandSenderName(), mc.thePlayer.getGameProfile().getId().toString().replace("-", ""), "fakeToken", "mojang"), mc.thePlayer != null ? mc.thePlayer.sendQueue : null, null);
            ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, playerInstance, mc.thePlayer.getGameProfile(), ObfHelper.gameProfile);
        }

        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        minicam.bindFramebuffer(true);

        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glColor3f(1.0F, 0.0F, 0.0F);

        EntityLivingBase viewEntity = mc.renderViewEntity;
        EntityClientPlayerMP mcPlayer = mc.thePlayer;

        mc.renderViewEntity = playerInstance;
        mc.thePlayer = playerInstance;

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

        playerInstance.yOffset = mcPlayer.yOffset;
        playerInstance.ySize = mcPlayer.ySize;

        playerInstance.setInvisible(mcPlayer.isInvisible());

        boolean hideGui = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;

        int tp = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 1;

        float tpDist = mc.entityRenderer.thirdPersonDistance;
        mc.entityRenderer.thirdPersonDistance = (float)TwitchPlays.config.getInt("minicamDistance") * 0.1F;

        float tpDistTemp = mc.entityRenderer.thirdPersonDistanceTemp;
        mc.entityRenderer.thirdPersonDistanceTemp = (float)TwitchPlays.config.getInt("minicamDistance") * 0.1F;

        playerInstance.prevRotationYaw = prevCamYaw;
        playerInstance.rotationYaw = camYaw;
        playerInstance.prevRotationPitch = prevCamPitch;
        playerInstance.rotationPitch = camPitch;

        mc.entityRenderer.renderWorld(event.renderTickTime, 0L);

        mc.entityRenderer.thirdPersonDistance = tpDist;
        mc.entityRenderer.thirdPersonDistanceTemp = tpDistTemp;

        mc.gameSettings.thirdPersonView = tp;

        mc.gameSettings.hideGUI = hideGui;

        mc.thePlayer = mcPlayer;

        mc.renderViewEntity = viewEntity;

        EntityFX.interpPosX = mcPlayer.lastTickPosX + (mcPlayer.posX - mcPlayer.lastTickPosX) * (double)event.renderTickTime;
        EntityFX.interpPosY = mcPlayer.lastTickPosY + (mcPlayer.posY - mcPlayer.lastTickPosY) * (double)event.renderTickTime;
        EntityFX.interpPosZ = mcPlayer.lastTickPosZ + (mcPlayer.posZ - mcPlayer.lastTickPosZ) * (double)event.renderTickTime;

        minicam.unbindFramebuffer();

        GL11.glPopMatrix();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glDisable(GL11.GL_LIGHTING);

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        mc.getFramebuffer().bindFramebuffer(true);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        updateMinicam = false;
    }

    public void renderMinicamOnScreen(TickEvent.RenderTickEvent event)
    {
        if(minicam == null || !init || !(Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat) || !showMinicam)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution reso = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        int width1 = reso.getScaledWidth() - 5 - (int)((float)reso.getScaledWidth() * (float)TwitchPlays.config.getInt("minicamSize") / 100F);
        int height1 = 5;
        int width2 = width1 + (int)((float)reso.getScaledWidth() * (float)TwitchPlays.config.getInt("minicamSize") / 100F);
        int height2 = height1 + (int)((float)reso.getScaledHeight() * (float)TwitchPlays.config.getInt("minicamSize") / 100F);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        minicam.bindFramebufferTexture();

        GL11.glTranslatef(0.0F, 0.0F, -909.999847412109261313162F);
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(width1+1, height2-1, -90.0D, 0.0D, 0.0D);
        tessellator.addVertexWithUV(width2-1, height2-1, -90.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(width2-1, height1+1, -90.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(width1+1, height1+1, -90.0D, 0.0D, 1.0D);
        tessellator.draw();
        Vec3 fog = mc.theWorld.getFogColor(event.renderTickTime);
        GL11.glColor4f((float)fog.xCoord, (float)fog.yCoord, (float)fog.zCoord, 1.0F);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(width1+2, height2-2, -90.0D, 0.0D, 0.0D);
        tessellator.addVertexWithUV(width2-2, height2-2, -90.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(width2-2, height1+2, -90.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(width1+2, height1+2, -90.0D, 0.0D, 1.0D);
        tessellator.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(width1+2, height2-2, -90.0D, 0.0D, 0.0D);
        tessellator.addVertexWithUV(width2-2, height2-2, -90.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(width2-2, height1+2, -90.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(width1+2, height1+2, -90.0D, 0.0D, 1.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glPopMatrix();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);

        GL11.glPushMatrix();
        float scale = 0.5F;

        GL11.glScalef(scale, scale, scale);

        StringBuilder sb = new StringBuilder();
        sb.append((int)Math.floor(mc.thePlayer.posX));
        sb.append(" ");
        sb.append((int)Math.floor(mc.thePlayer.boundingBox.minY));
        sb.append(" ");
        sb.append((int)Math.floor(mc.thePlayer.posZ));

        mc.fontRenderer.drawString(sb.toString(), (int)((width2 - 3) / scale - mc.fontRenderer.getStringWidth(sb.toString())), (int)((height1 + 3) / scale), 0xffffff, false);

        mc.fontRenderer.drawString("Tasks executed: " + tasksExecuted, (int)((width1 + 3) / scale), (int)((height1 + 3) / scale), 0xffffff, false);
        if(forceOpInput)
        {
            GL11.glTranslatef(0.0F, mc.fontRenderer.FONT_HEIGHT, 0.0F);
            mc.fontRenderer.drawString("Mod only input", (int)((width1 + 3) / scale), (int)((height1 + 4) / scale), 0xffffff, false);
        }
        if(mc.thePlayer.isSneaking())
        {
            GL11.glTranslatef(0.0F, mc.fontRenderer.FONT_HEIGHT, 0.0F);
            mc.fontRenderer.drawString("Sneaking", (int)((width1 + 3) / scale), (int)((height1 + 4) / scale), 0xffffff, false);
        }

        GL11.glPopMatrix();

    }

    public void renderTaskQueue(TickEvent.RenderTickEvent event)
    {
        if(!init || !(Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat))
        {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution reso = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        boolean hasMinicam = minicam != null && showMinicam;
        for(int i = 0; i < tasks.size(); i++)
        {
            Task task = tasks.get(i);
            if((hasMinicam ? (8 + (int)((float)reso.getScaledHeight() * (float)TwitchPlays.config.getInt("minicamSize") / 100F)) : 5) + ((i + 2) * mc.fontRenderer.FONT_HEIGHT + 1) > reso.getScaledHeight())
            {
                int line = mc.fontRenderer.getStringWidth(Integer.toString(tasks.size() - i) + " more...");
                mc.fontRenderer.drawString(Integer.toString(tasks.size() - i) + " more...", reso.getScaledWidth() - 6 - line, (hasMinicam ? (7 + (int)((float)reso.getScaledHeight() * (float)TwitchPlays.config.getInt("minicamSize") / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), 0xbbbbbb, false);
                break;
            }
            int taskNameWidth = mc.fontRenderer.getStringWidth(task.getName());
            mc.fontRenderer.drawString(task.getName(), reso.getScaledWidth() - 6 - taskNameWidth, (hasMinicam ? (7 + (int)((float)reso.getScaledHeight() * (float)TwitchPlays.config.getInt("minicamSize") / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            if(task.getCommander() != null)
            {
                String name = task.getCommander();
                int taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                int startX = reso.getScaledWidth() - 5 - (int)((float)reso.getScaledWidth() * (float)TwitchPlays.config.getInt("minicamSize") / 100F) + 1;

                while(startX + taskCommanderWidth >= reso.getScaledWidth() - 6 - taskNameWidth)
                {
                    name = name.substring(0, name.length() - 1);
                    if(name.isEmpty())
                    {
                        break;
                    }
                    taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                }
                mc.fontRenderer.drawString(name, startX, (hasMinicam ? (7 + (int)((float)reso.getScaledHeight() * (float)TwitchPlays.config.getInt("minicamSize") / 100F)) : 4) + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase == TickEvent.Phase.END)
        {
            Minecraft mc = Minecraft.getMinecraft();
            if(mc.theWorld != null)
            {
                prevCamYaw = camYaw;
                prevCamPitch = camPitch;

                if(clock != mc.theWorld.getWorldTime() || !mc.theWorld.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))
                {
                    updateMinicam = true;
                    clock = mc.theWorld.getWorldTime();
                    if(!tasks.isEmpty())
                    {
                        Task task = tasks.get(0);
                        if(!mc.thePlayer.isEntityAlive() && !task.canWorkDead())
                        {
                            tasks.remove(0);
                        }
                        else
                        {
                            if(task.timeActive == 0)
                            {
                                task.world = mc.theWorld;
                                task.player = mc.thePlayer;
                                taskCallTime.put(task.getClass(), (int)mc.theWorld.getWorldTime());
                                task.init();
                                tasksExecuted++;
                            }
                            task.tick();
                            if(task.timeActive >= task.maxActiveTime())
                            {
                                task.terminate();
                                tasks.remove(0);
                            }
                        }
                    }
                    for(int i = instaTasks.size() - 1; i >= 0; i--)
                    {
                        Task task = instaTasks.get(i);
                        if(task.timeActive == 0)
                        {
                            task.world = mc.theWorld;
                            task.player = mc.thePlayer;
                            taskCallTime.put(task.getClass(), (int)mc.theWorld.getWorldTime());
                            task.init();
                        }
                        task.tick();
                        if(task.timeActive >= task.maxActiveTime())
                        {
                            task.terminate();
                            instaTasks.remove(i);
                        }
                    }
                    if(turnTime > 0)
                    {
                        camYaw += (targetYaw - oriYaw) *(1F / (float)TURN_TIME);
                        camPitch += (targetPitch - oriPitch) *(1F / (float)TURN_TIME);
                        turnTime--;
                        if(turnTime == 0)
                        {
                            camYaw = targetYaw;
                            camPitch = targetPitch;
                        }
                    }
                }
                if(isDemocracy)
                {
                    if(democracyTimer > 0)
                    {
                        democracyTimer--;
                        if(democracyTimer == 0)
                        {
                            countingVotes = true;

                            boolean flag = false;
                            while(!flag)
                            {
                                int votes = -1;
                                String task = "";
                                for(Map.Entry<String, Integer> e : democracyVotes.entrySet())
                                {
                                    if(e.getValue() > votes)
                                    {
                                        votes = e.getValue();
                                        task = e.getKey();
                                    }
                                }
                                if(votes != -1)
                                {
                                    flag = parseChat(mc.theWorld, mc.thePlayer, task, null, false, true);
                                    democracyVotes.remove(task);
                                }
                                else
                                {
                                    flag = true;
                                }
                            }

                            countingVotes = false;

                            democracyTimer = TwitchPlays.config.getInt("democracyTimer") * 20;
                            democracyVotes.clear();
                            voters.clear();
                        }
                    }
                }
            }
        }
    }

    public void addVote(String user, String task)
    {
        if(!voters.contains(user))
        {
            voters.add(user);
            Integer votes = democracyVotes.get(task);
            if(votes == null)
            {
                votes = 0;
            }
            democracyVotes.put(task, votes + 1);
        }
    }

    public boolean parseChat(WorldClient world, EntityPlayerSP player, String s, String user, boolean isOp)//return true if task is added
    {
        if(forceOpInput && !isOp)
        {
            return false;
        }
        if(isDemocracy)
        {
            boolean isTask = parseChat(world, player, s, user, isOp, false);
            if(isTask)
            {
                String[] args = s.split(" ");
                String task = "";
                for(int i = 0; i < args.length; i++)
                {
                    if(!args[i].toLowerCase().trim().isEmpty())
                    {
                        task = task + args[i].toLowerCase().trim() + " ";
                        task = task.trim();
                    }
                }

                addVote(user, task);
            }

            return isTask;
        }
        return parseChat(world, player, s, user, isOp, true);
    }

    public boolean parseChat(WorldClient world, EntityPlayerSP player, String s, String user, boolean isOp, boolean add)//return true if task is added
    {
        String[] args = s.split(" ");
        ArrayList<String> actualArgs = new ArrayList<String>();
        for(int i = 0; i < args.length; i++)
        {
            if(!args[i].toLowerCase().trim().isEmpty())
            {
                actualArgs.add(args[i].toLowerCase().trim());
            }
        }
        int count = 1;
        if(actualArgs.isEmpty())
        {
            return false;
        }
        String arg0 = actualArgs.get(0);
        try
        {
            int count1 = Integer.parseInt(arg0.substring(arg0.length() - 1));
            if(count1 > TwitchPlays.config.getInt("inputMax"))
            {
                return false;
            }
            count = count1;
            arg0 = arg0.substring(0, arg0.length() - 1);
            actualArgs.remove(0);
            actualArgs.add(0, arg0);
        }
        catch(NumberFormatException e)
        {
        }
        if(!actualArgs.isEmpty() && TaskRegistry.hasTask(actualArgs.get(0)))
        {
            String[] newArgs = actualArgs.toArray(new String[actualArgs.size()]);
            boolean flag = false;
            for(int i = 0; i < count; i++)
            {
                Task task = TaskRegistry.createTask(world, player, newArgs);
                int lastCall = Integer.MAX_VALUE;
                if(task != null && taskCallTime.containsKey(task.getClass()))
                {
                    lastCall = (int)world.getWorldTime() - taskCallTime.get(task.getClass());
                }
                if(task != null && task.canBeAdded(ImmutableList.copyOf(tasks), lastCall) && (task.requiresOp(newArgs) && isOp || !task.requiresOp(newArgs)))
                {
                    if(add || task.requiresOp(newArgs) && !countingVotes)
                    {
                        task.setCommander(user);
                        if(task.bypassOrder(newArgs))
                        {
                            instaTasks.add(task);
                        }
                        else
                        {
                            tasks.add(task);
                        }
                    }
                    flag = true;
                    if(task.requiresOp(newArgs) && countingVotes)
                    {
                        flag = false;
                    }
                }
            }
            return flag;
        }
        return false;
    }

    @SubscribeEvent
    public void onChatEvent(ClientChatReceivedEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if(TwitchPlays.config.getInt("chatListen") == 1 && init && mc.theWorld != null && event.message instanceof ChatComponentTranslation)
        {
            ChatComponentTranslation msg = (ChatComponentTranslation)event.message;
            if(msg.getKey().equals("chat.type.text") && msg.getFormatArgs().length > 1)
            {
                String name = "";
                boolean isPlayer = false;
                if(msg.getFormatArgs()[0] instanceof ChatComponentText)
                {
                    name = ((ChatComponentText)msg.getFormatArgs()[0]).getUnformattedTextForChat();
                    isPlayer = name.equals(mc.thePlayer.getCommandSenderName());
                }
                String s = "";
                for(int i = 1; i < msg.getFormatArgs().length; i++)
                {
                    if(msg.getFormatArgs()[i] instanceof ChatComponentText)
                    {
                        s = s + ((ChatComponentText)msg.getFormatArgs()[i]).getUnformattedTextForChat();
                    }
                    else if(msg.getFormatArgs()[i] instanceof ChatComponentTranslation)
                    {
                        s = s + ((ChatComponentTranslation)msg.getFormatArgs()[i]).getUnformattedTextForChat();
                    }
                    else
                    {
                        s = s + msg.getFormatArgs()[i].toString();
                    }
                }
                parseChat(mc.theWorld, mc.thePlayer, s, name, isPlayer);
            }
        }
    }

    //TODO commands parsed, time enlapsed, etc.

    public long clock;

    public WorldClient worldInstance;
    public boolean init;

    public boolean isDemocracy;
    public boolean countingVotes;
    public int democracyTimer;
    public HashMap<String, Integer> democracyVotes = new HashMap<String, Integer>();
    public ArrayList<String> voters = new ArrayList<String>();

    public int tasksExecuted = 0;
    public ArrayList<Task> tasks = new ArrayList<Task>();
    public ArrayList<Task> instaTasks = new ArrayList<Task>();
    public HashMap<Class<? extends Task>, Integer> taskCallTime = new HashMap<Class<? extends Task>, Integer>();

    public boolean showMinicam;
    public boolean updateMinicam;
    public Framebuffer minicam;
    public EntityClientPlayerMP playerInstance;
    public float prevCamYaw;
    public float prevCamPitch;
    public float camYaw;
    public float camPitch;
    public float oriYaw;
    public float oriPitch;
    public float targetYaw;
    public float targetPitch;
    public int turnTime;
    public static final int TURN_TIME = 10;

    public String chatOwner = "";
    public boolean forceOpInput;

    @Override
    public synchronized void onGenericMessage(final GenericMessageEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.theWorld != null) {
            mc.func_152344_a(new Runnable() {

                @Override
                public void run() {
                    String msg = event.getMessage();
                    String name = event.getUser().getLogin();
                    if(msg != null && !msg.isEmpty()) {
                        boolean isOp = Lists.newArrayList(TwitchPlays.config.getString("moderators").split(",")).contains(name);
                        boolean isTask = parseChat(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().thePlayer, msg, name, isOp);
                        ChatComponentText message = new ChatComponentText("");
                        message.getChatStyle().setColor(EnumChatFormatting.DARK_PURPLE);
                        message.appendText((isOp ? "<@" : "<") + name + "> ");
                        ChatComponentText text = new ChatComponentText(msg);
                        text.getChatStyle().setColor(isTask ? EnumChatFormatting.GRAY : EnumChatFormatting.WHITE);
                        message.appendSibling(text);
                        Minecraft.getMinecraft().thePlayer.addChatMessage(message);
                    }

                }
            });

        }
    }

    @Override
    public synchronized void onJoin(final JoinEvent event) throws Exception // On chat connect
    {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {

                if(event.getUser() == chat.getUserBot()) {
                    if(init && Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation("twitchplays.chat.connected").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true)));
                    }
                }
            }
        });
    }

    @Override
    public synchronized void onPart(final PartEvent event) throws Exception // On chat disconnect
    {
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {

                if(event.getUser() == chat.getUserBot()) {
                    if(Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation("twitchplays.chat.disconnected").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true)));
                    }
                }
            }
        });
    }
}

package dev.itsmeow.twitchplays.client;

import dev.itsmeow.twitchplays.TwitchPlays;
import dev.itsmeow.twitchplays.TwitchPlays.Options;
import dev.itsmeow.twitchplays.client.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = TwitchPlays.MOD_ID, value = Side.CLIENT)
public class TickHandlerClient {
    private static WorldClient worldInstance;
    public static float prevCamYaw;
    public static float prevCamPitch;
    public static float camYaw;
    public static float camPitch;

    public static void initialize() {
        ChatTaskHandler.initBot();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();

            if(TwitchPlays.Options.SHOW_TASK_QUEUE) {
                renderTaskQueue(event);
            }

            if(mc.world != worldInstance) {
                if(worldInstance == null && !Options.AUTO_CONNECT_CHANNEL.isEmpty() && !ChatTaskHandler.isSessionRunning()) {
                    String name = Options.AUTO_CONNECT_CHANNEL;
                    if(!TwitchPlays.Options.HIDE_STATUS_MESSAGES) {
                        mc.player.sendMessage(new TextComponentTranslation("twitchplays.command.started", name).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                    }
                    ChatTaskHandler.startSession(name);
                }
                worldInstance = mc.world;
                if(worldInstance == null) {
                    ChatTaskHandler.endSession();
                }
            }
        }
    }

    public static void renderTaskQueue(TickEvent.RenderTickEvent event) {
        if(!ChatTaskHandler.isSessionRunning() || !(Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution reso = new ScaledResolution(mc);

        for(int i = 0; i < ChatTaskHandler.getTasks().size(); i++) {
            Task task = ChatTaskHandler.getTasks().get(i);
            if(5 + ((i + 2) * mc.fontRenderer.FONT_HEIGHT + 1) > reso.getScaledHeight()) {
                int line = mc.fontRenderer.getStringWidth(Integer.toString(ChatTaskHandler.getTasks().size() - i) + " more...");
                mc.fontRenderer.drawString(Integer.toString(ChatTaskHandler.getTasks().size() - i) + " more...", reso.getScaledWidth() - 6 - line, 4 + (i * mc.fontRenderer.FONT_HEIGHT + 1), 0xbbbbbb, false);
                break;
            }
            int taskNameWidth = mc.fontRenderer.getStringWidth(task.getDisplayName());
            mc.fontRenderer.drawString(task.getDisplayName(), reso.getScaledWidth() - 6 - taskNameWidth, 4 + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            if(task.getCommander() != null) {
                String name = task.getCommander();
                int taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                int startX = reso.getScaledWidth() - 5 - reso.getScaledWidth() + 1;

                while(startX + taskCommanderWidth >= reso.getScaledWidth() - 6 - taskNameWidth) {
                    name = name.substring(0, name.length() - 1);
                    if(name.isEmpty()) {
                        break;
                    }
                    taskCommanderWidth = mc.fontRenderer.getStringWidth(name);
                }
                mc.fontRenderer.drawString(name, startX, 4 + (i * mc.fontRenderer.FONT_HEIGHT + 1), i == 0 ? 0xff2222 : 0xffffff, false);
            }
        }
    }

}

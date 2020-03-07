package dev.itsmeow.twitchplays.client.command;

import java.util.List;

import dev.itsmeow.twitchplays.TwitchPlays.Options;
import dev.itsmeow.twitchplays.client.ChatTaskHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class CommandTwitchPlays extends CommandBase {

    @Override
    public String getName() {
        return "twitchplays";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + this.getName() + "           " + I18n.format("twitchplays.command.help");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "start", "end") : args.length == 2 ? getListOfStringsMatchingLastWord(args, Options.AUTO_CONNECT_CHANNEL) : super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        boolean needHelp = false;
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("end")) {
                if(ChatTaskHandler.isSessionRunning()) {
                    sender.sendMessage(new TextComponentTranslation("twitchplays.command.ended", ChatTaskHandler.getSessionChannel()).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                    ChatTaskHandler.endSession();
                } else {
                    sender.sendMessage(new TextComponentTranslation("twitchplays.command.noSession").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                }
            } else if(args[0].equalsIgnoreCase("start")) {
                sender.sendMessage(new TextComponentTranslation("twitchplays.command.start").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
            } else {
                needHelp = true;
            }
        } else if(args.length == 2) {
            if(args[0].equalsIgnoreCase("start") && !args[1].isEmpty()) {
                if(ChatTaskHandler.isSessionRunning()) {
                    sender.sendMessage(new TextComponentTranslation("twitchplays.command.alreadyStarted", ChatTaskHandler.getSessionChannel()).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                } else {
                    sender.sendMessage(new TextComponentTranslation("twitchplays.command.started", args[1].trim()).setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
                    ChatTaskHandler.startSession(args[1].trim());
                }
            } else {
                needHelp = true;
            }
        } else {
            sender.sendMessage(new TextComponentTranslation("twitchplays.command.end").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
            sender.sendMessage(new TextComponentTranslation("twitchplays.command.start").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
        }
        if(needHelp) {
            sender.sendMessage(new TextComponentTranslation("twitchplays.command.end").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
            sender.sendMessage(new TextComponentTranslation("twitchplays.command.start").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true)));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

}

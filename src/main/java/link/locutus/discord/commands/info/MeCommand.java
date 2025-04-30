package link.locutus.discord.commands.info;

import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBNation;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MeCommand extends Command {
    private final Who who;

    public MeCommand() {
        super("me", CommandCategory.GAME_INFO_AND_TOOLS);
        this.who = new Who();
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.me.cmd);
    }

    @Override
    public String help() {
        return Settings.commandPrefix(true) + "me";
    }

    @Override
    public String desc() {
        return "Get pnw info about yourself.";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return true;
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        
        if (me == null) {
            return "Please use " + CM.register.cmd.toSlashMention();
        }
        return who.onCommand(guild, channel, author, me, fullCommandRaw, Collections.singletonList(me.getNation_id() + ""));
    }
}

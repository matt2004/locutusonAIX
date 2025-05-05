package link.locutus.discord.commands.alliance;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.binding.bindings.PrimitiveBindings;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.*;

public class LeftAA extends Command {
    public LeftAA() {
        super("LeftAA", "AllianceHistory", CommandCategory.GAME_INFO_AND_TOOLS);
    }

    @Override
    public String help() {
        return super.help() + " <alliance|nation> [time] [nation-filter]";
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.alliance.departures.cmd);
    }
    @Override
    public boolean checkPermission(Guild server, User user) {
        return true;
    }

    @Override
    public String desc() {
        return """
                List the departures of nations from alliances
                Add `-a` to remove inactives
                Add `-v` to remove VM
                Add `-m` to remove members
                Add `-i` to list nation ids""";
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.isEmpty()) {
            return usage(args.size(), 1, channel);
        }
        NationOrAlliance target = PWBindings.nationOrAlliance(args.get(0), guild);
        long time = 0;
        if (args.size() >= 2) {
            time = PrimitiveBindings.timestamp(args.get(1));
        }
        NationList filter = null;
        if (args.size() >= 3) {
            filter = PWBindings.nationList(null, guild, args.get(2), author, me);
        }
        boolean ignoreInactive = flags.contains('a');
        boolean ignoreVM = flags.contains('v');
        boolean ignoreMembers = flags.contains('m');
        boolean listIds = flags.contains('i');
        GuildDB db = Locutus.imp().getGuildDB(guild);
        return UnsortedCommands.leftAA(channel, db, target,time,filter,ignoreInactive,ignoreVM,ignoreMembers,listIds,null);
    }
}

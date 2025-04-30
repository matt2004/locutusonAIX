package link.locutus.discord.commands.sheets;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.TimeUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Set;

public class DeserterSheet extends Command {
    public DeserterSheet() {
        super(CommandCategory.MILCOM, CommandCategory.GAME_INFO_AND_TOOLS, CommandCategory.GOV);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.sheets_milcom.DeserterSheet.cmd);
    }

    @Override
    public String help() {
        return super.help() + " <alliances> [time] [currently-in]";
    }

    @Override
    public String desc() {
        return """
                A sheet of all the nations that left an alliance in the specified timeframe.
                `currently-in` is optional and only checks those nations
                Add `-a` to remove inactive nations
                Add `-v` to remove vm nations
                Add `-n` to remove applicants (current)""";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return super.checkPermission(server, user) && Roles.MILCOM.has(user, server);
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.isEmpty()) return usage(args.size(), 1, channel);
        GuildDB db = Locutus.imp().getGuildDB(guild);
        Set<DBAlliance> alliances = PWBindings.alliances(guild, args.get(0), author, me);
        Long cutoff = args.size() >= 2 ? TimeUtil.timeToSec(args.get(1)) * 1000L : null;
        Set<DBNation> filter = args.size() >= 3 ? DiscordUtil.parseNations(guild, author, me, args.get(2), false, false) : null;
        boolean ignoreInactive = flags.contains('a');
        boolean ignoreVm = flags.contains('v');
        boolean ignoreMembers = flags.contains('m');
        return WarCommands.DeserterSheet(channel, db, alliances, cutoff, filter, ignoreInactive, ignoreVm, ignoreMembers);
    }
}

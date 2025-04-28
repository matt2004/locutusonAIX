package link.locutus.discord.commands.bank;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.PW;
import link.locutus.discord.apiv1.enums.Rank;
import link.locutus.discord.apiv1.enums.ResourceType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Offshore extends Command {
    public Offshore() {
        super("offshore", CommandCategory.ECON);
    }

    @Override
    public String help() {
        return Settings.commandPrefix(true) + "offshore <alliance-url> [aa-warchest] [account]";
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.offshore.send.cmd);
    }

    @Override
    public String desc() {
        return """
                Queue a transfer offshore (with authorization)
                `aa-warchest` is how much to leave in the AA bank- in the form `{money=1,food=2}`
                `account` is what account to offshore into (e.g. your alliance or guild). Defaults to sender alliance""";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        if (Roles.ECON.has(user, server) || Roles.INTERNAL_AFFAIRS.has(user, server) || Roles.MILCOM.has(user, server)) return true;
        DBNation nation = DiscordUtil.getNation(user);
        GuildDB db = Locutus.imp().getGuildDB(server);
        if (db != null && nation != null) {
            if (db.getOrNull(GuildKey.MEMBER_CAN_OFFSHORE) == Boolean.TRUE && Roles.MEMBER.has(user, server)) return true;

            Set<Integer> aaIds = db.getAllianceIds();
            return (!aaIds.isEmpty() && nation.getPosition() >= Rank.OFFICER.id && aaIds.contains(nation.getAlliance_id()));
        }
        return false;
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.isEmpty() || args.size() > 3) return usage();

        NationOrAllianceOrGuild account = null;
        Map<ResourceType, Double> warchest;
        if (args.size() >= 2) warchest = ResourceType.parseResources(args.get(1));
        else warchest = Collections.emptyMap();
        if (args.size() >= 3) {
            account = PWBindings.nationOrAllianceOrGuild(args.get(2));
        }

        GuildDB db = Locutus.imp().getGuildDB(guild);

        DBAlliance to = null;
        if (args.size() > 0) to = DBAlliance.getOrCreate(PW.parseAllianceId(args.get(0)));
        return BankCommands.offshore(author, db, channel, to, account, warchest, null);
    }
}
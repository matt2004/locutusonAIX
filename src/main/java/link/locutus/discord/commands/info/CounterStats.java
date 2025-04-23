package link.locutus.discord.commands.info;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.PW;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.*;

public class CounterStats extends Command {
    public CounterStats() {
        super(CommandCategory.MILCOM);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.stats_war.counterStats.cmd);
    }
    @Override
    public String help() {
        return super.help() + " <alliance-id>";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return super.checkPermission(server, user);
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.size() != 1) return usage(args.size(), 1, channel);
        Integer id = PW.parseAllianceId(args.get(0));
        if (id == null) return "Invalid id: `" + id + "`";

        List<Map.Entry<DBWar, CounterStat>> counters = Locutus.imp().getWarDb().getCounters(Collections.singleton(id));

        if (counters.isEmpty()) return "No data (to include treatied alliances, append `-a`)";

        int[] uncontested = new int[2];
        int[] countered = new int[2];
        int[] counter = new int[2];
        for (Map.Entry<DBWar, CounterStat> entry : counters) {
            CounterStat stat = entry.getValue();
            DBWar war = entry.getKey();
            switch (stat.type) {
                case ESCALATION, IS_COUNTER -> countered[stat.isActive ? 1 : 0]++;
                case UNCONTESTED -> {
                    if (war.getStatus() == WarStatus.ATTACKER_VICTORY) {
                        uncontested[stat.isActive ? 1 : 0]++;
                    } else {
                        counter[stat.isActive ? 1 : 0]++;
                    }
                }
                case GETS_COUNTERED -> counter[stat.isActive ? 1 : 0]++;
            }
        }

        int totalActive = counter[1] + uncontested[1];
        int totalInactive = counter[0] + uncontested[0];

        double chanceActive = ((double) counter[1] + 1) / (totalActive + 1);
        double chanceInactive = ((double) counter[0] + 1) / (totalInactive + 1);

        if (!Double.isFinite(chanceActive)) chanceActive = 0.5;
        if (!Double.isFinite(chanceInactive)) chanceInactive = 0.5;

        String title = "% of wars that are countered (" + PW.getName(id, true) + ")";
        String response = MathMan.format(chanceActive * 100) + "% for actives (" + totalActive + " wars)" + '\n' +
                MathMan.format(chanceInactive * 100) + "% for inactives (" + totalInactive + " wars)";

        channel.create().embed(title, response).send();
        return null;
    }
}

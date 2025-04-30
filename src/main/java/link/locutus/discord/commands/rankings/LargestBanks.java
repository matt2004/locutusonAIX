package link.locutus.discord.commands.rankings;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.builder.SummedMapRankBuilder;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.LootEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LargestBanks extends Command {
    public LargestBanks() {
        super(CommandCategory.MILCOM, CommandCategory.GAME_INFO_AND_TOOLS);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.alliance.stats.loot_ranking.cmd.show_total("true"));
    }

    @Override
    public String help() {
        return super.help() + " <time>";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return true;
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.size() != 1) return usage(args.size(), 1, channel);
        Map<Integer, Double> total = new HashMap<>();

        for (DBAlliance alliance : Locutus.imp().getNationDB().getAlliances()) {
            LootEntry loot = alliance.getLoot();
            if (loot != null) {
                total.put(alliance.getAlliance_id(), loot.convertedTotal());
            }
        }

        SummedMapRankBuilder<Integer, ? extends Number> sorted = new SummedMapRankBuilder<>(total).sort();
        sorted.nameKeys(i -> DBAlliance.getOrCreate(i).toShrink()).build(author, channel, fullCommandRaw, "AA bank");

        return null;
    }
}

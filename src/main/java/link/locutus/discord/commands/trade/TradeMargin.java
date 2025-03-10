package link.locutus.discord.commands.trade;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.command.ShrinkableEmbed;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.TradeCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.trade.TradeManager;
import link.locutus.discord.apiv1.enums.ResourceType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TradeMargin extends Command {
    public TradeMargin() {
        super("TradeMargin", "TradeMargins", "margin", CommandCategory.GAME_INFO_AND_TOOLS, CommandCategory.ECON);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.trade.margin.cmd);
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return true;
    }

    @Override
    public String desc() {
        return "Get the buy/sell margin for each resource\n" +
                "Add `-p` to show margin as a % of total";
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        boolean usePercent = flags.contains('p');

        TradeManager trader = Locutus.imp().getTradeManager();
        String refreshEmoji = "Refresh";

        Map<ResourceType, Double> low = trader.getLow().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue()));
        Map<ResourceType, Double> high = trader.getHigh().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue()));
        List<ResourceType> resources = new ArrayList<>(ResourceType.valuesList);
        resources.remove(ResourceType.MONEY);

        List<String> resourceNames = resources.stream().map(r -> r.name().toLowerCase()).collect(Collectors.toList());

        ArrayList<String> diffList = new ArrayList<>();

        for (ResourceType type : resources) {
            Double o1 = low.get(type);
            Double o2 = high.get(type);

            double diff = o1 == null || o2 == null || !Double.isFinite(o1) || !Double.isFinite(o2) ? Double.NaN : (o2 - o1);

            if (usePercent) {
                diff = 100 * diff / o2;
            }

            diffList.add(o1 == null ? "" : (MathMan.format(diff) + (usePercent ? "%" : "")));
        }

        channel.create().embed(new ShrinkableEmbed()
                .setTitle("Trade Margin")
                .addField("Resource", StringMan.join(resourceNames, "\n"), true)
                .addField("margin", StringMan.join(diffList, "\n"), true)
        ).commandButton(DiscordUtil.trimContent(fullCommandRaw), "Refresh").send();
        return null;
    }
}

package link.locutus.discord.commands.bank;

import link.locutus.discord.Locutus;
import link.locutus.discord.apiv1.enums.DepositType;
import link.locutus.discord.apiv1.enums.EscrowMode;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.TaxBracket;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.PW;
import link.locutus.discord.apiv1.enums.ResourceType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Warchest extends Command {
    public Warchest() {
        super(CommandCategory.ECON, CommandCategory.MILCOM);
    }
    @Override
    public boolean checkPermission(Guild server, User user) {
        return Roles.MEMBER.has(user, server) && server != null;
    }

    @Override
    public String help() {
        return Settings.commandPrefix(true) + "warchest <*|nations|tax_url> <resources> <note>";
    }


    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.transfer.warchest.cmd);
    }

    @Override
    public String desc() {
        return """
                Determine how much to send to each member to meet their warchest requirements (per city)
                Add `-s` to skip checking stockpile
                add `-m` to convert to money
                Add `-b` to bypass checks
                Add e.g. `nation:blah` to specify a nation account
                Add e.g. `alliance:blah` to specify an alliance account
                Add e.g. `offshore:blah` to specify an offshore account
                Add e.g. `tax_id:blah` to specify a tax bracket
                add `-t` to use receivers tax bracket account""";
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        GuildDB guildDb = Locutus.imp().getGuildDB(guild);
        DBNation nationAccount = null;
        DBAlliance allianceAccount = null;
        DBAlliance offshoreAccount = null;
        TaxBracket taxAccount = null;

        String escrowModeStr = DiscordUtil.parseArg(args, "escrow");
        EscrowMode escrowMode = escrowModeStr != null ? PWBindings.EscrowMode(escrowModeStr) : null;

        String nationAccountStr = DiscordUtil.parseArg(args, "nation");
        if (nationAccountStr != null) {
            nationAccount = PWBindings.nation(author, nationAccountStr);
        }

        String allianceAccountStr = DiscordUtil.parseArg(args, "alliance");
        if (allianceAccountStr != null) {
            allianceAccount = PWBindings.alliance(allianceAccountStr);
        }

        String offshoreAccountStr = DiscordUtil.parseArg(args, "offshore");
        if (offshoreAccountStr != null) {
            offshoreAccount = PWBindings.alliance(offshoreAccountStr);
        }

        String taxIdStr = DiscordUtil.parseArg(args, "tax_id");
        if (taxIdStr == null) taxIdStr = DiscordUtil.parseArg(args, "bracket");
        if (taxIdStr != null) {
            taxAccount = PWBindings.bracket(guildDb, "tax_id=" + taxIdStr);
        }

        if (flags.contains('t')) {
            if (taxAccount != null) return "You can't specify both `tax_id` and `-t`";
        }

        if (args.size() < 3) {
            return usage("Current warchest (per city): " + ResourceType.toString(guildDb.getPerCityWarchest(me)), channel);
        }


        Map<ResourceType, Double> perCity = ResourceType.parseResources(args.get(1));
        if (perCity.isEmpty()) return "Invalid amount: `" + args.get(1) + "`";
        boolean ignoreInactives = !flags.contains('i');

        DepositType.DepositTypeInfo type = PWBindings.DepositTypeInfo(args.get(2));

        String arg = args.get(0);
        List<DBNation> nations = new ArrayList<>(DiscordUtil.parseNations(guild, author, me, arg, false, false));
        if (nations.size() != 1 || !flags.contains('b')) {
            nations.removeIf(n -> n.getPosition() <= 1);
            nations.removeIf(n -> n.getVm_turns() != 0);
            nations.removeIf(n -> n.active_m() > 2880);
            nations.removeIf(n -> n.isGray() && n.getOff() == 0);
            nations.removeIf(n -> n.isBeige() && n.getCities() <= 4);
        }
        if (nations.isEmpty()) {
            return "No nations found (add `-f` to force send)";
        }
        boolean skipStockpile = flags.contains('s');
        return UnsortedCommands.warchest(
                guildDb,
                channel,
                guild,
                author,
                me,
                new SimpleNationList(nations),
                perCity,
                type,
                skipStockpile,
                nationAccount,
                allianceAccount,
                offshoreAccount,
                taxAccount,
                flags.contains('t'),
                null,
                null,
                flags.contains('m'),
                escrowMode,
                flags.contains('b'),
                flags.contains('f'));
    }
}
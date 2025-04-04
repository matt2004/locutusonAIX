package link.locutus.discord.commands.sheets;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.WarStatus;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.PW;
import link.locutus.discord.util.battle.sim.WarNation;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.war.WarCard;
import link.locutus.discord.apiv1.enums.city.building.Buildings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import link.locutus.discord.util.scheduler.KeyValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombatantSheet extends Command {
    public CombatantSheet() {
        super(CommandCategory.GOV, CommandCategory.GAME_INFO_AND_TOOLS, CommandCategory.MILCOM);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.sheets_milcom.combatantSheet.cmd);
    }

    @Override
    public String help() {
        return super.help() + " <alliances>";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return super.checkPermission(server, user) && Roles.MILCOM.has(user, server);
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.size() != 1) return usage(args.size(), 1, channel);
        Set<Integer> alliances = DiscordUtil.parseAllianceIds(guild, args.get(0));
        if (alliances == null) return usage("Unknown alliance: `" + args.get(0) + "`", channel);

        Set<DBWar> wars = Locutus.imp().getWarDb().getActiveWars(alliances, WarStatus.ACTIVE, WarStatus.DEFENDER_OFFERED_PEACE, WarStatus.ATTACKER_OFFERED_PEACE);
        wars.removeIf(w -> {
            DBNation n1 = Locutus.imp().getNationDB().getNationById(w.getAttacker_id());
            DBNation n2 = Locutus.imp().getNationDB().getNationById(w.getDefender_id());
            if (n1 == null || n2 == null) {
                return true;
            }
            DBNation self = alliances.contains(n1.getAlliance_id()) ? n1 : n2;
            return n1.active_m() > 4320 || n2.active_m() > 4320 || self.getPosition() <= 1;
        });

        if (wars.isEmpty()) return "No wars found";

        Map<DBWar, WarCard> warMap = new HashMap<>();

        int i = 0;
        for (DBWar war : wars) {
            WarCard card = new WarCard(war, true);
            if (!card.isActive()) continue;
            warMap.put(war, card);
        }

        try {

            Map.Entry<Map<DBNation, DBNation>, Map<DBNation, DBNation>> kdMap = simulateWarsKD(warMap.values());

        GuildDB db = Locutus.imp().getGuildDB(guild);
        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.ACTIVE_COMBATANT_SHEET);

        List<Object> header = new ArrayList<>(Arrays.asList(
            "nation",
            "alliance",
            "cities",
            "avg_infra",
            "score",
            "soldier%",
            "tank%",
            "air%",
            "sea%",
            "off",
            "def",
            "-ground",
            "-air",
            "-sea",
            "'+ground",
            "'+air",
            "'+sea",
            "net_ground",
            "net_air",
            "net_sea"
        ));

        sheet.setHeader(header);

            Map<DBNation, DBNation> losses = kdMap.getValue();
            Map<DBNation, DBNation> kills = kdMap.getKey();
        for (Map.Entry<DBNation, DBNation> entry : losses.entrySet()) {
            // tank 50
            DBNation nation = entry.getKey();
            DBNation loss = entry.getValue();
            DBNation kill = kills.get(loss);

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), PW.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), PW.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getCities());
            header.set(3, nation.getAvg_infra());
            header.set(4, nation.getScore());

            double soldierMMR = (double) nation.getSoldiers() / (Buildings.BARRACKS.getUnitCap() * nation.getCities());
            double tankMMR = (double) nation.getTanks() / (Buildings.FACTORY.getUnitCap() * nation.getCities());
            double airMMR = (double) nation.getAircraft() / (Buildings.HANGAR.getUnitCap() * nation.getCities());
            double navyMMR = (double) nation.getShips() / (Buildings.DRYDOCK.getUnitCap() * nation.getCities());

            header.set(5, soldierMMR);
            header.set(6, tankMMR);
            header.set(7, airMMR);
            header.set(8, navyMMR);

            header.set(9, nation.getOff());
            header.set(10, nation.getDef());

            int groundTotal = nation.getSoldiers() + nation.getTanks() * 1000;
            {
                int groundLoss = nation.getSoldiers() - loss.getSoldiers() + (nation.getTanks() - loss.getTanks()) * 1000;
                double groundPct = 100 * (groundTotal == 0 ? -1 : -groundLoss / (double) groundTotal);
                double airPct = 100 * (nation.getAircraft() == 0 ? -1 : -(nation.getAircraft() - loss.getAircraft()) / (double) nation.getAircraft());
                double seaPct = 100 * (nation.getShips() == 0 ? -1 : -(nation.getShips() - loss.getShips()) / (double) nation.getShips());

                header.set(11, groundPct);
                header.set(12, airPct);
                header.set(13, seaPct);
                int groundKill = kill.getSoldiers() - nation.getSoldiers() + (kill.getTanks() - nation.getTanks()) * 1000;
                double groundPctKill = 100 * (groundTotal == 0 ? 0 : groundKill / (double) groundTotal);
                double airPctKill = 100 * (nation.getAircraft() == 0 ? 0 : (kill.getAircraft() - nation.getAircraft()) / (double) nation.getAircraft());
                double seaPctKill = 100 * (nation.getShips() == 0 ? 0 : (kill.getShips() - nation.getShips()) / (double) nation.getShips());

                if (groundPctKill == 0 && nation.getSoldiers() != 0) groundPctKill = 100;
                if (airPctKill == 0 && nation.getAircraft() != 0) airPctKill = 100;
                if (seaPctKill == 0 && nation.getShips() != 0) seaPctKill = 100;

                header.set(14, groundPctKill);
                header.set(15, airPctKill);
                header.set(16, seaPctKill);

                header.set(17, groundPctKill + groundPct);
                header.set(18, airPctKill + airPct);
                header.set(19, seaPctKill + seaPct);
            }

            sheet.addRow(header);
        }

        sheet.updateWrite();

            sheet.attach(channel.create(), "combatant").send();
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map.Entry<Map<DBNation, DBNation>,Map<DBNation, DBNation>> simulateWarsKD(Collection<WarCard> warcards) {
        Map<DBNation, DBNation> losses = new HashMap<>();
        Map<DBNation, DBNation> kills = new HashMap<>();
        int i = 0;
        for (WarCard warcard : warcards) {
            DBWar war = warcard.getWar();
            DBNation n1 = Locutus.imp().getNationDB().getNationById(war.getAttacker_id());
            DBNation n2 = Locutus.imp().getNationDB().getNationById(war.getDefender_id());
            WarNation attacker = warcard.toWarNation(true);
            WarNation defender = warcard.toWarNation(false);

            performAttacks(losses, kills, attacker, defender, n1, n2);

            attacker = warcard.toWarNation(true);
            defender = warcard.toWarNation(false);

            performAttacks(losses, kills, defender, attacker, n2, n1);
        }
        return new KeyValue<>(kills, losses);
    }

    public void performAttacks(Map<DBNation, DBNation> losses, Map<DBNation, DBNation> kills, WarNation attacker, WarNation defender, DBNation attackerOrigin, DBNation defenderOrigin) {
        DBNation attackerKills = kills.computeIfAbsent(attackerOrigin, f -> attackerOrigin.copy());
        DBNation defenderLosses = losses.computeIfAbsent(defenderOrigin, f -> defenderOrigin.copy());

        if (attacker.groundAttack(defender, attacker.getSoldiers(), attacker.getTanks(), true, true)) {
            addLosses(defenderOrigin, attackerKills, defenderLosses, defender);
        }
        if (attacker.airstrikeAir(defender, attacker.getAircraft(), true)) {
            addLosses(defenderOrigin, attackerKills, defenderLosses, defender);
        }
        if (attacker.naval(defender, attacker.getShips(), false)) {
            addLosses(defenderOrigin, attackerKills, defenderLosses, defender);
        }
    }

    public void addLosses(DBNation defenderOrigin,  DBNation attackerKills, DBNation defenderLosses, WarNation defender) {
        int soldierLosses = defenderOrigin.getSoldiers() - defender.getSoldiers();
        int tankLosses = defenderOrigin.getTanks() - defender.getTanks();
        int aircraftLosses = defenderOrigin.getAircraft() - defender.getAircraft();
        int shipLosses = defenderOrigin.getShips() - defender.getShips();

        defenderLosses.setSoldiers(Math.max(0, defenderLosses.getSoldiers() + soldierLosses * -1));
        defenderLosses.setTanks(Math.max(0, defenderLosses.getTanks() + tankLosses * -1));
        defenderLosses.setAircraft(Math.max(0, defenderLosses.getAircraft() + aircraftLosses * -1));
        defenderLosses.setShips(Math.max(0, defenderLosses.getShips() + shipLosses * -1));

        attackerKills.setSoldiers(Math.max(0, attackerKills.getSoldiers() + soldierLosses));
        attackerKills.setTanks(Math.max(0, attackerKills.getTanks() + tankLosses));
        attackerKills.setAircraft(Math.max(0, attackerKills.getAircraft() + aircraftLosses));
        attackerKills.setShips(Math.max(0, attackerKills.getShips() + shipLosses));
    }
}

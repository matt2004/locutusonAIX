package link.locutus.discord.commands.sheets;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.PW;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.apiv1.enums.MilitaryUnit;
import link.locutus.discord.apiv1.enums.city.JavaCity;
import link.locutus.discord.apiv1.enums.city.building.Buildings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MMRSheet extends Command {
    public MMRSheet() {
        super(CommandCategory.MILCOM, CommandCategory.GAME_INFO_AND_TOOLS, CommandCategory.GOV);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.sheets_milcom.MMRSheet.cmd);
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return Locutus.imp().getGuildDB(server).isValidAlliance() && Roles.MILCOM.has(user, server);
    }

    @Override
    public String help() {
        return super.help() + " <nations>";
    }

    @Override
    public String desc() {
        return """
                Generate a sheet of alliance/nation/city MMR
                Add `-f` to force an update
                Add `-c` to list it by cities""";
    }


    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        try {
            String sheetUrl = DiscordUtil.parseArg(args, "sheet");
            if (args.size() != 1) return usage(args.size(), 1, channel);
            if (args.get(0).contains("*")) return "Cannot use *";

            Collection<DBNation> nations = DiscordUtil.parseNations(guild, author, me, args.get(0), false, false);

            GuildDB db = Locutus.imp().getGuildDB(guild);

            SpreadSheet sheet;
            if (sheetUrl != null) {
                sheet = SpreadSheet.create(sheetUrl);
            } else {
                sheet = SpreadSheet.create(db, SheetKey.MMR_SHEET);
            }


            List<Object> header = new ArrayList<>(Arrays.asList(
                    "city",
                    "nation",
                    "alliance",
                    "\uD83C\uDFD9", // cities
                    "\uD83C\uDFD7", // avg_infra
                    "score",
                    "\uD83D\uDDE1",
                    "\uD83D\uDEE1",
                    "\uD83D\uDC82",
                    "\u2699",
                    "\u2708",
                    "\u26F5",
                    "barracks",
                    "factory",
                    "hangar",
                    "drydock",
                    "$\uD83D\uDC82",
                    "$\u2699",
                    "$A\u2708",
                    "$\u26F5"
            ));

            sheet.setHeader(header);
            nations = new ArrayList<>(nations);
            nations.removeIf(n -> n.hasUnsetMil());

            Map<Integer, Set<DBNation>> byAlliance = new HashMap<>();

            for (DBNation nation : nations) {
                byAlliance.computeIfAbsent(nation.getAlliance_id(), f -> new HashSet<>()).add(nation);
            }

            Map<Integer, DBNation> averageByAA = new HashMap<>();


            Map<DBNation, List<Object>> nationRows = new HashMap<>();

            double barracksTotal = 0;
            double factoriesTotal = 0;
            double hangarsTotal = 0;
            double drydocksTotal = 0;

            double soldierBuyTotal = 0;
            double tankBuyTotal=  0;
            double airBuyTotal = 0;
            double navyBuyTotal= 0;

            for (Map.Entry<Integer, Set<DBNation>> entry : byAlliance.entrySet()) {
                int aaId = entry.getKey();

                Set<DBNation> aaNations = entry.getValue();
                for (DBNation nation : aaNations) {

                    double barracks = 0;
                    double factories = 0;
                    double hangars = 0;
                    double drydocks = 0;

                    double soldierBuy = 0;
                    double tankBuy=  0;
                    double airBuy = 0;
                    double navyBuy= 0;

                    List<Object> row = new ArrayList<>(header);

                    Map<Integer, JavaCity> cities = nation.getCityMap(flags.contains('f'), flags.contains('f'), flags.contains('f'));
                    int i = 0;
                    for (Map.Entry<Integer, JavaCity> cityEntry : cities.entrySet()) {
                        int cityBarracks = cityEntry.getValue().getBuilding(Buildings.BARRACKS);
                        int cityFactories = cityEntry.getValue().getBuilding(Buildings.FACTORY);
                        int cityHangars = cityEntry.getValue().getBuilding(Buildings.HANGAR);
                        int cityDrydocks = cityEntry.getValue().getBuilding(Buildings.DRYDOCK);
                        barracks += cityBarracks;
                        factories += cityFactories;
                        hangars += cityHangars;
                        drydocks += cityDrydocks;
                        if (flags.contains('c')) {
                            String url = MarkupUtil.sheetUrl("CITY " + (++i), PW.City.getCityUrl(cityEntry.getKey()));
                            setRow(url, row, nation, cityBarracks, cityFactories, cityHangars, cityDrydocks, 0, 0, 0, 0);
                            sheet.addRow(row);
                        }
                    }

                    long turn = TimeUtil.getTurn();
                    long dayStart = TimeUtil.getTimeFromTurn(turn - (turn % 12));
                    soldierBuy = 100 * Locutus.imp().getNationDB().getMilitaryBuy(nation, MilitaryUnit.SOLDIER, dayStart) / (Buildings.BARRACKS.getUnitDailyBuy() * barracks);
                    tankBuy = 100 * Locutus.imp().getNationDB().getMilitaryBuy(nation, MilitaryUnit.TANK, dayStart) / (Buildings.FACTORY.getUnitDailyBuy() * factories);
                    airBuy = 100 * Locutus.imp().getNationDB().getMilitaryBuy(nation, MilitaryUnit.AIRCRAFT, dayStart) / (Buildings.HANGAR.getUnitDailyBuy() * hangars);
                    navyBuy = 100 * Locutus.imp().getNationDB().getMilitaryBuy(nation, MilitaryUnit.SHIP, dayStart) / (Buildings.DRYDOCK.getUnitDailyBuy() * drydocks);

                    if (!Double.isFinite(soldierBuy)) soldierBuy = 100;
                    if (!Double.isFinite(tankBuy)) tankBuy = 100;
                    if (!Double.isFinite(airBuy)) airBuy = 100;
                    if (!Double.isFinite(navyBuy)) navyBuy = 100;

                    barracks /= nation.getCities();
                    factories /= nation.getCities();
                    hangars /= nation.getCities();
                    drydocks /= nation.getCities();

                    barracksTotal += barracks;
                    factoriesTotal += factories;
                    hangarsTotal += hangars;
                    drydocksTotal += drydocks;

                    soldierBuyTotal += soldierBuy;
                    tankBuyTotal += tankBuy;
                    airBuyTotal += airBuy;
                    navyBuyTotal += navyBuy;

                    setRow("NATION", row, nation, barracks, factories, hangars, drydocks, soldierBuy, tankBuy, airBuy, navyBuy);
                    sheet.addRow(row);
                }

                barracksTotal /= aaNations.size();
                factoriesTotal /= aaNations.size();
                hangarsTotal /= aaNations.size();
                drydocksTotal /= aaNations.size();

                soldierBuyTotal /= aaNations.size();
                tankBuyTotal /= aaNations.size();
                airBuyTotal /= aaNations.size();
                navyBuyTotal /= aaNations.size();

                String name = PW.getName(aaId, true);
                DBNation total = DBNation.createFromList("", entry.getValue(), false);

                total.setNation_id(0);
                total.setAlliance_id(aaId);

                List<Object> row = new ArrayList<>(header);
                setRow("ALLIANCE", row, total, barracksTotal, factoriesTotal, hangarsTotal, drydocksTotal, soldierBuyTotal, tankBuyTotal, airBuyTotal, navyBuyTotal);
                sheet.addRow(row);
            }

            sheet.updateClearCurrentTab();
            sheet.updateWrite();
            String response = "";
            if (!flags.contains('f')) response += "\nNote: Results may be outdated, add `-f` to update.";

            sheet.attach(channel.create(), "mmr", response).send();
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setRow(String name, List<Object> row, DBNation nation, double barracks, double factories, double hangars, double drydocks, double soldierBuy, double tankBuy, double airBuy, double navyBuy) {
        row.set(0, name);
        row.set(1, MarkupUtil.sheetUrl(nation.getNation(), PW.getUrl(nation.getNation_id(), false)));
        row.set(2, MarkupUtil.sheetUrl(nation.getAllianceName(), PW.getUrl(nation.getAlliance_id(), true)));
        row.set(3, nation.getCities());
        row.set(4, nation.getAvg_infra());
        row.set(5, nation.getScore());
        row.set(6, nation.getOff());
        row.set(7, nation.getDef());

        double soldierPct = (double) nation.getSoldiers() / (Buildings.BARRACKS.getUnitCap() * nation.getCities());
        double tankPct = (double) nation.getTanks() / (Buildings.FACTORY.getUnitCap() * nation.getCities());
        double airPct = (double) nation.getAircraft() / (Buildings.HANGAR.getUnitCap() * nation.getCities());
        double navyPct = (double) nation.getShips() / (Buildings.DRYDOCK.getUnitCap() * nation.getCities());

        row.set(8, soldierPct);
        row.set(9, tankPct);
        row.set(10, airPct);
        row.set(11, navyPct);

        row.set(12, barracks);
        row.set(13, factories);
        row.set(14, hangars);
        row.set(15, drydocks);

        row.set(16, soldierBuy);
        row.set(17, tankBuy);
        row.set(18, airBuy);
        row.set(19, navyBuy);
    }
}
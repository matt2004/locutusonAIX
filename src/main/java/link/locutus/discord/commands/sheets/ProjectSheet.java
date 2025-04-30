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
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.apiv1.enums.city.project.Project;
import link.locutus.discord.apiv1.enums.city.project.Projects;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectSheet extends Command {
    public ProjectSheet() {
        super(CommandCategory.INTERNAL_AFFAIRS, CommandCategory.GAME_INFO_AND_TOOLS, CommandCategory.GOV);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.project.sheet.cmd);
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return Locutus.imp().getGuildDB(server).isValidAlliance() && (Roles.INTERNAL_AFFAIRS.has(user, server) || Roles.ECON.has(user, server));
    }

    @Override
    public String help() {
        return super.help() + " <nations>";
    }

    @Override
    public String desc() {
        return help() + "\n" +
                "Use `-f` to force an update of projects";
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.size() != 1) return usage(args.size(), 1, channel);

        Set<DBNation> nations = DiscordUtil.parseNations(guild, author, me, args.get(0), false, false);

        GuildDB db = Locutus.imp().getGuildDB(guild);

        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.PROJECT_SHEET);

        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "\uD83C\uDFD9", // cities
                "\uD83C\uDFD7", // avg_infra
                "score"
        ));

        for (Project value : Projects.values) {
            header.add(value.name());
        }

        if (flags.contains('f')) {
            List<Integer> ids = nations.stream().map(f -> f.getNation_id()).collect(Collectors.toList());
            Locutus.imp().runEventsAsync(events -> Locutus.imp().getNationDB().updateNations(ids, events));
        }

        sheet.setHeader(header);

        for (DBNation nation : nations) {

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), PW.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), PW.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getCities());
            header.set(3, nation.getAvg_infra());
            header.set(4, nation.getScore());

            for (int i = 0; i < Projects.values.length; i++) {
                Project project = Projects.values[i];
                header.set(5 + i, nation.hasProject(project) + "");
            }

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(channel.create(), "projects").send();
        return null;
    }
}
package link.locutus.discord.commands.buildcmd;

import link.locutus.discord.Locutus;
import link.locutus.discord.apiv1.enums.ResourceType;
import link.locutus.discord.apiv1.enums.city.JavaCity;
import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.pnw.json.CityBuild;
import link.locutus.discord.pnw.json.CityBuildRange;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.web.WebUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class AssignBuild extends Command {
    public AssignBuild() {
        super("AssignBuild", "build", CommandCategory.ECON, CommandCategory.MEMBER);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.build.assign.cmd);
    }
    public static String build(@Me IMessageIO io, GuildDB db, DBNation me, int cities, String arg) throws InterruptedException, ExecutionException, IOException {
        JavaCity to = null;
        if (arg.contains("/city/")) {
            throw new IllegalArgumentException("Not implemented.");
        } else if (arg.charAt(0) == '{') {
            CityBuild build = WebUtil.GSON.fromJson(arg, CityBuild.class);
            to = new JavaCity(build);
        } else {
            String category = arg.toLowerCase(Locale.ROOT);
            Map<String, List<CityBuildRange>> builds = db.getBuilds();
            if (!builds.containsKey(category)) {
                throw new IllegalArgumentException("No category for: " + category + ". Available categories are: " + StringMan.getString(db.getBuilds().keySet()));
            }

            List<CityBuildRange> list = builds.get(category);
            for (CityBuildRange range : list) {
                if (cities >= range.getMin() && cities <= range.getMax()) {
                    to = new JavaCity(range.getBuildGson());
                    break;
                }
            }
            if (to == null) {
                throw new IllegalArgumentException("Invalid build: " + arg);
            }
        }

        double[] totalArr = new double[ResourceType.values.length];
        Map<Integer, JavaCity> from = me.getCityMap(true);
        String instructions = to.instructions(from, totalArr, true, true);
        String emoji = "Grant";
        String command = Settings.commandPrefix(true) + "grant {usermention} " + to.toJson(false);
        io.create().embed("Build", instructions).commandButton(command, emoji).send();
        return null;
    }

    @Override
    public String help() {
        return Settings.commandPrefix(true) + "build [category]";
    }

    @Override
    public String desc() {
        return "Have the bot give you a build for war or raiding, based on your city count. Available categories are: `" + Settings.commandPrefix(true) + "build ?`";
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {

        
        if (me == null) {
            return "Invalid nation, Are you sure you are registered?" + author.getAsMention();
        }

        if (args.size() != 1) {
            return usage(args.size(), 1, channel);
        }
        GuildDB db = Locutus.imp().getGuildDB(guild);

        return build(channel, db, me, me.getCities(), args.get(0));
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return Roles.MEMBER.has(user, server);
    }
}

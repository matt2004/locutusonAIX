package link.locutus.discord.commands.external.guild;

import link.locutus.discord.commands.manager.Command;
import link.locutus.discord.commands.manager.CommandCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.NationMeta;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class Meta extends Command {
    public Meta() {
        super("NationMeta", "Meta", CommandCategory.USER_INFO, CommandCategory.INTERNAL_AFFAIRS, CommandCategory.LOCUTUS_ADMIN);
    }

    @Override
    public List<CommandRef> getSlashReference() {
        return List.of(CM.admin.debug.nation_meta.cmd);
    }
    @Override
    public String help() {
        return super.help() + " <nation> <key>";
    }

    @Override
    public boolean checkPermission(Guild server, User user) {
        return Roles.INTERNAL_AFFAIRS.hasOnRoot(user);
    }

    @Override
    public String onCommand(Guild guild, IMessageIO channel, User author, DBNation me, String fullCommandRaw, List<String> args, Set<Character> flags) throws Exception {
        if (args.size() != 2) {
            return usage(args.size(), 2, channel);
        }

        DBNation nation = DiscordUtil.parseNation(args.get(0), true, guild);
        if (nation == null) return "Invalid nation: `" + args.get(0) + "`";

        NationMeta meta;
        try {
            meta = NationMeta.valueOf(args.get(1).toUpperCase());
        } catch (IllegalArgumentException e) {
            return usage("Keys: " + StringMan.getString(NationMeta.values()), channel);
        }

        ByteBuffer buf = nation.getMeta(meta);
        if (buf == null) return "No value set.";

        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        buf = ByteBuffer.wrap(arr);

        switch (arr.length) {
            case 0 -> {
                return "" + (buf.get() & 0xFF);
            }
            case 4 -> {
                return "" + (buf.getInt());
            }
            case 8 -> {
                ByteBuffer buf2 = ByteBuffer.wrap(arr);
                return buf.getLong() + "/" + MathMan.format(buf2.getDouble());
            }
            default -> {
                return new String(arr, StandardCharsets.ISO_8859_1);
            }
        }
    }
}

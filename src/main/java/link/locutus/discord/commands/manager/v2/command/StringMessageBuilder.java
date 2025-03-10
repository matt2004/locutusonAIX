package link.locutus.discord.commands.manager.v2.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import link.locutus.discord.web.WebUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.*;

public class StringMessageBuilder extends AMessageBuilder {

    public StringMessageBuilder(StringMessageIO parent, long id, long timeCreated, User author) {
        super(parent, id, timeCreated, author);
    }

    public StringMessageBuilder(Guild guild) {
        super(new StringMessageIO(null, guild), 0, System.currentTimeMillis(), null);
    }

    public static StringMessageBuilder fromText(String body, boolean onlyParseJson, Guild guild) {
        StringMessageBuilder msg = new StringMessageBuilder(guild);
        JsonObject bodyJson = null;
        try {
            bodyJson = WebUtil.GSON.fromJson(body, JsonObject.class);
            boolean contains = false;
            for (String required : new String[]{"content", "embeds", "tables", "attachments"}) {
                if (bodyJson.has(required)) {
                    contains = true;
                    break;
                }
            }
            if (contains) {
                msg.appendJson(bodyJson);
                return msg;
            }
        } catch (Exception ignore) {}
        if (onlyParseJson) return null;
        msg.append(body);
        return msg;
    }

    public static List<StringMessageBuilder> list(User author, String... message) {
        List<StringMessageBuilder> result = new ArrayList<>();
        for (String s : message) {
            result.add(of(author, s));
        }
        return result;
    }

    public static StringMessageBuilder of(User author, String message) {
        StringMessageBuilder builder = new StringMessageBuilder(null, 0, System.currentTimeMillis(), author);
        builder.append(message);
        return builder;
    }
}

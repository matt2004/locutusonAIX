package link.locutus.discord.web.commands.page;

import io.javalin.http.Context;
import link.locutus.discord.commands.manager.v2.binding.WebStore;
import link.locutus.discord.util.MarkupUtil;
import gg.jte.generated.precompiled.auth.JteredirectGenerated;
import gg.jte.generated.precompiled.auth.JteredirectjsGenerated;
import java.net.MalformedURLException;
import java.net.URL;

public class PageHelper {
    public static String redirect(WebStore ws, Context context, String url, boolean useJs) {
        URL urlObj;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        if (useJs) {
            return WebStore.render(f -> JteredirectjsGenerated.render(f, null, ws, urlObj));
        } else {
            context.header("Referrer-Policy", "no-referrer");
            context.redirect(url);
            context.status(302);
            context.header("cache-control", "no-store");
            return WebStore.render(f -> JteredirectGenerated.render(f, null, ws, urlObj));
        }
    }
}

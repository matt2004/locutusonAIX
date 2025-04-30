package link.locutus.discord.util.update;

import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.Coalition;
import link.locutus.discord.db.entities.Treaty;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.event.treaty.*;
import link.locutus.discord.util.AlertUtil;
import link.locutus.discord.util.ImageUtil;
import link.locutus.discord.util.PW;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TreatyUpdateProcessor {

    @Subscribe
    public void onTreatyCreate(TreatyCreateEvent event) {
        update("Signed", event);
    }
    @Subscribe
    public void onTreatyCancel(TreatyCancelEvent event) {
        update("Cancelled", event);
    }
    @Subscribe
    public void onTreatyDowngrade(TreatyDowngradeEvent event) {
        update("Downgraded", event);
    }

//    @Subscribe
//    public void onTreatyExtend(TreatyExtendEvent event) {
//        update("Extended", event);
//    }

    @Subscribe
    public void onTreatyUpgraded(TreatyUpgradeEvent event) {
        update("Upgraded", event);
    }
    @Subscribe
    public void onTreatyExpire(TreatyExpireEvent event) {
        update("Expired", event);
    }

    private void update(String title, TreatyChangeEvent event) {
        Treaty previous = event.getPrevious();

        Treaty current = event.getCurrent();

        Treaty existing = previous == null ? current : previous;
        DBAlliance fromAA = DBAlliance.get(existing.getFromId());
        DBAlliance toAA = DBAlliance.get(existing.getToId());

        // Ignore treaty changes from alliance deletion
        if (fromAA == null || toAA == null) return;

        Set<Treaty> treaties = new HashSet<>();
        treaties.addAll(fromAA.getTreaties().values());
        treaties.addAll(toAA.getTreaties().values());
        Supplier<byte[]> imgSupplier = () -> {
            try {
                return ImageUtil.generateTreatyGraph(treaties);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };
        if (previous == null) {
            title += " " + current.getType();
        } else if (current == null) {
            title += " " + previous.getType();
        } else  if(current.getType() != previous.getType()) {
            title += " " + (previous.getType() + "->" + current.getType());
        } else {
            title += " " + current.getType();
        }

        int maxRank = Integer.MAX_VALUE;
        StringBuilder body = new StringBuilder();
        body.append("Time remaining: " + existing.getExpiresDiscordString() + " (" + (existing.isPermanent() ? "Permanent" : (existing.getTurnsRemaining() + " turns")) + ")\n");
        body.append("From: " + PW.getMarkdownUrl(existing.getFromId(), true));
        if (fromAA != null) {
            maxRank = fromAA.getRank();
            body.append(" - #" + maxRank + " (" + fromAA.getNations(true, 7200, true).size() + " nations)");
        }
        body.append("\n");
        body.append("To: " + PW.getMarkdownUrl(existing.getToId(), true));
        if (toAA != null) {
            int rank = toAA.getRank();
            maxRank = Math.min(maxRank, rank);
            body.append(" - #" + rank + " (" + toAA.getNations(true, 7200, true).size() + " nations)");
        }
        body.append("\n");

        String finalTitle = title + (maxRank == Integer.MAX_VALUE ? "" : " | rank #" + maxRank);
        AlertUtil.forEachChannel(f -> true, GuildKey.TREATY_ALERTS, new BiConsumer<MessageChannel, GuildDB>() {
            @Override
            public void accept(MessageChannel channel, GuildDB guildDB) {
                StringBuilder finalBody = new StringBuilder(body);
                if (guildDB.hasAlliance() || !guildDB.isWhitelisted()) {
                    Set<Integer> tracked = guildDB.getAllies(true);
                    if (!tracked.isEmpty()) {
                        finalBody.append("\n\n**IN SPHERE**");
                        tracked.addAll(guildDB.getCoalition("enemies"));
                        tracked.addAll(guildDB.getCoalition(Coalition.DNR));
                        tracked.addAll(guildDB.getCoalition(Coalition.DNR_MEMBER));
                        tracked.addAll(guildDB.getCoalition(Coalition.MASKEDALLIANCES));
                        tracked.addAll(guildDB.getCoalition(Coalition.COUNTER));
                        tracked.addAll(guildDB.getCoalition(Coalition.FA_FIRST));
                        tracked.addAll(guildDB.getCoalition(Coalition.OFFSHORE));
                        tracked.addAll(guildDB.getCoalition(Coalition.OFFSHORING));
                        tracked.addAll(guildDB.getCoalition(Coalition.TRACK_DEPOSITS));

                        if (!tracked.contains(existing.getFromId()) && !tracked.contains(existing.getToId())) {
                            return;
                        }
                    }
                }
                DiscordChannelIO io = new DiscordChannelIO(channel);
                IMessageBuilder msg = io.create().embed(finalTitle, finalBody.toString().trim());
                byte[] img = imgSupplier.get();
                if (img != null) {
                    msg.image("treaties.png", img);
                }
                msg.sendWhenFree();
            }
        });
    }
}

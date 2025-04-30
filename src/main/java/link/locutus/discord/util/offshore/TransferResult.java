package link.locutus.discord.util.offshore;

import link.locutus.discord.apiv1.enums.Rank;
import link.locutus.discord.apiv1.enums.ResourceType;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.scheduler.KeyValue;

import java.util.*;
import java.util.stream.Collectors;

public class TransferResult {
    private OffshoreInstance.TransferStatus status;
    private final NationOrAllianceOrGuild receiver;
    private final List<String> resultMessage;
    private final double[] amount;
    private final String note;

    public static Map<OffshoreInstance.TransferStatus, Integer> count(Collection<TransferResult> list) {
        Map<OffshoreInstance.TransferStatus, Integer> map = new HashMap<>();
        for (TransferResult result : list) {
            map.put(result.getStatus(), map.getOrDefault(result.getStatus(), 0) + 1);
        }
        return ArrayUtil.sortMap(map, false);
    }

    public static String toFileString(Collection<TransferResult> list) {
        return "Receiver\tStatus\tNote\tMessage\n" +
                list.stream().map(f ->
                        f.getReceiver().getName() + "\t" +
                        f.getStatus().name() + "\t" +
                        f.getNote() + "\t" +
                        f.getStatus().getMessage() + ". " + StringMan.join(f.resultMessage, ", ").replace("\n", ". ")
                ).collect(Collectors.joining("\n"));
    }

    public static Map.Entry<String, String> toEmbed(List<TransferResult> results) {
        String title, body;
        if (results.size() == 1) {
            TransferResult result = results.get(0);
            title = result.toTitleString();
            body = result.toEmbedString();
        } else {
            int success = results.stream().mapToInt(f -> f.getStatus().isSuccess() ? 1 : 0).sum();
            int escrowed = results.stream().mapToInt(f -> f.getStatus() == OffshoreInstance.TransferStatus.ESCROWED ? 1 : 0).sum();

            String mixedStatus = escrowed == success ? escrowed > 0 ? "Escrow" : "Transfer" : escrowed > 0 ? "Escrow/Transfer" : "Transfer";

            int failed = results.size() - success;
            title = mixedStatus + (success > 0 ? failed > 0 ? " with Errors" : " Success" : "Aborted");
            if (failed > 0) {
                title += " (";
                if (escrowed > 0) {
                    title += escrowed + " escrowed, ";
                }
                if (success != escrowed && success > 0) {
                    title += (success - escrowed) + " transferred, ";
                }
                if (failed > 0) {
                    title += failed + " failed";
                }
                title += ")";
            }
            body = results.stream().map(TransferResult::toLineString).collect(Collectors.joining("\n"));
        }
        return KeyValue.of(title, body);
    }

    public TransferResult(OffshoreInstance.TransferStatus status, NationOrAllianceOrGuild receiver, Map<ResourceType, Double> amount, String note) {
        this(status, receiver, ResourceType.resourcesToArray(amount), note);
    }

    public static Map<NationOrAllianceOrGuild, TransferResult> toMap(List<TransferResult> list) {
        Map<NationOrAllianceOrGuild, TransferResult> errors = new LinkedHashMap<>();
        for (TransferResult result : list) {
            TransferResult existing = errors.get(result.getReceiver());
            if (existing != null) {
                if (existing.getStatus().isSuccess() && !result.getStatus().isSuccess()) {
                    existing.status = result.status;
                }
                existing.resultMessage.addAll(result.resultMessage);
            } else {
                errors.put(result.getReceiver(), result);
            }
        }
        return errors;
    }

    public void setStatus(OffshoreInstance.TransferStatus status) {
        this.status = status;
    }

    public TransferResult(OffshoreInstance.TransferStatus status, NationOrAllianceOrGuild receiver, double[] amount, String note) {
        this.status = status;
        this.receiver = receiver;
        this.resultMessage = new ArrayList<>();
        this.amount = amount;
        this.note = note;
    }

    public TransferResult addMessage(String... messages) {
        this.resultMessage.addAll(Arrays.asList(messages));
        return this;
    }

    public TransferResult addMessages(List<String> messages) {
        this.resultMessage.addAll(messages);
        return this;
    }

    public OffshoreInstance.TransferStatus getStatus() {
        return status;
    }

    public NationOrAllianceOrGuild getReceiver() {
        return receiver;
    }

    public List<String> getMessage() {
        return resultMessage;
    }

    public String getMessageJoined(boolean dotPoints) {
        if (dotPoints) {
            return "- " + String.join("\n- ", resultMessage);
        }
        return String.join("\n", resultMessage);
    }

    public double[] getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public String toLineString() {
        String msg = "Transfer: `" + status.name() + "` to " + receiver.getMarkdownUrl() + " for `" + ResourceType.toString(amount) + "` using note `" + note + "`";
        if (!resultMessage.isEmpty()) {
            msg += "\n" + getMessageJoined(true);
        }
        return msg;
    }

    public String toEmbedString() {
        StringBuilder body = new StringBuilder();
        body.append("**Status:** `").append(status.name()).append("`\n");
        body.append("**To:** ").append(receiver.getMarkdownUrl());
        if (receiver.isNation()) {
            DBNation nation = receiver.asNation();
            if (nation.getAlliance_id() > 0) {
                body.append(" | ").append(nation.getAlliance().getMarkdownUrl());
                if (nation.getPositionEnum().id <= Rank.APPLICANT.id) {
                    body.append(" (applicant)");
                }
            } else {
                body.append(" | AA:0");
            }
        }
        body.append("\n");
        body.append("**Amount:** `").append(ResourceType.toString(amount)).append("`\n");
        body.append(" - worth: `$" + MathMan.format(ResourceType.convertedTotal(amount)) + "`\n");
        body.append("**Note:** `").append(note).append("`\n");
        if (!resultMessage.isEmpty()) {
            if (resultMessage.size() == 1) {
                body.append("**Response:** ").append(getMessageJoined(false));
            } else if (!resultMessage.isEmpty()) {
                body.append("**Response:**\n").append(getMessageJoined(true));
            }
        }
        return body.toString();
    }

    public String toTitleString() {
        String title;
        if (status.isSuccess()) {
            title = "Successfully " + (status == OffshoreInstance.TransferStatus.ESCROWED ? "escrowed" : "transferred");
        } else {
            title = "Failed to " + (status == OffshoreInstance.TransferStatus.ESCROWED ? "escrow" : "transfer");
        }
        title += " to " + receiver.getTypePrefix() + ":" + receiver.getName();
        if (status != OffshoreInstance.TransferStatus.SUCCESS) {
            title += " (" + status.name() + ")";
        }
        return title;
    }
}

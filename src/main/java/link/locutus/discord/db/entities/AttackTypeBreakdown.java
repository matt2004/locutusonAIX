package link.locutus.discord.db.entities;

import link.locutus.discord.apiv1.domains.subdomains.attack.v3.AbstractCursor;
import link.locutus.discord.apiv3.enums.AttackTypeSubCategory;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.update.WarUpdateProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AttackTypeBreakdown {
    private final String nameB;
    private final String nameA;
    private final AtomicInteger totalA = new AtomicInteger();
    private final AtomicInteger totalB = new AtomicInteger();
    private final Map<AttackTypeSubCategory, Integer> mapA = new HashMap<>();
    private final Map<AttackTypeSubCategory, Integer> mapB = new HashMap<>();

    public AttackTypeBreakdown(String nameA, String nameB) {
        this.nameA = nameA;
        this.nameB = nameB;
    }
    public int getTotalAttacks(boolean primary) {
        return (primary ? totalA : totalB).get();
    }

    public int getTotalAttacks(AttackTypeSubCategory category, boolean primary) {
        return (primary ? mapA : mapB).getOrDefault(category, 0);
    }

    public void addAttack(DBWar war, AbstractCursor attack, boolean isAttacker) {
        addAttack(war, attack, (w, a) -> isAttacker, (w, a) -> !isAttacker);
    }

    public AttackTypeBreakdown addAttack(DBWar war, AbstractCursor attack, BiFunction<DBWar, AbstractCursor, Boolean> isPrimary, BiFunction<DBWar, AbstractCursor, Boolean> isSecondary) {
        boolean primary = isPrimary.apply(war, attack);
        boolean secondary = isSecondary.apply(war, attack);
        if (!primary && !secondary) return this;

        Map<AttackTypeSubCategory, Integer> map = primary ? mapA : mapB;
        AtomicInteger total = primary ? totalA : totalB;

        AttackTypeSubCategory category = WarUpdateProcessor.incrementCategory(attack, map);
        if (category != null) {
            total.incrementAndGet();
        }
        return this;
    }

    public List<List<String>> toTableList() {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = Arrays.asList("type", nameA, nameB);
        rows.add(row);

        List<AttackTypeSubCategory> allTypes = new ArrayList<>();
        allTypes.addAll(mapA.keySet());
        allTypes.addAll(mapB.keySet());
        Collections.sort(allTypes);
        allTypes = new ArrayList<>(new LinkedHashSet<>(allTypes));
        for (AttackTypeSubCategory type : allTypes) {
            String amtA = MathMan.format(mapA.getOrDefault(type, 0));
            String amtB = MathMan.format(mapB.getOrDefault(type, 0));
            rows.add(Arrays.asList(type.name(), amtA, amtB));
        }
        rows.add(Arrays.asList("TOTAL", MathMan.format(totalA.get()), MathMan.format(totalB.get())));
        return rows;
    }
}

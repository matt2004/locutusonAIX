package link.locutus.discord.apiv1.domains.subdomains.attack.v3.cursors;

import com.politicsandwar.graphql.model.WarAttack;
import link.locutus.discord.apiv1.domains.subdomains.attack.DBAttack;
import link.locutus.discord.apiv1.domains.subdomains.attack.v3.FailedCursor;
import link.locutus.discord.apiv1.enums.AttackType;
import link.locutus.discord.apiv1.enums.ResourceType;
import link.locutus.discord.apiv1.enums.SuccessType;
import link.locutus.discord.db.WarDB;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.util.io.BitBuffer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ALootCursor extends FailedCursor {
    public boolean hasLoot = false;
    public double[] looted = ResourceType.getBuffer();
    private int loot_percent_cents;
    private int alliance_id;

    @Override
    public double[] addAttLoot(double[] buffer) {
        if (hasLoot) {
            ResourceType.subtract(buffer, looted);
        }
        return buffer;
    }

    @Override
    public double[] addDefLoot(double[] buffer) {
        if (hasLoot) {
            ResourceType.add(buffer, looted);
        }
        return buffer;
    }

    @Override
    public double getAttLootValue() {
        return -ResourceType.convertedTotal(looted);
    }

    @Override
    public double getDefLootValue() {
        return ResourceType.convertedTotal(looted);
    }

    public double[] addDefLosses(double[] buffer, DBWar war) {
        return addDefLoot(buffer);
    }
    public double[] addAttLosses(double[] buffer, DBWar war) {
        ResourceType.subtract(buffer, looted);
        return buffer;
    }
    public double getAttLossValue(DBWar war) {
        return -ResourceType.convertedTotal(looted);
    }
    public double getDefLossValue(DBWar war) {
        return ResourceType.convertedTotal(looted);
    }

    @Override
    public SuccessType getSuccess() {
        return SuccessType.PYRRHIC_VICTORY;
    }

    @Override
    public void load(DBAttack legacy) {
        super.load(legacy);
        this.hasLoot = legacy.loot != null && !ResourceType.isZero(legacy.loot);
        this.loot_percent_cents = (int) (legacy.getLootPercent() * 100 * 100);
        this.alliance_id = legacy.getLooted() == null ? 0 : legacy.getLooted();
        if (hasLoot) {
            this.looted = legacy.loot;
        } else if (legacy.getMoney_looted() > 0) {
            hasLoot = true;
            Arrays.fill(looted, 0);
            looted[ResourceType.MONEY.ordinal()] = legacy.getMoney_looted();
        }
    }

    @Override
    public double[] getLoot() {
        return hasLoot ? looted : null;
    }

    @Override
    public double getLootPercent() {
        return loot_percent_cents * 0.0001d;
    }

    @Override
    public AttackType getAttack_type() {
        return AttackType.A_LOOT;
    }

    @Override
    public void load(WarAttack attack, WarDB db) {
        super.load(attack, db);
        String note = attack.getLoot_info();
        Arrays.fill(looted, 0);
        if (note != null) {
            looted[ResourceType.MONEY.ordinal()] = attack.getMoney_looted();
            looted[ResourceType.COAL.ordinal()] = attack.getCoal_looted();
            looted[ResourceType.OIL.ordinal()] = attack.getOil_looted();
            looted[ResourceType.URANIUM.ordinal()] = attack.getUranium_looted();
            looted[ResourceType.IRON.ordinal()] = attack.getIron_looted();
            looted[ResourceType.BAUXITE.ordinal()] = attack.getBauxite_looted();
            looted[ResourceType.LEAD.ordinal()] = attack.getLead_looted();
            looted[ResourceType.GASOLINE.ordinal()] = attack.getGasoline_looted();
            looted[ResourceType.MUNITIONS.ordinal()] = attack.getMunitions_looted();
            looted[ResourceType.STEEL.ordinal()] = attack.getSteel_looted();
            looted[ResourceType.ALUMINUM.ordinal()] = attack.getAluminum_looted();
            looted[ResourceType.FOOD.ordinal()] = attack.getFood_looted();

            AtomicInteger allianceId = new AtomicInteger();
            loot_percent_cents = (int) (DBAttack.parseBankLoot(note, allianceId, null) * 100 * 100);
            this.alliance_id = allianceId.get();
            if (alliance_id == 0) {
                DBWar war = getWar(db);
                if (war != null) {
                    alliance_id = war.getAttacker_id() == attacker_id ? war.getDefender_aa() : war.getAttacker_aa();
                }
            }

            hasLoot = !ResourceType.isZero(looted);
        } else {
            loot_percent_cents = 0;
            hasLoot = false;
            alliance_id = 0;
        }
    }

    @Override
    public int getAllianceIdLooted() {
        return alliance_id;
    }

    @Override
    public void load(DBWar war, BitBuffer input) {
        super.load(war, input);
        // load resources
        hasLoot = input.readBit();
        if (hasLoot) {
            loot_percent_cents = input.readVarInt();
            for (ResourceType type : ResourceType.values) {
                if (type == ResourceType.CREDITS) continue;
                looted[type.ordinal()] = input.readVarLong() * 0.01d;
            }
        } else {
            loot_percent_cents = 0;
        }
        if (input.readBit()) {
            alliance_id = input.readVarInt();
        }
    }

    @Override
    public Set<Integer> getCityIdsDamaged() {
        return Collections.emptySet();
    }

    @Override
    public void serialze(BitBuffer output) {
        super.serialze(output);
        // add current
        output.writeBit(hasLoot);
        if (hasLoot) {
            output.writeVarInt(loot_percent_cents);
            for (ResourceType type : ResourceType.values) {
                if (type == ResourceType.CREDITS) continue;
                output.writeVarLong((long) (looted[type.ordinal()] * 100));
            }
        }
        output.writeBit(alliance_id != 0);
        if (alliance_id != 0) {
            output.writeVarInt(alliance_id);
        }
    }
}

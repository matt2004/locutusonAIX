package link.locutus.discord.apiv1.domains.subdomains.attack.v3;

import com.politicsandwar.graphql.model.WarAttack;
import link.locutus.discord.apiv1.domains.subdomains.attack.DBAttack;
import link.locutus.discord.apiv1.enums.*;
import link.locutus.discord.apiv1.enums.city.building.Building;
import link.locutus.discord.db.WarDB;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.io.BitBuffer;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.Map;
import java.util.Set;

public abstract class AbstractCursor implements IAttack {
    protected DBWar war_cached;
    protected int war_attack_id;
    protected long date;
    protected int war_id;
    protected int attacker_id;
    protected int defender_id;

    public void load(DBAttack legacy) {
        war_attack_id = legacy.getWar_attack_id();
        date = Math.max(TimeUtil.getOrigin(), legacy.getDate());
        war_id = legacy.getWar_id();
        attacker_id = legacy.getAttacker_id();
        defender_id = legacy.getDefender_id();
    }

    @Override
    public abstract AttackType getAttack_type();
    @Override
    public abstract SuccessType getSuccess();

    public void load(WarAttack attack, WarDB db) {
        war_cached = null;
        war_attack_id = attack.getId();
        date = attack.getDate().toEpochMilli();
        long now =  System.currentTimeMillis();
        if (date > now) {
            System.err.println("Attack date is in the future: " + date);
            date = now;
        }
        war_id = attack.getWar_id();
        attacker_id = attack.getAtt_id();
        defender_id = attack.getDef_id();
    }

    public void serialze(BitBuffer output) {
        output.writeInt(war_attack_id);
        output.writeVarLong(date - TimeUtil.getOrigin());
        output.writeInt(war_id);
        output.writeBit(attacker_id > defender_id);
    }

    public void initialize(DBWar war, BitBuffer input) {
        war_cached = war;
        war_attack_id = input.readInt();
        date = input.readVarLong() + TimeUtil.getOrigin();
        war_id = input.readInt();
        boolean isAttackerGreater = input.readBit();
        if (isAttackerGreater) {
            if (war.getAttacker_id() > war.getDefender_id()) {
                attacker_id = war.getAttacker_id();
                defender_id = war.getDefender_id();
            } else {
                attacker_id = war.getDefender_id();
                defender_id = war.getAttacker_id();
            }
        } else {
            if (war.getAttacker_id() > war.getDefender_id()) {
                attacker_id = war.getDefender_id();
                defender_id = war.getAttacker_id();
            } else {
                attacker_id = war.getAttacker_id();
                defender_id = war.getDefender_id();
            }
        }
    }

    public void load(DBWar war, BitBuffer input) {

    }


    @Override
    public boolean isAttackerIdGreater() {
        return attacker_id > defender_id;
    }

    @Override
    public int getAttacker_id() {
        return attacker_id;
    }

    @Override
    public int getDefender_id() {
        return defender_id;
    }

    @Override
    public int getWar_attack_id() {
        return war_attack_id;
    }

    public long getDate() {
        return date;
    }

    @Override
    public int getWar_id() {
        return war_id;
    }

    @Override
    public DBWar getWar(WarDB db) {
        if (war_cached == null) {
            war_cached = db.getWar(war_id);
        }
        return war_cached;
    }

//    public Map<ResourceType, Double> getLosses2(boolean attacker, boolean units, boolean infra, boolean consumption, boolean includeLoot, boolean includeBuildings) {
//        double[] buffer = ResourceType.getBuffer();
//        addLosses(buffer, attacker, units, infra, consumption, includeLoot, includeBuildings);
//        return PW.resourcesToMap(buffer);
//    }

//    @Override
//    public double[] getLosses(double[] buffer, boolean attacker, boolean units, boolean infra, boolean consumption, boolean includeLoot, boolean includeBuildings, Function<Research, Integer> research) {
//        if (units) {
//            getUnitLossCost(buffer, attacker, research);
//        }
//        if (includeLoot) {
//            double[] loot = getLoot();
//            if (loot != null) {
//                if (attacker) {
//                    ResourceType.subtract(buffer, loot);
//                } else {
//                    ResourceType.add(buffer, loot);
//                }
//            }
//            else if (getMoney_looted() != 0) {
//                buffer[ResourceType.MONEY.ordinal()] += attacker ? -getMoney_looted() : getMoney_looted();
//            }
//        }
//        if (!attacker) {
//            if (infra && getInfra_destroyed_value() != 0) {
//                buffer[ResourceType.MONEY.ordinal()] += getInfra_destroyed_value();
//            }
//        }
//
//        if (consumption) {
//            double mun = attacker ? getAtt_mun_used() : getDef_mun_used();
//            double gas = attacker ? getAtt_gas_used() : getDef_gas_used();
//            if (mun > 0) {
//                buffer[ResourceType.MUNITIONS.ordinal()] += mun;
//            }
//            if (gas > 0) {
//                buffer[ResourceType.GASOLINE.ordinal()] += gas;
//            }
//        }
//
//        if (includeBuildings && !attacker) {
//            buffer = getBuildingCost(buffer);
//        }
//        return buffer;
//    }

    public abstract Map<Building, Integer> getBuildingsDestroyed();

    public abstract Set<Integer> getCityIdsDamaged();

    public abstract int[] getAttUnitLosses(int[] buffer);
    public abstract int[] getDefUnitLosses(int[] buffer);

    public abstract int getAttUnitLosses(MilitaryUnit unit);
    public abstract int getDefUnitLosses(MilitaryUnit unit);

    public Map<MilitaryUnit, Integer> getUnitLosses2(boolean isAttacker) {
        int[] buffer = MilitaryUnit.getBuffer();
        return ArrayUtil.toMap(getUnitLosses(buffer, isAttacker), MilitaryUnit.values);
    }
}

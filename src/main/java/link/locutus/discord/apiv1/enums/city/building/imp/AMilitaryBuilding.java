package link.locutus.discord.apiv1.enums.city.building.imp;

import link.locutus.discord.apiv1.enums.BuildingType;
import link.locutus.discord.apiv1.enums.MilitaryUnit;
import link.locutus.discord.apiv1.enums.city.building.MilitaryBuilding;

public class AMilitaryBuilding extends ABuilding implements MilitaryBuilding {
    private final MilitaryUnit unit;
    private final int max;
    private final int perDay;
    private final double requiredCitizens;

    public AMilitaryBuilding(BuildingBuilder parent, MilitaryUnit unit, int max, int perDay, double requiredCitizens) {
        super(parent);
        this.unit = unit;
        this.max = max;
        this.perDay = perDay;
        this.requiredCitizens = requiredCitizens;
    }

    @Override
    public MilitaryUnit getMilitaryUnit() {
        return unit;
    }

    @Override
    public double getCitizensPerUnit() {
        return requiredCitizens;
    }

    /**
     * max unit
     * @return
     */
    @Override
    public int getUnitCap() {
        return max;
    }

    @Override
    public int getUnitDailyBuy() {
        return perDay;
    }

    @Override
    public BuildingType getType() {
        return BuildingType.MILITARY;
    }
}
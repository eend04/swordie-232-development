package net.swordie.ms.constants;

import net.swordie.ms.enums.ItemGrade;

import java.util.Objects;

public class WeightedItemOptionKey {

    private final int itemId;
    private final int upgradeLevel;
    private final boolean bonus;
    private final ItemGrade grade;

    public WeightedItemOptionKey(int itemId, int upgradeLevel, boolean bonus, ItemGrade grade) {
        this.itemId = itemId;
        this.upgradeLevel = upgradeLevel;
        this.bonus = bonus;
        this.grade = grade;
    }

    public int getItemId() {
        return itemId;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public boolean isBonus() {
        return bonus;
    }

    public ItemGrade getGrade() {
        return grade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeightedItemOptionKey that = (WeightedItemOptionKey) o;
        return itemId == that.itemId &&
                upgradeLevel == that.upgradeLevel &&
                bonus == that.bonus &&
                grade == that.grade;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, upgradeLevel, bonus, grade);
    }
}
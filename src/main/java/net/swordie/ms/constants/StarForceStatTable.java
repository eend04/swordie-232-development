package net.swordie.ms.constants;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.enums.EnchantStat;

public final class StarForceStatTable {

    private StarForceStatTable() {
    }

    /*
     * Class-stat increases for weapons at stars 16-22.
     * Columns: 128-137, 138-149, 150-159, 160-199, 200-249.
     */
    private static final int[][] WEAPON_CLASS_STAT_DELTA = {
            {7, 9, 11, 13, 15}, // 16★
            {7, 9, 11, 13, 15}, // 17★
            {7, 9, 11, 13, 15}, // 18★
            {7, 9, 11, 13, 15}, // 19★
            {7, 9, 11, 13, 15}, // 20★
            {0, 9, 11, 13, 15}, // 21★
            {0, 9, 11, 13, 15}  // 22★
    };

    /*
     * Class-stat increases for armor, accessories and badges at stars 16-22.
     * Columns: 128-137, 138-149, 150-159, 160-199, 200-249, 250.
     */
    private static final int[][] ARMOR_CLASS_STAT_DELTA = {
            {7, 9, 11, 13, 15, 17}, // 16★
            {7, 9, 11, 13, 15, 17}, // 17★
            {7, 9, 11, 13, 15, 17}, // 18★
            {7, 9, 11, 13, 15, 17}, // 19★
            {7, 9, 11, 13, 15, 17}, // 20★
            {0, 9, 11, 13, 15, 17}, // 21★
            {0, 9, 11, 13, 15, 17}  // 22★
    };

    /*
     * Weapon ATT/M.ATT increases at stars 16-25.
     * Columns: 128-137, 138-149, 150-159, 160-199, 200-249.
     */
    private static final int[][] WEAPON_ATTACK_DELTA = {
            {6, 7, 8, 9, 13},       // 16★
            {6, 7, 8, 9, 13},       // 17★
            {7, 8, 9, 10, 14},      // 18★
            {8, 9, 10, 11, 14},     // 19★
            {9, 10, 11, 12, 15},    // 20★
            {0, 11, 12, 13, 16},    // 21★
            {0, 12, 13, 14, 17},    // 22★
            {0, 30, 31, 32, 34},    // 23★
            {0, 31, 32, 33, 35},    // 24★
            {0, 32, 33, 34, 36}     // 25★
    };

    /*
     * Armor/accessory ATT/M.ATT increases at stars 16-30.
     * Columns: 128-137, 138-149, 150-159, 160-199, 200-249, 250.
     */
    private static final int[][] ARMOR_ATTACK_DELTA = {
            {7, 8, 9, 10, 12, 14},   // 16★
            {8, 9, 10, 11, 13, 15},  // 17★
            {9, 10, 11, 12, 14, 16}, // 18★
            {10, 11, 12, 13, 15, 17},// 19★
            {11, 12, 13, 14, 16, 18},// 20★
            {0, 13, 14, 15, 17, 19}, // 21★
            {0, 15, 16, 17, 19, 21}, // 22★
            {0, 17, 18, 19, 21, 23}, // 23★
            {0, 19, 20, 21, 23, 25}, // 24★
            {0, 21, 22, 23, 25, 27}, // 25★
            {0, 22, 23, 24, 26, 28}, // 26★
            {0, 23, 24, 25, 27, 29}, // 27★
            {0, 24, 25, 26, 28, 30}, // 28★
            {0, 25, 26, 27, 29, 31}, // 29★
            {0, 26, 27, 28, 30, 32}  // 30★
    };

    /**
     * Returns the GMS per-star value for the requested stat.
     *
     * A null result means the Wiki does not provide a value for this
     * equipment level/star combination. The caller should then retain
     * the existing server calculation.
     */
    static Integer getDelta(Equip equip, EnchantStat es, short chuc, int curAmount) {
        int star = chuc + 1;
        int reqLevel = equip.getReqLevel() + equip.getiIncReq();
        int itemId = equip.getItemId();

        if (star < 1) {
            return null;
        }

        boolean weapon = ItemConstants.isWeapon(itemId)
                && !ItemConstants.isSecondary(itemId);
        boolean badge = ItemConstants.isBadge(itemId);

        if (weapon) {
            int bracket = getWeaponLevelBracket(reqLevel);

            if (bracket < 0) {
                return null;
            }

            if (isClassStat(es)) {
                if (star <= 15) {
                    return getCommonClassStatDelta(star);
                }
                if (star <= 22) {
                    return WEAPON_CLASS_STAT_DELTA[star - 16][bracket];
                }
                if (star <= 25) {
                    return 0;
                }
                return null;
            }

            if (es.isAttackType()) {
                if (star > 25) {
                    return null;
                }

                // GMS only increases the type of attack the weapon already has.
                if (curAmount <= 0) {
                    return 0;
                }

                // Stars 1-15 use the updated weapon ATT/M.ATT value each time.
                if (star <= 15) {
                    return (int) Math.floor(curAmount * 0.02D + 1D);
                }

                return WEAPON_ATTACK_DELTA[star - 16][bracket];
            }

            return null;
        }

        int bracket = getArmorLevelBracket(reqLevel);

        if (bracket < 0) {
            return null;
        }

        if (badge) {
            if (isClassStat(es)) {
                if (star <= 15) {
                    return getCommonClassStatDelta(star);
                }
                if (star <= 22) {
                    return ARMOR_CLASS_STAT_DELTA[star - 16][bracket];
                }
                return null;
            }

            // Badges only receive All Stats in the published table.
            if (es.isAttackType() && star <= 22) {
                return 0;
            }

            return null;
        }

        if (isClassStat(es)) {
            if (star <= 15) {
                return getCommonClassStatDelta(star);
            }
            if (star <= 22) {
                return ARMOR_CLASS_STAT_DELTA[star - 16][bracket];
            }
            if (star <= 30) {
                // Armor/accessories receive no additional class stats at 23-30★.
                return 0;
            }
            return null;
        }

        if (es.isAttackType()) {
            if (star <= 15) {
                // Gloves receive +1 ATT/M.ATT at these specific stars.
                return ItemConstants.isGlove(itemId) && isGloveAttackStar(star) ? 1 : 0;
            }
            if (star <= 30) {
                return ARMOR_ATTACK_DELTA[star - 16][bracket];
            }
            return null;
        }

        return null;
    }

    private static boolean isClassStat(EnchantStat es) {
        return es == EnchantStat.STR
                || es == EnchantStat.DEX
                || es == EnchantStat.INT
                || es == EnchantStat.LUK;
    }

    private static int getCommonClassStatDelta(int star) {
        if (star >= 1 && star <= 5) {
            return 2;
        }
        if (star >= 6 && star <= 15) {
            return 3;
        }
        return 0;
    }

    private static boolean isGloveAttackStar(int star) {
        return star == 5
                || star == 7
                || star == 9
                || star == 11
                || star == 13
                || star == 14
                || star == 15;
    }

    private static int getWeaponLevelBracket(int level) {
        if (level >= 128 && level <= 137) return 0;
        if (level >= 138 && level <= 149) return 1;
        if (level >= 150 && level <= 159) return 2;
        if (level >= 160 && level <= 199) return 3;
        if (level >= 200 && level <= 249) return 4;
        return -1;
    }

    private static int getArmorLevelBracket(int level) {
        if (level >= 128 && level <= 137) return 0;
        if (level >= 138 && level <= 149) return 1;
        if (level >= 150 && level <= 159) return 2;
        if (level >= 160 && level <= 199) return 3;
        if (level >= 200 && level <= 249) return 4;
        if (level == 250) return 5;
        return -1;
    }
}
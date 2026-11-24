package net.swordie.ms.handlers.item;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.packet.ZeroPool;
import net.swordie.ms.connection.packet.field.FieldPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.ICOGProbabilityCalculator;
import net.swordie.ms.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.EquipBaseStat.*;
import static net.swordie.ms.enums.InvType.*;

public class ItemUpgradeHandler {

    private static final Logger log = LogManager.getLogger();


    @Handler(op = InHeader.USER_EX_ITEM_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserExItemUpgradeItemUseRequest(Client c, InPacket inPacket) {
        inPacket.decodeInt(); //tick
        var usePosition = inPacket.decodeShort();
        var eqpPosition = inPacket.decodeShort();
        var enchantSkill = inPacket.decodeByte() != 0;

        Char chr = c.getChr();
        Item flame = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(usePosition);
        var invType = eqpPosition < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(eqpPosition);

        if (flame == null || equip == null || !ItemConstants.canEquipHaveFlame(equip)) {
            chr.chatMessage(SystemNotice, "Could not find flame or equip.");
            chr.dispose();
            return;
        }

        if (!ItemConstants.isRebirthFlame(flame.getItemId())) {
            chr.chatMessage(SystemNotice, "This item is not a rebirth flame.");
            chr.dispose();
            return;
        }

        if (!flameEquip(chr, equip, eqpPosition, flame, true)) {
            return;
        }

        chr.dispose();
    }

    private static boolean flameEquip(Char chr, Equip equip, short ePos, Item flame, boolean consumeItem) {
        Client c = chr.getClient();
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(flame.getItemId()).getScrollStats();
        if (vals.size() > 0) {
            int reqEquipLevelMax = vals.getOrDefault(ScrollStat.reqEquipLevelMax, 250);

            if (equip.getReqLevel() + equip.getiIncReq() > reqEquipLevelMax) {
                c.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("Equipment level does not meet scroll requirements.")));
                chr.dispose();
                return false;
            }

            boolean success = Util.succeedProp(vals.getOrDefault(ScrollStat.success, 100));

            if (success) {
                boolean eternalFlame =
    flame.getItemId() == ItemConstants.ETERNAL_REBIRTH_FLAME;
equip.randomizeFlameStats(eternalFlame);

                updateZeroEgoEquipComplete(chr, equip, ePos, true);
            }

            if (consumeItem) {
                c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, flame.getItemId(), equip.getItemId(), false));
                equip.updateToChar(chr);
                chr.consumeItem(flame);
                chr.write(WvsContext.rebirthFlameResult(flame.getItemId(), equip.getBagIndexWithEquipped(), 0));
            }
        }
        return true;
    }

    @Handler(op = InHeader.USER_MEMORIAL_CUBE_OPTION_REQUEST)
    public static void handleUserMemorialCubeOptionRequest(Char chr, InPacket inPacket) {
        MemorialCubeInfo mci = chr.getMemorialCubeInfo();
        if (mci == null) {
            return;
        }

        inPacket.decodeInt(); // tick
        boolean chooseBefore = inPacket.decodeByte() == 7;
        mci.applyPotential(chooseBefore);
        mci.getEquip().updateToChar(chr);
        chr.setMemorialCubeInfo(null);
    }

    @Handler(op = InHeader.GOLD_HAMMER_REQUEST)
    public static void handleGoldHammerRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int iPos = inPacket.decodeInt(); // hammer slot
        int hammerID = inPacket.decodeInt(); // hammer item id
        int eInvType = inPacket.decodeInt(); // use hammer? useless though
        int ePos = inPacket.decodeInt(); // equip slot

        InvType invType = InvType.getInvTypeByVal(eInvType);
        if (invType.equals(EQUIP) && ePos < 0) {
            invType = EQUIPPED;
        }
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot((short) ePos);
        Item hammer = chr.getInventoryByType(CONSUME).getItemBySlot((short) iPos);

        if (hammerEquip(chr, equip, hammer, hammerID, ePos)) {
            chr.consumeItem(hammerID, 1);
        }
    }

    private static boolean hammerEquip(Char chr, Equip equip, Item hammer, int hammerID, int ePos) {
        if (chr.getClient().getWorld().isReboot()) {
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 1, 0));
            chr.getOffenseManager().addOffense(String.format("Character %d attempted to hammer in reboot world.", chr.getId()));
            return false;
        }

        short maxHammers = ItemConstants.MAX_HAMMER_SLOTS;

        if (equip != null) {
            var defaultEquip = ItemData.getEquipInfoById(equip.getItemId());
            if (defaultEquip.isHasIUCMax()) {
                maxHammers = defaultEquip.getIucMax();
            }
        }

        if (equip == null || !ItemConstants.canEquipGoldHammer(equip) ||
                hammer == null || !ItemConstants.isGoldHammer(hammer) || hammerID != hammer.getItemId()) {
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 1, 0));
            chr.getOffenseManager().addOffense(String.format("Character %d tried to use hammer (id %d) on an invalid equip (id %d)",
                    chr.getId(), hammer == null ? 0 : hammer.getItemId(), equip == null ? 0 : equip.getItemId()));
            return false;
        }

        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(hammer.getItemId()).getScrollStats();

        if (vals.size() > 0) {
            if (equip.getIuc() >= maxHammers) {
                chr.getOffenseManager().addOffense(String.format("Character %d tried to use hammer (id %d) an invalid equip (id %d)",
                        chr.getId(), hammerID, equip.getItemId()));
                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 2, 0));
                return false;
            }

            boolean success = Util.succeedProp(vals.getOrDefault(ScrollStat.success, 100));

            if (success) {
                equip.addStat(iuc, 1); // +1 hammer used
                equip.addStat(tuc, 1); // +1 upgrades available
                equip.updateToChar(chr);

                updateZeroEgoEquipComplete(chr, equip, ePos, true);

                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Success, 0, equip.getIuc()));
            } else {
                chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Fail, 1, equip.getIuc()));
            }
        }
        return true;
    }

    @Handler(op = InHeader.GOLD_HAMMER_COMPLETE)
    public static void handleGoldHammerComplete(Char chr, InPacket inPacket) {
        int returnResult = inPacket.decodeInt();
        int result = inPacket.decodeInt();
        if (returnResult == GoldHammerResult.Success.getVal() || returnResult == GoldHammerResult.Fail.getVal()) {
            //I think its ok to just send back the result given.
            chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Done, result, 0));
        } else {
            chr.getOffenseManager().addOffense(String.format("Character %d have invalid gold hammer complete returnResult %d",
                    chr.getId(), returnResult));
        }
    }

    @Handler(op = InHeader.USER_UPGRADE_ASSIST_ITEM_USE_REQUEST)
    public static void handleUserUpgradeAssistItemUseRequest(Client c, InPacket inPacket) {

        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to scroll in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort(); //Use Position
        var invType = InvType.getInvTypeByVal(inPacket.decodeShort());
        short ePos = inPacket.decodeShort(); //Eqp Position
        byte bEnchantSkill = inPacket.decodeByte(); //no clue what this means exactly
//        short idk = inPacket.decodeShort(); //No clue what this is, stayed  00 00  throughout different tests
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        invType = ePos < 0 ? EQUIPPED : invType;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            return;
        }
        int scrollID = scroll.getItemId();
        switch (scrollID) {
            case 2532000: // Safety Scroll
            case 2532001: // Pet Safety Scroll
            case 2532002: // Safety Scroll
            case 2532003: // Safety Scroll
            case 2532004: // Pet Safety Scroll
            case 2532005: // Safety Scroll
                equip.addAttribute(EquipAttribute.UpgradeCountProtection);
                break;
            case 2530000: // Lucky Day
            case 2530002: // Lucky Day
            case 2530003: // Pet Lucky Day
            case 2530004: // Lucky Day
            case 2530006: // Pet Lucky Day
                equip.addAttribute(EquipAttribute.LuckyDay);
                break;
            case 2531000: // Protection Scroll
            case 2531001:
            case 2531004:
            case 2531005:
                equip.addAttribute(EquipAttribute.ProtectionScroll);
                break;
        }
        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), true, false, scrollID, equip.getItemId(), false));

        updateZeroEgoEquipComplete(chr, equip, ePos, true);

        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_HYPER_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserHyperUpgradeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        var usePos = inPacket.decodeShort();
        var equipPos = inPacket.decodeShort();

        var scroll = chr.getConsumeInventory().getItemBySlot(usePos);
        var equip = equipPos < 0
                ? chr.getEquippedInventory().getItemBySlot(Math.abs(equipPos))
                : chr.getEquipInventory().getItemBySlot(equipPos);

        if (scroll == null || !(equip instanceof Equip)) {
            chr.chatMessage("Could not find the scroll or the equip.");
            chr.dispose();
            return;
        }

        boolean advanced = false;
        boolean safeguard = false;

        switch (scroll.getItemId()) {
            case 2049301: // Normal EE
            case 2049307:
                break;
            case 2049023: // AEE
            case 2049300:
            case 2049303:
            case 2049306:
            case 2049323:
                advanced = true;
                break;
            case 2049325: // Noboom AEE
                advanced = true;
                safeguard = true;
                break;
        }

        boolean success = ItemHandlerModule.enhanceItem(chr, (Equip) equip, advanced, safeguard);
        if (!success) {
            chr.dispose();
        } else {
            chr.consumeItem(scroll);
        }
    }

    @Handler(op = InHeader.USER_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to scroll in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort(); //Use Position
        var invType = InvType.getInvTypeByVal(inPacket.decodeShort());
        short ePos = inPacket.decodeShort(); //Eqp Position
        byte bEnchantSkill = inPacket.decodeByte(); //no clue what this means exactly
        short idk = inPacket.decodeShort(); //No clue what this is, stayed  00 00  throughout different tests

        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        int scrollID = scroll.getItemId();

        invType = ePos < 0 ? EQUIPPED : invType;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);


        if (scrollEquip(chr, equip, ePos, scrollID, true)) {
            chr.consumeItem(scroll);
        }
    }

    public static boolean scrollEquip(Char chr, Equip equip, short ePos, int scrollID, boolean updateToChar) {
        Client c = chr.getClient();
        boolean success = true;
        boolean boom = false;
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
        if (vals.size() <= 0) {
            chr.chatMessage("Could not find scroll data.");
            chr.dispose();
            return false;
        }
        if (equip == null || equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            chr.dispose();
            return false;
        }
        if (ItemConstants.isSymbol(equip.getItemId()) || !ItemData.getItemInfoByID(scrollID).canBeUsedOnItem(equip)) {
            chr.chatMessage("Invalid item to use this item on.");
            chr.dispose();
            return false;
        }
        boolean recover = vals.getOrDefault(ScrollStat.recover, 0) != 0;
        boolean reset = vals.getOrDefault(ScrollStat.reset, 0) + vals.getOrDefault(ScrollStat.perfectReset, 0) != 0;
        if (equip.getBaseStat(tuc) <= 0 && (!recover && !reset)) {
            chr.chatMessage("This item cannot be scrolled.");
            chr.dispose();
            return false;
        }
        boolean useTuc = !recover && !reset;
        int successChance = 100;
        int curse = vals.getOrDefault(ScrollStat.cursed, 0);

        success = Util.succeedProp(successChance);
        if (success) {
            boolean chaos = vals.containsKey(ScrollStat.randStat);
if (chaos) {
    boolean noNegative = vals.containsKey(ScrollStat.noNegative);
    int max = vals.containsKey(ScrollStat.incRandVol) ? ItemConstants.INC_RAND_CHAOS_MAX : ItemConstants.RAND_CHAOS_MAX;
    // Add this line - define your minimum value
    int minRoll = 10; // Change this to your desired minimum value
    
for (EquipBaseStat ebs : ScrollStat.getRandStats()) {
    int cur = (int) equip.getBaseStat(ebs);
    if (cur == 0 && ebs != EquipBaseStat.iPAD) {
        continue;
    }
        int randStat = Util.getRandom(max);
        randStat = !noNegative && Util.succeedProp(50) ? -randStat : randStat;
        
        // Apply minimum bound
        if (randStat < minRoll) {
            randStat = minRoll;
        }
        
        equip.addStat(ebs, randStat);
    }
}
            if (recover) {
                Equip fullTucEquip = ItemData.getEquipDeepCopy(equip.getItemId(), false);
                int maxTuc = fullTucEquip.getTuc() + equip.getIuc();
                if (equip.getTuc() + equip.getCuc() >= maxTuc) {
                    chr.chatMessage("This item has no open slots to recover.");
                } else {
                    equip.addStat(tuc, 1);
                }
            } else if (reset) {
                equip.resetStats();

                if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
                    int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
                    Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
                    otherEquip.resetStats();
                }

            } else {
                for (Map.Entry<ScrollStat, Integer> entry : vals.entrySet()) {
                    ScrollStat ss = entry.getKey();
                    int val = entry.getValue();
                    if (ss.getEquipStat() != null) {
                        equip.addStat(ss.getEquipStat(), val);
                    }
                }
            }
            if (useTuc) {
                equip.addStat(tuc, -1);
                equip.addStat(cuc, 1);
            }
        } else {
            if (curse > 0) {
                boom = Util.succeedProp(curse);
                if (boom && !equip.hasAttribute(EquipAttribute.ProtectionScroll)) {
                    chr.consumeItem(equip);
                } else {
                    boom = false;
                }
            }
            if (useTuc && !equip.hasAttribute(EquipAttribute.UpgradeCountProtection)) {
                equip.addStat(tuc, -1);
            }
        }
        equip.removeAttribute(EquipAttribute.ProtectionScroll);
        equip.removeAttribute(EquipAttribute.LuckyDay);
        if (useTuc) {
            equip.removeAttribute(EquipAttribute.UpgradeCountProtection);
        }
        equip.recalcEnchantmentStats();
        if (updateToChar) {
            c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), boom));
            if (!boom) {
                equip.updateToChar(chr);
                updateZeroEgoEquipComplete(chr, equip, ePos, success);
            }
        }
        return true;
    }

    private static void updateZeroEgoEquipComplete(Char chr, Equip equip, int ePos, boolean success) {
        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
            int otherEquipPos = Math.abs(ePos) == 10 ? 11 : 10;
            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
            otherEquip.copyAttributesFrom(equip);
            otherEquip.copyItemOptionsFrom(equip);
            otherEquip.copySecondaryStatsFrom(equip);
            otherEquip.copyFlameStatsFrom(equip);
            otherEquip.updateToChar(chr);

            chr.write(ZeroPool.egoEquipComplete(success, true));
        }
    }

    @Handler(op = InHeader.USER_ITEM_OPTION_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserItemOptionUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        byte bEnchantSkill = inPacket.decodeByte(); // bool or byte?
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            chr.dispose();
            return;
        } else if (!ItemConstants.canEquipHavePotential(equip)) {
            chr.getOffenseManager().addOffense(String.format("Character %d tried to add potential an ineligible item (id %d)", chr.getId(), equip.getItemId()));
            chr.dispose();
            return;
        }
        int scrollID = scroll.getItemId();
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
        int chance = vals.getOrDefault(ScrollStat.success, 100);
        int curse = vals.getOrDefault(ScrollStat.cursed, 0);
        boolean success = Util.succeedProp(chance);
        if (success) {
            short val;
            int thirdLineChance = ItemConstants.THIRD_LINE_CHANCE;
            switch (scrollID / 10) {
                case 204940: // Rare Pot
                case 204941:
                case 204942:
                case 204943:
                case 204944:
                case 204945:
                case 204946:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204970: // Epic pot
                case 204971:
                    val = ItemGrade.HiddenEpic.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204974: // Unique Pot
                case 204975:
                case 204976:
                case 204979:
                    val = ItemGrade.HiddenUnique.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;
                case 204978: // Legendary Pot
                    val = ItemGrade.HiddenLegendary.getVal();
                    equip.setHiddenOptionBase(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(false);
                    }
                    break;

                default:
                    chr.chatMessage(Mob, "Unhandled scroll " + scrollID);
                    chr.dispose();
                    log.error("Unhandled scroll " + scrollID);
                    return;
            }

            updateZeroEgoEquipComplete(chr, equip, ePos, success);
        }

        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), false));
        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_ADDITIONAL_SLOT_EXTEND_ITEM_USE_REQUEST)
    public static void handleUserAdditionalSlotExtendItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Item equipItem = chr.getInventoryByType(invType).getItemBySlot(ePos);

        if (item == null || equipItem == null) {
            chr.chatMessage("Could not find either the use item or the equip.");
            return;
        }

        int itemID = item.getItemId();
        Equip equip = (Equip) equipItem;

        if (((Equip) equipItem).getOptionBonus(2) != 0) {
            chr.chatMessage("This item already has 3 bonus potentials.");
            chr.dispose();
            return;
        }

        int successChance = ItemData.getItemInfoByID(itemID).getScrollStats().getOrDefault(ScrollStat.success, 100);
        boolean success = Util.succeedProp(successChance);
        if (success) {
            switch (itemID / 100) {
                case 20483: // Equipment bonus stamps
                    equip.setOption(2, equip.getRandomOption(true, 2), true);
                    break;
                default:
                    log.error("Unhandled slot extend item " + itemID);
                    chr.chatMessage("Unhandled slot extend item " + itemID);
                    return;
            }
            equip.updateToChar(chr);

            updateZeroEgoEquipComplete(chr, equip, ePos, true);
        }
        chr.consumeItem(item);
        chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, itemID, equip.getItemId(), false));
    }

    @Handler(op = InHeader.USER_ITEM_SLOT_EXTEND_ITEM_USE_REQUEST)
    public static void handleUserItemSlotExtendItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Item equipItem = chr.getInventoryByType(invType).getItemBySlot(ePos);

        if (item == null || equipItem == null) {
            chr.chatMessage("Could not find either the use item or the equip.");
            chr.dispose();
            return;
        }

        int itemID = item.getItemId();
        Equip equip = (Equip) equipItem;

        if (((Equip) equipItem).getOptionBase(2) != 0) {
            chr.chatMessage("This item already has 3 potentials.");
            chr.dispose();
            return;
        }

        int successChance = ItemData.getItemInfoByID(itemID).getScrollStats().getOrDefault(ScrollStat.success, 100);
        boolean success = Util.succeedProp(successChance);
        if (success) {
            switch (itemID / 100) {
                case 20495: // Equipment stamps
                    equip.setOption(2, equip.getRandomOption(false, 2), false);
                    break;
                default:
                    log.error("Unhandled slot extend item " + itemID);
                    chr.chatMessage("Unhandled slot extend item " + itemID);
                    return;
            }
            equip.updateToChar(chr);

            updateZeroEgoEquipComplete(chr, equip, ePos, true);
        }
        chr.consumeItem(item);
        chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, itemID, equip.getItemId(), false));
    }

    @Handler(op = InHeader.USER_ADDITIONAL_OPT_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserAdditionalOptUpgradeItemUseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (c.getWorld().isReboot()) {
            log.error(String.format("Character %d attempted to use bonus potential in reboot world.", chr.getId()));
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        byte bEnchantSkill = inPacket.decodeByte();
        Item scroll = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos);
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (scroll == null || equip == null) {
            chr.chatMessage(SystemNotice, "Could not find scroll or equip.");
            return;
        }
        int scrollID = scroll.getItemId();
        boolean success;
        Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
        int chance = vals.getOrDefault(ScrollStat.success, 100);
        int curse = vals.getOrDefault(ScrollStat.cursed, 0);
        success = Util.succeedProp(chance);
        if (success) {
            short val;
            int thirdLineChance = ItemConstants.THIRD_LINE_CHANCE;
            switch (scrollID) {
                case 2048305: // Bonus Pot
                case 2048308:
                case 2048309:
                case 2048310:
                case 2048311:
                case 2048313:
                case 2048314:
                case 2048316:
                case 2048329:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBonus(val, thirdLineChance);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(true);
                    }
                    break;
                case 2048306: // Special Bonus Pot
                case 2048307:
                case 2048315:
                case 2048331:
                    val = ItemGrade.HiddenRare.getVal();
                    equip.setHiddenOptionBonus(val, 100);

                    if (ItemConstants.isLongOrBigSword(equip.getItemId())) { // Zero cannot use 'release option'
                        equip.releaseOptions(true);
                    }
                    break;
                default:
                    chr.chatMessage(Mob, "Unhandled scroll " + scrollID);
                    break;
            }

            updateZeroEgoEquipComplete(chr, equip, ePos, true);
        }
        c.write(FieldPacket.showItemUpgradeEffect(chr.getId(), success, false, scrollID, equip.getItemId(), false));
        equip.updateToChar(chr);
        chr.consumeItem(scroll);
    }

    @Handler(op = InHeader.USER_ITEM_RELEASE_REQUEST)
    public static void handleUserItemReleaseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); //tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(uPos); // old system with magnifying glasses
        var invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        if (equip == null) {
            chr.chatMessage(SystemNotice, "Could not find equip.");
            return;
        }
        boolean base = equip.getOptionBase(0) < 0;
        boolean bonus = equip.getOptionBonus(0) < 0;
        if (base && bonus) {
            equip.releaseOptions(true);
            equip.releaseOptions(false);
        } else {
            equip.releaseOptions(bonus);
        }

        updateZeroEgoEquipComplete(chr, equip, ePos, true);

        c.write(FieldPacket.showItemReleaseEffect(chr.getId(), ePos, bonus));
        if (invType.equals(EQUIPPED)) {
            chr.getInventoryByType(invType).recalcBaseStats(chr);
        }
        equip.updateToChar(chr);
    }

    public static int flameEquipUntilTargetScore(Char chr, Equip equip, short equippos, int flameID, int targetScore, int nxPerRoll, int delayPerRollMS, BaseStat mainStat) {
        if (delayPerRollMS > 200) {
            delayPerRollMS = 200;
        }
        double max = 0;
        if (!ItemConstants.canEquipHaveFlame(equip)) {
            return 0;
        }
        Item flame = chr.getConsumeInventory().getItemByItemID(flameID);
        if (flame == null) {
            if (nxPerRoll > 0) {
                return 0;
            }
            flame = ItemData.getItemDeepCopy(flameID);
        }
        int budget;
        if (nxPerRoll > 0) {
            budget = Math.min(chr.getConsumeInventory().getQuantity(flameID), chr.getNX() / nxPerRoll);
        } else {
            budget = 100000;
        }
        int nxBudget = chr.getNX();
        int flameUsed = 0;
        if (mainStat == null) mainStat = GameConstants.getMainStatForRequiredJob(equip.getInfo().getrJob());
        int cost = 0;
        while (true) {
            if (cost + nxPerRoll > nxBudget) {
                flameUsed = -flameUsed;
                break;
            }
            cost += nxPerRoll;

            double flameScore = equip.getFlameScoreFor(mainStat);
            if (delayPerRollMS > 0) {
                chr.chatMessage(SystemNotice, "Flame Score: " + flameScore);
            }
            if (flameScore > max) {
                max = flameScore;
                chr.chatMessage(SystemNotice, "Score: " + max + " in " + flameUsed + " attempts.");
            }
            if (!(flameScore < targetScore)) break;
            if (flameUsed >= budget) {
                flameUsed = -flameUsed;
                break;
            }
            boolean flamed = flameEquip(chr, equip, equippos, flame, delayPerRollMS > 0);
            if (!flamed) {
                flameUsed = -flameUsed;
                break;
            }
            flameUsed++;
            if (delayPerRollMS > 0) {
                try {
                    Thread.sleep(delayPerRollMS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        chr.consumeItem(flameID, Math.abs(flameUsed));
        if (delayPerRollMS <= 0) {
            equip.updateToChar(chr);
            chr.deductNX(cost);
            chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), true, false, flameID, equip.getItemId(), false));
        }
        return flameUsed;
    }

    // positive = success, 0/negative = fail
    public static int scrollEquipUntilTargetScore(Char chr, Equip equip, short ePos, int scrollID, double targetAvgATT, int nxPerRoll, boolean consumeScroll) {
        if (!ItemHandler.isScrollable(equip)) {
            return 0;
        }
        int hammerCost = 1000;
        int maxAttempts = 100000;
        int innocenceCost = 10000;

        Map<ScrollStat, Integer> scrollStats = ItemData.getItemInfoByID(scrollID).getScrollStats();
        int scrollsUsed = 0;
        BaseStat mainStat = GameConstants.getMainStatForRequiredJob(equip.getInfo().getrJob());
        short maxHammerSlots = ItemConstants.canEquipGoldHammer(equip) ? ItemConstants.MAX_HAMMER_SLOTS : 0;
        Item hammer = ItemData.getItemDeepCopy(ItemConstants.GOLDEN_HAMMER_100);
        double targetAvgScore = targetAvgATT * 4; // Internal score, att = 4, stat = 1
        int maxSlot = equip.getTuc() + equip.getCuc() + maxHammerSlots - equip.getIuc();
        double targetScore = targetAvgScore * maxSlot;
        double maxAvg = 51; // 12.75 Avg
//        double diff = maxAvg - targetAvgScore;
//        double thresholdPerCuc = 0.1;
        double approxChance = ICOGProbabilityCalculator.getProbabilityEqualOrAbove(maxSlot, targetAvgATT * maxSlot);
        int scrollCount = chr.getConsumeInventory().getQuantity(scrollID);
        if (targetScore > maxAvg * maxSlot) {
            return 0;
        }
        boolean belowMargin = false;
        int nxBudget = chr.getNX();
        int cost = 0;
        while (true) {
            if (scrollsUsed < 0) {
                break;
            }
            if ((equip.getTuc() == 0 && equip.getIuc() == maxHammerSlots) || belowMargin) {
                if (equip.getTuc() == 0) {
                    double score = equip.getScrollScoreFor(mainStat);
                    double finalAvgScore = score / equip.getCuc();
                    chr.chatMessage(ItemMessage, String.format("Final Scroll Score: +%s %.2f (%.2f avg att equiv)", equip.getCuc(), score, finalAvgScore / 4));
                    if (finalAvgScore >= targetAvgScore) {
                        break;
                    }
                }
                if (cost + innocenceCost > nxBudget) {
                    scrollsUsed = -scrollsUsed;
                    break;
                }
                equip.applyInnocenceScroll();
                cost += innocenceCost;

                belowMargin = false;
            }

            boolean scrolled = false;

            while (equip.getTuc() > 0 || equip.getIuc() < maxHammerSlots) {
                if (scrollsUsed >= scrollCount && consumeScroll) {
                    scrollsUsed = -scrollsUsed;
                    break;
                }
                if (!equip.hasAttribute(EquipAttribute.UpgradeCountProtection)) {
                    if (cost + 30000 > nxBudget) {
                        scrollsUsed = -scrollsUsed;
                        break;
                    }
                    equip.addAttribute(EquipAttribute.UpgradeCountProtection);
                    cost += 30000;
                }
                if (!equip.hasAttribute(EquipAttribute.LuckyDay)) {
                    if (cost + 15000 > nxBudget) {
                        scrollsUsed = -scrollsUsed;
                        break;
                    }
                    equip.addAttribute(EquipAttribute.LuckyDay);
                    cost += 15000;
                }
                if (!equip.hasAttribute(EquipAttribute.ProtectionScroll)) {
                    if (cost + 20000 > nxBudget) {
                        scrollsUsed = -scrollsUsed;
                        break;
                    }
                    equip.addAttribute(EquipAttribute.ProtectionScroll);
                    cost += 20000;
                }
                short tuc = equip.getTuc();
                short cuc = equip.getCuc();
                int hammerAvailable = maxHammerSlots - equip.getIuc();
                int remainingSlots = tuc + hammerAvailable;

                double score = equip.getScrollScoreFor(mainStat);
                double avg = score / cuc;
                double potentialMaxScore = score + maxAvg * remainingSlots;

                if (equip.getTuc() == 0 && hammerAvailable > 0) {
                    if (cost + hammerCost > nxBudget) {
                        scrollsUsed = -scrollsUsed;
                        break;
                    }
                    if (!hammerEquip(chr, equip, hammer, ItemConstants.GOLDEN_HAMMER_100, ePos)) {
                        scrollsUsed = -scrollsUsed;
                        break;
                    }
                    cost += hammerCost;
                }

                if (potentialMaxScore < targetScore) {
//                    chr.chatMessage(Mob, String.format("Impossible Aborting: +%s avg %.1f (max %.1f / target %.1f)", cuc, avg / 4, potentialMaxScore / maxSlot / 4, targetAvgATT));//                    .chatMessage(Mob, String.format("potentialMaxScore %.1f < requiredScore %.1f)", potentialMaxScore / maxSlot / 4, requiredScore / maxSlot / 4));
                    belowMargin = true;
                    break;
                }
                double currentChance = getExpectedChanceIfContinue(equip, targetScore, remainingSlots, mainStat);
                if (remainingSlots / currentChance > maxSlot / approxChance) {
//                    chr.chatMessage(Mob, String.format("Normal Dist. Aborting: +%s avg %.1f/%.1f(continue %.3f%% / reset %.3f%%)", cuc, avg / 4, targetAvgATT, currentChance * 100, approxChance * 100));
                    belowMargin = true;
                    break;
                }

//                double threshold = maxAvg - (1.1 * diff) - thresholdPerCuc * Math.pow(cuc, 2); // Hybrid Greedy
//                if (avg < threshold) {
//                    chr.chatMessage(Mob, String.format("Greedy Aborting: +%s avg %.1f / threshold %.1f (max %.1f / target %.1f)", cuc, avg / 4, threshold / 4, potentialMaxScore / maxSlot / 4, targetAvgATT));
//                    belowMargin = true;
//                    break;
//                }


                if (cost + nxPerRoll > nxBudget) {
                    scrollsUsed = -scrollsUsed;
                    break;
                }
                scrolled = scrollEquip(chr, equip, ePos, scrollID, false);
                if (!scrolled) {
                    scrollsUsed = -scrollsUsed;
                    break;
                }
                cost += nxPerRoll;

                scrollsUsed++;
                if (scrollsUsed >= maxAttempts) {
                    scrollsUsed = -scrollsUsed;
                    break;
                }
            }
            if (!scrolled && !belowMargin) {
                scrollsUsed = -scrollsUsed;
                break;
            }
        }

        equip.updateToChar(chr);
        if (consumeScroll) {
            chr.consumeItem(scrollID, Math.abs(scrollsUsed));
        }
        chr.deductNX(cost);
//        chr.write(FieldPacket.showItemUpgradeEffect(chr.getId(), scrollsUsed > 0, false, scrollID, equip.getItemId(), false));
        return scrollsUsed;
    }

    public static double getExpectedChanceIfContinue(Equip equip, double targetScore, int remainingSlots, BaseStat baseStat) {
        double remainingScore = (targetScore - equip.getScrollScoreFor(baseStat)) / 4D;
        return ICOGProbabilityCalculator.getProbabilityEqualOrAbove(remainingSlots, remainingScore);
    }

    public static void spellTraceUntilMaxLevel(Char chr, short bagIndex, int scrollID) {
        Equip equip = (Equip) chr.getEquipInventory().getItemBySlot(bagIndex);
        short maxHammerSlots = ItemConstants.canEquipGoldHammer(equip) ? ItemConstants.MAX_HAMMER_SLOTS : 0;

        while (true) {
            int hammerAvailable = maxHammerSlots - equip.getIuc();

            if (equip.getTuc() == 0 && hammerAvailable > 0) {
                if (chr.getNX() < 1000) {
                    break;
                }
                chr.deductNX(1000);
                if (!hammerEquip(chr, equip, ItemData.getItemDeepCopy(ItemConstants.GOLDEN_HAMMER_100), ItemConstants.GOLDEN_HAMMER_100, bagIndex)) {
                    break;
                }
            }

            if (!ItemHandlerModule.spellTraceEquip(chr, bagIndex, scrollID, false)) {
                break;
            }
        }
    }
}

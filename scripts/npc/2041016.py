import time
from net.swordie.ms.constants import GameConstants
from net.swordie.ms.enums import InvType

scrollList = [[2049301, "Normal Chance", 10000, 0], [2049301, "+10% Chance", 250000, 100], [2049301, "+15% Chance", 50000, 150],
              [2049301, "+20% Chance", 200000, 200], [2049301, "+25% Chance", 1000000, 250], [2049301, "+30% Chance", 2000000, 300]]
manualBonus = 50 # +5% if you click manually
listitem = []
itemID = []
newlist = []
chr = sm.getChr()

def restore_item(bagIndex):
    itemId = str(sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP))
    requiredNX = GameConstants.getVestigeRecoverCost(chr.getEquipInventory().getItemBySlot(bagIndex))
    if sm.sendAskAccept(
            "Restore trace #v" + itemId + "# #e #t" + itemId + "##n. This will cost %s NX." % requiredNX):
        if sm.getChr().getNX() < requiredNX:
            sm.sendSayOkay("You do not have enough NX.")
            return False
        sm.restoreEquipTrace(bagIndex)
        sm.deductNX(requiredNX)
        return True
    else:
        return None

while True:
    selection = sm.sendNext(
        "#e<Equip Auto Starforce Enhancement NPC>#n \r\n " +
        "\r\n Hey #h #. I am in charge of Equip Enchancing. Please choose of the options below: \r\n \r\n " +
        "\r\n#b#L1#Use Equip Enchancement.#l " +
        "\r\n#L2#Restore Equipment Trace.#l")
    if selection == 1:
        newlist = []
        itemID = []
        listitem = eval(sm.getEquipsStarforceable())
        listitem.sort()
        for x in range(len(listitem)):
            itemID.append(sm.getItemIDByBagIndex(listitem[x], InvType.EQUIP))
            newlist.append('\n#L' + str(listitem[x]) + '##v' + str(itemID[x]) + '#' + "#t" + str(itemID[x]) + "#\n\r")
        if not newlist:
            sm.sendSayOkay("No equips to enhance")
            continue
        bagIndex = sm.sendNext(''.join(newlist))
        itemId = str(sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP))
        while True:
            stats = eval(sm.getEquipStatsByBagIndex(bagIndex))
            chuc = stats[0]
            downgradable = stats[1]
            mesocost = stats[2]
            successrate = stats[3]
            failurerate = stats[4]
            maxstars = stats[5]
            isTrace = stats[6]
            if isTrace:
                sm.sendSayOkay("Item Destroyed, RIP")
                result = restore_item(bagIndex)
                if result is None:
                    break
                if result == False:
                    continue
            if chuc == 30:
                sm.sendSayOkay("Wow, 30 stars, congratulations!")
                break
            if chuc >= 25:
                failText = 'Double Downgrade'
            else:
                failText = 'Destroy'
            previewText = ("#v" + itemId + "# #e #t" + itemId + "##n - Stars: %s (MAX %s)" % (chuc, maxstars)
                           + "\r\n#bMeso Cost: %s Mesos" % format(mesocost, ',')
                           + "\r\n#gSuccess Rate: %s%%" % (successrate / 10)
                           + " - #r%s on Failure: %s%%" % (failText, failurerate / 10.0)
                           + ("#r" if downgradable else "#g")
                           + "\r\nDowngrade on Failure: %s" % downgradable)
            selectionTier = sm.sendNext(
                previewText + "#k"
                + "\r\n#L0#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[0][3]/10 + manualBonus/10, scrollList[0][2])
                + "\r\n#L1#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[1][3]/10 + manualBonus/10, scrollList[1][2])
                # + "\r\n#L2#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[2][3]/10 + manualBonus/10, scrollList[2][2])
                + "\r\n#L3#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[3][3]/10 + manualBonus/10, scrollList[3][2])
                # + "\r\n#L4#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[4][3]/10 + manualBonus/10, scrollList[4][2])
                + "\r\n#L5#+{}% Chance (#e{:,} NX#n)#l".format(scrollList[5][3]/10 + manualBonus/10, scrollList[5][2])
                + "\r\n\r\n#L10#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[0][3]/10, scrollList[0][2])
                + "\r\n#L11#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[1][3]/10, scrollList[1][2])
                # + "\r\n#L12#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[2][3]/10, scrollList[2][2])
                + "\r\n#L13#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[3][3]/10, scrollList[3][2])
                # + "\r\n#L14#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[4][3]/10, scrollList[4][2])
                + "\r\n#L15#(AUTO) +{}% Chance Until ... (#e{:,} NX#n)#l".format(scrollList[5][3]/10, scrollList[5][2])
                + "\r\n\r\n#L999#Back#l"
            )
            if selectionTier < 0 or selectionTier >= 999:
                break
            target = -1
            if selectionTier >= 10:
                selectionTier -= 10
                nxPerRoll = scrollList[selectionTier][2]
                extraChance = scrollList[selectionTier][3]
                target = sm.sendAskNumber(
                    "Starforce (%s) %s NX Until:" % (scrollList[selectionTier][1], str(nxPerRoll)), 15, 1, 30)
                sm.enhanceItemUntil(bagIndex, extraChance, target, nxPerRoll)
            elif target == -1:
                nxPerRoll = scrollList[selectionTier][2]
                extraChance = scrollList[selectionTier][3] + manualBonus
                stats = eval(sm.getEquipStatsByBagIndex(bagIndex))
                chuc = stats[0]
                mesocost = stats[2]
                isTrace = stats[6]
                if isTrace or (0 < target <= chuc):
                    break
                if sm.getMesos() < mesocost:
                    sm.sendSayOkay("You do not have enough Mesos.")
                    break
                if sm.getChr().getNX() < nxPerRoll:
                    sm.sendSayOkay("You do not have enough NX.")
                    break
                else:
                    sm.enhanceItem(bagIndex, extraChance)
                    sm.deductNX(nxPerRoll)

    elif selection == 2:
        newlist = []
        listitem = eval(sm.getEquipsTraces())
        for x in range(len(listitem)):
            item = sm.getItemIDByBagIndex(listitem[x], InvType.EQUIP)
            itemID = sm.getItemIDByBagIndex(listitem[x], InvType.EQUIP)
            newlist.append('#L' + str(listitem[x]) + '##v' + str(itemID) + '#' + "#t" + str(itemID) + "#\r\n")
        if not newlist:
            sm.sendSayOkay("No equips to restore")
            continue
        bagIndex = sm.sendNext(''.join(newlist))
        restore_item(bagIndex)

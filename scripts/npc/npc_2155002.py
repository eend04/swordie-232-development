# npc_2155002 Auto Flame NPC
import time
from net.swordie.ms.enums import InvType
from net.swordie.ms.enums import BaseStat
from net.swordie.ms.constants import ItemConstants

flameID = ItemConstants.POWERFUL_REBIRTH_FLAME
flameNXCost = 20000
requiredNXPerRoll = 5000
bulkSize = 500
selection = sm.sendNext(
    "#e<Equip Auto Flame NPC>#n \r\n Hey #h #. I am in charge of Equip Flaming. Please choose one of the options below:" +
    "\r\n#L0#Buy #v%d# #t%d# #l" % (flameID, flameID) +
    "\r\n#L1#Auto Flame #l")

if selection == 0:  # Buy Flame
    NX = sm.getChr().getNX()
    amount = sm.sendAskNumber(
        "You currently have #r%s NX#b" % format(NX, ',') +
        "\r\nHow many #b#v%d# #t%d# do you want to buy? (%s EACH)" % (flameID, flameID, format(flameNXCost, ',')), 1, 1, NX/flameNXCost)
    if sm.canHold(flameID, amount):
        sm.deductNX(flameNXCost * amount)
        sm.giveItem(flameID, amount)
    else:
        sm.sendNext("Please make some space in your inventory.")

if selection == 1:  # Auto Flame
    newlist = []
    listitem = sorted(eval(sm.getEquipsFlameable()))
    for i in range(len(listitem)):
        itemID = sm.getItemIDByBagIndex(listitem[i], InvType.EQUIP)
        newlist.append(
            '#L%s#(%s NX/ea) ' % (str(listitem[i]), format(requiredNXPerRoll, ','))
            + '#v%s##t%s#\r\n' % (str(itemID),str(itemID)))
    if not newlist:
        sm.sendSayOkay("No equips to flame")
        sm.dispose(True)
    NX = sm.getChr().getNX()
    selection2 = 0
    while selection2 <= 0:
        playerFlame = sm.getQuantityOfItem(flameID)
        selection2 = sm.sendNext(
            "You currently have #r%s#i%s# AND %s NX#b" % (str(playerFlame),str(flameID),format(NX, ','))
            + "\r\n\n#kPlease choose an item to scroll:"
            + "\r\n#L0##b(%s NX) Buy %s#i%s#\r\n" % (format(flameNXCost * bulkSize, ','), format(bulkSize, ','), str(flameID))
            + "".join(newlist))
        if selection2 == 0:
            if not sm.canHold(flameID, bulkSize):
                sm.sendSayOkay("Please make some space in your inventory.")
                continue
            if NX < flameNXCost * bulkSize:
                sm.sendSayOkay("You do not have enough NX.")
                continue
            sm.giveItem(flameID, bulkSize)
            sm.deductNX(flameNXCost * bulkSize)
        else:
            bagIndex = selection2
            itemId = str(sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP))
            targetScore = sm.sendAskNumber(
                "Flame #v%s# #e #t%s##n?" % (itemId, itemId)
                + "\r\nThis will cost %s NX (per roll fee)" % format(requiredNXPerRoll, ',')
                + "\r\n(AND #v%d# #t%d# so make sure you have some)" % (flameID, flameID)
                + "\r\nYou currently have #r%s#i%d##b" % (format(playerFlame, ','),flameID)
                + "\r\nPlease specify your target flame score:"
                + "\r\n(main 1, sec 0.125, att 4, all 10)"
                + "\r\n(boss 8, dmg 16)", 100, 50, 1200)
            statChoice = sm.sendNext("Select Target Stat: "
                                     +"\r\n#L0##b Auto (for equipment job)"
                                     +"\r\n#L1##b STR (+dex)"
                                     +"\r\n#L2##b DEX (+str)"
                                     +"\r\n#L3##b INT (+luk)"
                                     +"\r\n#L4##b LUK (+dex)"
                                     +"\r\n#L5##b Xenon (STR DEX LUK)")
            if statChoice == 0:
                targetStat = None
            elif statChoice == 1:
                targetStat = BaseStat.str
            elif statChoice == 2:
                targetStat = BaseStat.dex
            elif statChoice == 3:
                targetStat = BaseStat.inte
            elif statChoice == 4:
                targetStat = BaseStat.luk
            elif statChoice == 5:
                targetStat = BaseStat.allStat
            if sm.getChr().getNX() < requiredNXPerRoll:
                sm.sendSayOkay("You do not have enough NX.")
                dispose(True)
            flameUsed = sm.autoFlame(bagIndex, flameID, targetScore, requiredNXPerRoll, targetStat)

            if flameUsed > 0:
                sm.sendSayOkay("YAY, you got it in %d flames" % flameUsed)
            elif flameUsed == 0:
                sm.sendSayOkay("Your equip already meet the target score or you don't have the neccessary flames and NX")
                selection2 = 0
            else:
                sm.sendSayOkay("You have wasted %d flames and %d NX for nothing :(" % (-flameUsed, -flameUsed*requiredNXPerRoll))
                selection2 = 0

                # maybe handle and display result??

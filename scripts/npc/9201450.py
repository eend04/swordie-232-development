# 9201450 Auto Scroll NPC
from net.swordie.ms.enums import InvType
from net.swordie.ms.constants import ItemConstants
from net.swordie.ms.util import ICOGProbabilityCalculator
import math

def promptChooseEquip(listitem):
    newlist = []
    for i in range(len(listitem)):
        itemID = sm.getItemIDByBagIndex(listitem[i], InvType.EQUIP)
        newlist.append('#L%s# #v%s# #z%s#\r\n' % (str(listitem[i]), str(itemID), str(itemID)))
    if not newlist:
        sm.sendSayOkay("Nothing to scroll")
        sm.dispose()
    prompt = ("You currently have #r" + format(NX, ',') + " NX#b"
              + "\r\n\n#kPlease choose an item to scroll:"
                "\r\n" + "".join(newlist))
    return prompt

spellTraceID = 4001832
traceNXCost = 100000  # 10k trace
requiredNX = 10000

icogID = ItemConstants.CHAOS_SCROLL_OF_GOODNESS_50
icog60NXCost = 1000000
requiredNXPerRoll = 5000

scrolls = [ # [id, cost]
    [ItemConstants.INNOCENCE_SCROLL_100, 10000],
    [ItemConstants.PRIME_SCROLL_ACCESSORY, 1000000],
    [ItemConstants.PRIME_SCROLL_ARMOR, 1000000],
    [ItemConstants.PRIME_SCROLL_ONE_HANDED, 10000000],
    [ItemConstants.PRIME_SCROLL_TWO_HANDED, 10000000],
    # [2613050, 10000000], # X ATT (no actual att increase, nexon probably hardcode something somewhere)
    # [2612061, 10000000], # X ATT
    # [2613051, 10000000], # X MATT
    # [2612062, 10000000], # X MATT

    [ItemConstants.GOLDEN_HAMMER_100, 1000]
]

primeID = ItemConstants.PRIME_SCROLL_ONE_HANDED
weaponPrimeCost = 10000000

armorPrimeID = ItemConstants.PRIME_SCROLL_ARMOR
armorPrimeCost = 1000000

accPrimeID = ItemConstants.PRIME_SCROLL_ACCESSORY
accPrimeCost = 1000000

NX = sm.getChr().getNX()

selection = sm.sendNext(
    "#e<Equip Auto Scroll NPC>#n \r\n Hey #h #. I am in charge of Equip Scrolling. Please choose one of the options below:" +
    "\r\n#L0# #v%d# AUTO Spell Trace #l" % (spellTraceID) +
    "\r\n#L1# #v%d# AUTO ICOG#l" % (icogID) +
    "\r\n#L2# #v%d# AUTO (For Weapon) #l" % (primeID) +
    "\r\n#L3# #v%d# AUTO (For Armor) #l" % (armorPrimeID) +
    "\r\n#L4# #v%d# AUTO (For Accessory) #l" % (accPrimeID) +
    "\r\n#L5# BUY ETC #v%d# #v%d# #v%d# #v%d# #l" % (ItemConstants.INNOCENCE_SCROLL_100, ItemConstants.PRIME_SCROLL_ONE_HANDED, ItemConstants.PRIME_SCROLL_ARMOR, ItemConstants.GOLDEN_HAMMER_100))

if selection < 0:
    sm.dispose()

if selection == 0:  # Auto Spell Trace
    newlist = []
    listitem = sorted(eval(sm.getEquipsScrollable()))
    for i in range(len(listitem)):
        itemID = sm.getItemIDByBagIndex(listitem[i], InvType.EQUIP)
        newlist.append(
            '#L' + str(listitem[i]) + '#(%s NX)#v' % format(requiredNX, ',') + str(itemID) + '#' + "#z" + str(
                itemID) + "#\r\n")
    if not newlist:
        sm.sendSayOkay("No equips to spelltrace")
        sm.dispose()
    traceSelection = 0
    while traceSelection <= 0:
        playerSpellTrace = sm.getQuantityOfItem(spellTraceID)
        traceSelection = sm.sendNext(
            "You currently have #r" + str(playerSpellTrace) + "#i4001832# AND " + format(NX, ',') + " NX#b"
            + "\r\n\n#kPlease choose an item to scroll:"
            + "\r\n#L0##b(%s NX) Buy 10,000#i4001832#\r\n" % format(traceNXCost, ',')
            + "".join(newlist))
        if traceSelection == 0:
            if NX < traceNXCost:
                sm.sendSayOkay("You do not have enough NX.")
                dispose()
            sm.giveItem(spellTraceID, 10000)
            sm.deductNX(traceNXCost)
        else:
            bagIndex = traceSelection
            itemId = sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP)
            stats = eval(sm.getEquipStatsByBagIndex(bagIndex))
            bodyPart = stats[8]
            if not ItemConstants.isAccessory(itemId): # bodyPart in ["Weapon", "Gloves"] + isHeart included
                scrollID = sm.sendNext(
                    "Select a scroll type (" + bodyPart + ")"
                    + "\r\n#L0#100%"
                    + "\r\n#L1#70%"
                    + "\r\n#L2#30%"
                    + "\r\n#L3#15%"
                )
            else:
                scrollID = sm.sendNext(
                    "Select a scroll type (" + bodyPart + ")"
                    + "\r\n#L0#100%"
                    + "\r\n#L1#70%"
                    + "\r\n#L2#30%"
                )
            itemId = str(itemId)
            if sm.sendAskAccept(
                    "SpellTrace #v" + itemId + "# #e #z" + itemId + "##n. to Max Level?"
                    + "\r\nThis will cost %s NX (one time fee)" % requiredNX
                    + "\r\n(AND the usual Spell Trace costs so make sure you have some)"
                    + "\r\n\nYou currently have #r" + str(playerSpellTrace) + "#i4001832##b\r\n"):
                if sm.getChr().getNX() < requiredNX:
                    sm.sendSayOkay("You do not have enough NX.")
                    dispose()
                sm.deductNX(requiredNX)
                sm.autoSpellTrace(bagIndex, scrollID)

if selection == 1:  # ICOG
    buySelection = sm.sendNext(
        "#v%d# #z%d# #l" % (icogID, icogID) +
        "\r\n#L0#Buy Scroll #l" +
        "\r\n#L1#Auto Scroll for target avg...#l")
    if buySelection == 0:
        amount = sm.sendAskNumber(
            "You currently have #r%s NX#b" % format(NX, ',') +
            "\r\nHow many #b#v%d# #z%d# do you want to buy? (%s EACH)" % (icogID, icogID, format(icog60NXCost, ',')), 1, 1, NX/icog60NXCost)
        if sm.canHold(icogID, amount):
            sm.deductNX(icog60NXCost * amount)
            sm.giveItem(icogID, amount)
        else:
            sm.sendNext("Please make some space in your inventory.")
    elif buySelection == 1:
        newlist = []
        listitem = sorted(eval(sm.getEquipsScrollable()))
        for i in range(len(listitem)):
            itemID = sm.getItemIDByBagIndex(listitem[i], InvType.EQUIP)
            newlist.append(
                '#L%s# #v%s# #z%s#\r\n' % (str(listitem[i]), str(itemID), str(itemID)))
        if not newlist:
            sm.sendSayOkay("No equips to scroll")
            sm.dispose(True)
        selection2 = 0
        while selection2 <= 0:
            selection2 = sm.sendNext(
                "You currently have #r%s NX#b" % format(NX, ',')
                + "\r\n#kAuto #v%s# #z%s# " % (str(icogID), str(icogID))
                + "\r\nThis will cost %s NX (extra) + scroll #v%s#" % (requiredNXPerRoll, icogID)
                + "\r\n#kPlease choose an item to scroll:\r\n"
                + "".join(newlist))
            bagIndex = selection2
            itemId = str(sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP))

            targetScore = sm.sendAskNumber(
                    "Scroll #v" + itemId + "# #e #z" + itemId + "##n. to Max Level?"
                    + "\r\nThis will cost %s NX + #v%s# per scroll" % (requiredNXPerRoll, icogID)
                    + "\r\n+%s NX per #v%s# +%s NX per #v%s#" % (10000, 2049610, 1000, 2470003)
                    + "\r\nChoose target average ATT equivalent"
                    + "\r\nFrom 10-125 (1 decimal point) ex. 80 for 8.0 avg"
                    + "\r\n(ATT 1x + STAT 0.25x + Secondary 0.025x)", 80, 10, 125)

            maxslots = eval(sm.getEquipStatsByBagIndex(bagIndex))[9]
            targetFloat = float(targetScore) / 10.0
            approxChance = float(ICOGProbabilityCalculator.getProbabilityEqualOrAbove(maxslots, targetFloat * maxslots))
            sm.sendNext("Your chance of getting avg %.1f or equivalent in %d slots (for a specific class) is approximately" % (targetFloat, maxslots)
                        + "\r\n#rEstimated Chance %.2f%% (higher if equip is for any class)" % (approxChance * 100)
                        + "\r\n#rEstimated Cost %.1f scrolls" % float(1/approxChance * math.sqrt(maxslots)))

            if sm.getChr().getNX() < requiredNXPerRoll:
                sm.sendSayOkay("You do not have enough NX.")
                sm.dispose(True)
            icogUsed = sm.autoIcog(bagIndex, icogID, targetFloat, requiredNXPerRoll)

            if icogUsed > 0:
                sm.sendSayOkay("YAY, you got it in %d scrolls" % icogUsed)
            elif icogUsed == 0:
                sm.sendSayOkay("Your equip already meet the target score or you don't have the neccessary scrolls and NX")
            else:
                sm.sendSayOkay("You have wasted %d scrolls and %s NX for nothing :(" % (-icogUsed, format(-icogUsed*requiredNXPerRoll, ',')))


def autoScroll(bagIndex, scrollID, scrollCost):
    itemId = str(sm.getItemIDByBagIndex(bagIndex, InvType.EQUIP))
    if sm.sendAskAccept(
            "Scroll #v" + itemId + "# #e #z" + itemId + "##n. to Max Level?\r\n(Auto Golden Hammer 1000 NX)"
            + "\r\nThis will cost #r%s NX per #b\r\n#v%d# #z%d#" % (format(scrollCost, ','), scrollID, scrollID)
            + "\r\n#kYou currently have #r" + format(NX, ',') + " NX#b"):
        if sm.getChr().getNX() < scrollCost:
            sm.sendSayOkay("You do not have enough NX.")
            dispose()
        scrollUsed = sm.autoScroll(bagIndex, scrollID, scrollCost, False)

        if scrollUsed > 0:
            sm.sendSayOkay("YAY, you got it in %d scrolls" % scrollUsed)
        elif scrollUsed == 0:
            sm.sendSayOkay("Could not scroll, check if you have enough NX")
        else:
            sm.sendSayOkay("You have used %d scrolls (%s NX), #rbut some error occured, maybe you ran out of NX" % (
            -scrollUsed, format(-scrollUsed * scrollCost, ',')))

if selection == 2: # Prime Weapon
    listitem = sorted(eval(sm.getEquipsWeaponScrollable()))
    bagIndex = sm.sendNext(promptChooseEquip(listitem))
    autoScroll(bagIndex, primeID, weaponPrimeCost)
if selection == 3: # Prime Armor
    listitem = sorted(eval(sm.getEquipsArmorScrollable()))
    bagIndex = sm.sendNext(promptChooseEquip(listitem))
    autoScroll(bagIndex, armorPrimeID, armorPrimeCost)
if selection == 4: # Prime Accessory
    listitem = sorted(eval(sm.getEquipsAccessoryScrollable()))
    bagIndex = sm.sendNext(promptChooseEquip(listitem))
    autoScroll(bagIndex, accPrimeID, accPrimeCost)
if selection == 5:  # Buy Scrolls
    scrollListMsg = []
    for i in range(len(scrolls)):
        scroll = scrolls[i]
        scrollID = scroll[0]
        scrollCostNX = scroll[1]

        scrollListMsg.append('#L%s#(%s NX)#v%s##z%s#\r\n' % (i, format(scrollCostNX, ','), scrollID, scrollID))
    selection_scroll = sm.sendNext("Please select the scroll you would like to buy\r\n" + "".join(scrollListMsg))

    scrollID = scrolls[selection_scroll][0]
    scrollCostNX = scrolls[selection_scroll][1]

    amount = sm.sendAskNumber(
        "You currently have #r%s NX#b" % format(NX, ',') +
        "\r\nHow many #b#v%d# #z%d# do you want to buy? (%s EACH)" % (scrollID, scrollID, format(scrollCostNX, ',')), 1, 1, NX/scrollCostNX)
    if sm.canHold(scrollID, amount):
        sm.deductNX(scrollCostNX * amount)
        sm.giveItem(scrollID, amount)
    else:
        sm.sendNext("Please make some space in your inventory.")
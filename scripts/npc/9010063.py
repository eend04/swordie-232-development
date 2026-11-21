from net.swordie.ms.loaders import DressingRoom

# Dressing Room NPC1

equip_type = 0
equip_page = 0
page_limit = 50
expire_time = 5

# flags
flag_main = 1
flag_browse = 1
flag_search = 0
# select category
# select specific page
# go back to categories

categories = [
    ["Hats", 2, 0],
    ["Tops", 2, 0],
    ["Bottoms", 2, 0],
    ["Overalls", 2, 0],
    ["Shoes", 2, 0],
    ["Gloves", 2, 0],
    ["Capes", 2, 0],
    ["Weapons", 2, 0],
    ["Face Accessories", 2, 0],
    ["Eye Accessories", 2, 0],
    ["Rings", 2, 0],
    ["Androids", 2, 0]
]
all_jobs = {
    0: "Beginner",
    1: "Warrior",
    4: "Bowman",
    2: "Mage",
    8: "Thief",
    16: "Pirate"
}

selected_item = 0


def prompt_main():
    text = "Please select a category\r\n"
    count = 0
    text += "#e#r#L666#Browse Cash Item#l#n\r\n"
    text += "#e#r#L555#Browse Non-Cash Item#l#n\r\n"
    text += "\t#e#r#L999#Search Cash Item#l#n\r\n"
    text += "\t#e#r#L888#Search Non-Cash Item#l#n\r\n"
    return text


def prompt_body_part():
    parts = DressingRoom.getValidPart()
    text = "#ePlease select a body part\r\n"

    for part in parts:
        if part.getVal() == 0:  # invalid / unknown
            continue
        text += "\n#b#L%s#%s (ID: %s)#l\r\n" % (part.getVal(), str(part), part.getVal())
    return text


def prompt_job(body_part):
    jobs = DressingRoom.getValidJob(body_part)
    text = "#ePlease select job\r\n"
    count = 0
    for job in jobs:
        text += "\n#b#L%s#%s (%s)#l\r\n" % (job, all_jobs.get(job), job)
        count += 1
    return text


def prompt_equips(equip_tuple, page, limit):
    size = equip_tuple.getLeft()
    equips = equip_tuple.getRight()

    fromIndex = page
    toIndex = page + limit

    if (toIndex > size):
        toIndex = size

    text = "Displaying " + repr(fromIndex) + " - " + repr(toIndex) + " of " + repr(size) + " \r\n"
    text += "#b#L10000#Back to Categories#l\r\n"
    if page != 0:
        text += "#b#L9998#Previous Page\r\n"
    else:
        text += "\r\n"
    if (toIndex) < size:
        text += "#b#L9999#Next Page#l\r\n"
    for equip in equips:
        item_id = equip.getItemId()
        rLevel = equip.getrLevel()
        text += "#b#L%s# (Lv. %s)#v%s# #z%s##l#k\r\n" % (repr(item_id), rLevel, repr(item_id), repr(item_id))

    return text


def prompt_options(item_id):
    selected_item = item_id
    text = (
        "You've selected #v{0}# #b#z{0}##k.\r\n\r\n"
        "Please select what you would like to do:\r\n"
        "#L1#Get one#l\r\n"
        # "#L10#Get 10#l\r\n"
        "#L0#Go back to the list, I misclicked :(#l"
    ).format(
        item_id,
    )

    return text


def prompt_search():
    text = (
        "Please enter the name of the item you want to search for:"
    )

    return text


def message_done(item_id):
    text = ("Got #v{0}# #b#z{0}##k.\r\n\r\n").format(item_id)
    return text


def browseItems():
    global flag_browse, selection_options, item, flag_main, equip_page
    if selection_equips == 9998:  # previous
        equip_page -= page_limit
    elif selection_equips == 9999:  # next
        equip_page += page_limit
    elif selection_equips == 10000:  # exit
        flag_browse = 0
        equip_page = 0
    else:
        # flag_browse = 0
        selection_options = sm.sendNext(prompt_options(selection_equips))
        item = selection_equips
        if selection_options == 0:  # exit - return to list
            flag_browse = 1
        elif selection_options != 0:
            for i in range(selection_options):
                sm.giveItem(item)
            sm.sendNext(message_done(item))
    return equip_page

def build_equip_list_text(size, search_result, equips):
    text = "Displaying {0} results for query '{1}'.\r\n".format(size, search_result)
    for id, string in equips.items():
        text += "#b#L{0}##v{0}# #z{0}##l#k\r\n".format(id)
    text += "#b#L10000#Back to Categories#l"
    return text


while flag_main == 1:
    selection_main = sm.sendNext(prompt_main())

    if selection_main == 555:  # non-cash
        selection_part = sm.sendNext(prompt_body_part())
        selection_job = sm.sendNext(prompt_job(selection_part))
        flag_browse = 1
        while flag_browse == 1:
            equips = sm.getDressingRoomEquipsNonCash(selection_part, selection_job, equip_page, page_limit)
            selection_equips = sm.sendNext(prompt_equips(equips, equip_page, page_limit))
            equip_page = browseItems()
    elif selection_main == 999 or selection_main == 888:  # search
        flag_search = 1
        search_result = sm.sendAskText(prompt_search(), "", 1, 12)
        search_tuple = sm.getDressingRoomEquipsSearch(search_result, selection_main == 999)

        size = search_tuple.getLeft()
        equips = search_tuple.getRight()

        text_list = build_equip_list_text(size, search_result, equips)

        if size == 0:
            sm.sendNext("There were no results for '{0}'.".format(search_result))
        else:
            flag_browse = 1
            while flag_browse == 1:
                selection_equips = sm.sendNext(text_list)

                if selection_equips == 10000:  # back to categories
                    flag_browse = 0
                    break

                selection_options = sm.sendNext(prompt_options(selection_equips))
                item = selection_equips

                if selection_options == 0:  # exit - return to list
                    pass
                else:
                    sm.giveItem(item)
                    sm.sendNext(message_done(item))

                # rebuild list after action
                text_list = build_equip_list_text(size, search_result, equips)
    else:  # cash
        flag_browse = 1
        while flag_browse == 1:
            equips = sm.getDressingRoomEquipsCash(selection_main, equip_page, page_limit)
            selection_equips = sm.sendNext(prompt_equips(equips, equip_page, page_limit))
            equip_page = browseItems()

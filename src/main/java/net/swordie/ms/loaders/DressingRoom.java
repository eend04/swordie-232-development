package net.swordie.ms.loaders;

import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.containerclasses.EquipItemInfo;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DressingRoom {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    public static final List<EquipItemInfo> androids = new ArrayList<>();
    public static final Map<BodyPart, List<EquipItemInfo>> cashByPart = new HashMap<>();
    public static final Map<BodyPart, Map<Integer, List<EquipItemInfo>>> byPartByJob = new HashMap<>(); // map bodypart -> rjob -> list(equip)

    public enum DressingRoomType {
        Hats(0),
        Tops(1),
        Bottoms(2),
        Overalls(3),
        Shoes(4),
        Gloves(5),
        Capes(6),
        Weapons(7),
        FaceAccessory(8),
        EyeAccessory(9),
        Rings(10),
        Androids(11);

        private final int id;

        DressingRoomType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static DressingRoomType getTypeById(int id) {
            return Arrays.stream(values()).filter(fae -> fae.getId() == id).findFirst().orElse(null);
        }
    }

    public DressingRoom() {
    }

    public static void load() {
        long start = System.currentTimeMillis();
        Collection<EquipItemInfo> equips = Objects.requireNonNull(ItemData.loadAllEquipFromFile()).values();

        equips.parallelStream().forEach(equip -> {
            if (equip.getItemId() < 1000000) {
                return;
            }

            if (equip.isCash()) {
                int bp = ItemConstants.getBodyPartFromItem(equip.getItemId(), 2);
                BodyPart bodyPart = BodyPart.getByVal(bp);
                synchronized (cashByPart) {
                    cashByPart.computeIfAbsent(bodyPart, k -> Collections.synchronizedList(new ArrayList<>())).add(equip);
                }
            } else {
                int job = equip.getrJob();
                int bp = ItemConstants.getBodyPartFromItem(equip.getItemId(), 2);
                BodyPart bodyPart = BodyPart.getByVal(bp);
                synchronized (byPartByJob) {
                    byPartByJob.computeIfAbsent(bodyPart, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(job, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(equip);
                }
            }
            if (ItemConstants.isAndroid(equip.getItemId())) {
                synchronized (androids) {
                    androids.add(equip);
                }
            }
        });

        log.info(String.format("Loaded Dressing Room in %dms.", System.currentTimeMillis() - start));
    }

    public static List<EquipItemInfo> getAndroids() {
        return androids;
    }

    public static List<EquipItemInfo> getCashByPart(BodyPart bodyPart) {
        return cashByPart.computeIfAbsent(bodyPart, k -> new ArrayList<>());
    }

    public static List<EquipItemInfo> getCashByPart(int bodyPart) {
        return getCashByPart(BodyPart.getByVal(bodyPart));
    }

    public static List<EquipItemInfo> getByPartByJob(BodyPart bodyPart, int job) {
        return byPartByJob.computeIfAbsent(bodyPart, k -> new HashMap<>()).computeIfAbsent(job, k -> new ArrayList<>());
    }

    public static List<EquipItemInfo> getByPartByJob(int bodyPart, int job) {
        return getByPartByJob(BodyPart.getByVal(bodyPart), job);
    }

    public static List<BodyPart> getValidPart() {
        Set<BodyPart> parts = byPartByJob.keySet();
        List<BodyPart> bodyPartsList = new ArrayList<>();
        for (BodyPart part : parts) {
            bodyPartsList.add(part);
        }

        Collections.sort(bodyPartsList);

        return bodyPartsList;
    }

    public static List<Integer> getValidJob(BodyPart bodyPart) {
        Set<Integer> jobs = byPartByJob.computeIfAbsent(bodyPart, k -> new HashMap<>()).keySet();

        List<Integer> jobsList = new ArrayList<>(jobs);
        Collections.sort(jobsList);

        return jobsList;
    }

    public static List<Integer> getValidJob(int bodyPart) {
        return getValidJob(BodyPart.getByVal(bodyPart));
    }


    /*
    {"Accessory", "Android", "Cap", "Cape", "Coat", "Dragon", "Face", "Glove",
     "Longcoat", "Mechanic", "Pants", "PetEquip", "Ring", "Shield", "Shoes", "Totem", "Weapon", "MonsterBook",
     "ArcaneForce"};
     */
}

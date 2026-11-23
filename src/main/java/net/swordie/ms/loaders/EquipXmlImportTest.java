
package net.swordie.ms.loaders;

import net.swordie.ms.connection.db.DatabaseManager;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.loaders.containerclasses.EquipItemInfo;

import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EquipXmlImportTest {

    // Eternal Archer items to import.
    // The Hood (1042435) is intentionally excluded.
    private static final int[] ITEM_IDS = {
            1062287, // Eternal Archer Pants
            1152214, // Eternal Archer Shoulder
            1082762, // Eternal Archer Gloves
            1073631, // Eternal Archer Shoes
            1103435,  // Eternal Archer Cape
            1042435 //top
    };

    public static void main(String[] args) throws Exception {

        DatabaseManager.init();

        // Load existing equipment data.
        Map<Integer, EquipItemInfo> existingEquips =
                ItemData.loadAllEquipFromFile();

        if (existingEquips == null || existingEquips.isEmpty()) {
            throw new IllegalStateException(
                    "Could not load existing equips.dat"
            );
        }

        System.out.println(
                "Loaded existing equipment: " + existingEquips.size()
        );

        Map<Integer, EquipItemInfo> imported = new LinkedHashMap<>();

        // Import all five Eternal Archer pieces.
        for (int itemId : ITEM_IDS) {

            Path importDir = Path.of(
                    ServerConstants.DIR,
                    "test-import"
            );

            Path xmlPath = findXmlFile(importDir, itemId);

            System.out.println("\n==================================");
            System.out.println("Importing item: " + itemId);
            System.out.println("Using XML: " + xmlPath);
            System.out.println("==================================");

            EquipItemInfo equip = importEquip(xmlPath, itemId);

            // Test conversion into Swordie's runtime Equip object.
            Equip runtimeEquip = equip.toEquip();

            if (runtimeEquip == null
                    || runtimeEquip.getItemId() != itemId) {
                throw new IllegalStateException(
                        "Runtime Equip conversion failed for " + itemId
                );
            }

            // Add imported item to Swordie's equipment map.
            ItemData.getEquips().put(itemId, equip);

            imported.put(itemId, equip);

            System.out.println(
                    "Runtime conversion successful: " + itemId
            );
        }

        // Save the complete equipment map to a separate test file.
        Path testDatPath = Path.of(
                ServerConstants.DAT_DIR,
                "equips-test.dat"
        );

        // Clear the previous test file before writing.
        try (RandomAccessFile raf = new RandomAccessFile(
                testDatPath.toFile(), "rw")) {
            raf.setLength(0);
        }

        ItemData.saveEquips(testDatPath.toString());

        System.out.println(
                "\nSaved test equipment file: " + testDatPath
        );

        // Read the offset table from the newly generated file.
        Map<Integer, Integer> offsets = new HashMap<>();

        IOHelper.loadOffsets(offsets, testDatPath.toString());

        // Verify every imported item in the serialized file.
        for (Map.Entry<Integer, EquipItemInfo> entry
                : imported.entrySet()) {

            int itemId = entry.getKey();
            EquipItemInfo equip = entry.getValue();

            if (!offsets.containsKey(itemId)) {
                throw new IllegalStateException(
                        "Item missing from saved offset table: " + itemId
                );
            }

            try (RandomAccessFile raf = new RandomAccessFile(
                    testDatPath.toFile(), "r")) {

                raf.seek(offsets.get(itemId));

                int savedId = raf.readInt();

                short savedJob = raf.readShort();
                short savedLevel = raf.readShort();

                short savedReqStr = raf.readShort();
                short savedReqDex = raf.readShort();
                short savedReqInt = raf.readShort();
                short savedReqLuk = raf.readShort();
                short savedReqPop = raf.readShort();

                short savedStr = raf.readShort();
                short savedDex = raf.readShort();

                raf.readShort(); // INT
                raf.readShort(); // LUK

                short savedPdd = raf.readShort();

                raf.readShort(); // MDD
                raf.readShort(); // Max HP
                raf.readShort(); // Max MP

                short savedPad = raf.readShort();

                raf.readShort(); // MAD
                raf.readShort(); // EVA
                raf.readShort(); // ACC
                raf.readShort(); // Craft
                raf.readShort(); // Speed
                raf.readShort(); // Jump
                raf.readShort(); // Damage rate
                raf.readShort(); // Stat rate
                raf.readShort(); // Boss damage rate

                short savedImdr = raf.readShort();
                short savedTuc = raf.readShort();

                int savedCharmExp = raf.readInt();
                int savedSetItemId = raf.readInt();

                // Compare serialized values against imported XML values.
                if (savedId != itemId
                        || savedJob != equip.getrJob()
                        || savedLevel != equip.getrLevel()
                        || savedReqStr != equip.getrStr()
                        || savedReqDex != equip.getrDex()
                        || savedReqInt != equip.getrInt()
                        || savedReqLuk != equip.getrLuk()
                        || savedReqPop != equip.getrPop()
                        || savedStr != equip.getiStr()
                        || savedDex != equip.getiDex()
                        || savedPdd != equip.getiPDD()
                        || savedPad != equip.getiPad()
                        || savedImdr != equip.getImdr()
                        || savedTuc != equip.getTuc()
                        || savedCharmExp != equip.getCharmEXP()
                        || savedSetItemId != equip.getSetItemID()) {

                    throw new IllegalStateException(
                            "Serialized equipment values did not match "
                                    + "for item " + itemId
                    );
                }

                System.out.println(
                        "\nSERIALIZATION TEST PASSED: " + itemId
                );

                System.out.println("Saved item ID: " + savedId);
                System.out.println("Saved level requirement: " + savedLevel);
                System.out.println(
                        "Saved STR / DEX: " + savedStr + " / " + savedDex
                );
                System.out.println(
                        "Saved PDD / PAD: " + savedPdd + " / " + savedPad
                );
                System.out.println("Saved IED: " + savedImdr);
                System.out.println("Saved upgrade slots: " + savedTuc);
                System.out.println("Saved set ID: " + savedSetItemId);
            }

            // Create an individual diagnostic report.
            writeReport(itemId, equip);
        }

        System.out.println("\n==================================");
        System.out.println("ALL FIVE ITEMS IMPORTED SUCCESSFULLY");
        System.out.println("==================================");
        System.out.println("Output: " + testDatPath);
    }

    /**
     * Finds an XML file using either:
     * 1062287.xml
     * 01062287.xml
     *
     * The filename without the leading zero is preferred
     * if both versions exist.
     */
    private static Path findXmlFile(Path directory, int itemId) {

        Path normalPath = directory.resolve(itemId + ".xml");

        if (Files.isRegularFile(normalPath)) {
            return normalPath;
        }

        Path paddedPath = directory.resolve(
                String.format(Locale.ROOT, "%08d.xml", itemId)
        );

        if (Files.isRegularFile(paddedPath)) {
            return paddedPath;
        }

        throw new IllegalStateException(
                "Missing XML file for item " + itemId
                        + ". Checked both:\n"
                        + normalPath + "\n"
                        + paddedPath
        );
    }

    private static EquipItemInfo importEquip(
            Path xmlPath,
            int expectedItemId
    ) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document = factory.newDocumentBuilder().parse(
                xmlPath.toFile()
        );

        Element root = document.getDocumentElement();

        Element info = findChild(root, "imgdir", "info");

        if (info == null) {
            throw new IllegalStateException(
                    "Could not find item info node: " + expectedItemId
            );
        }

        // Extract item ID from the XML root name.
        // Integer.parseInt automatically removes leading zeroes.
        String imageName = root.getAttribute("name");

        int itemId = Integer.parseInt(
                imageName.replace(".img", "")
        );

        if (itemId != expectedItemId) {
            throw new IllegalStateException(
                    "XML item ID mismatch. Expected "
                            + expectedItemId + ", found " + itemId
            );
        }

        EquipItemInfo equip = new EquipItemInfo();

        // Item identity and requirements.
        equip.setItemId(itemId);
        equip.setrJob((short) value(info, "reqJob"));
        equip.setrLevel((short) value(info, "reqLevel"));
        equip.setrStr((short) value(info, "reqSTR"));
        equip.setrDex((short) value(info, "reqDEX"));
        equip.setrInt((short) value(info, "reqINT"));
        equip.setrLuk((short) value(info, "reqLUK"));
        equip.setrPop((short) value(info, "reqPOP"));

        // Base stats.
        equip.setiStr((short) value(info, "incSTR"));
        equip.setiDex((short) value(info, "incDEX"));
        equip.setiInt((short) value(info, "incINT"));
        equip.setiLuk((short) value(info, "incLUK"));
        equip.setiPDD((short) value(info, "incPDD"));
        equip.setiMDD((short) value(info, "incMDD"));
        equip.setiMaxHp((short) value(info, "incMHP"));
        equip.setiMaxMp((short) value(info, "incMMP"));
        equip.setiPad((short) value(info, "incPAD"));
        equip.setiMad((short) value(info, "incMAD"));
        equip.setiEva((short) value(info, "incEVA"));
        equip.setiAcc((short) value(info, "incACC"));
        equip.setiSpeed((short) value(info, "incSpeed"));
        equip.setiJump((short) value(info, "incJump"));

        // Additional stats.
        equip.setDamR((short) value(info, "damR"));
        equip.setStatR((short) value(info, "statR"));
        equip.setImdr((short) value(info, "imdR"));
        equip.setBdr((short) value(info, "bdR"));

        // Upgrade and item properties.
        equip.setTuc((short) value(info, "tuc"));
        equip.setIucMax((short) value(info, "IUCMax"));
        equip.setSetItemID(value(info, "setItemID"));
        equip.setPrice(value(info, "price"));
        equip.setAttackSpeed(value(info, "attackSpeed"));
        equip.setCharmEXP(value(info, "charmEXP"));

        // Boolean properties.
        equip.setCash(value(info, "cash") != 0);
        equip.setExpireOnLogout(value(info, "expireOnLogout") != 0);
        equip.setExItem(value(info, "exItem") != 0);
        equip.setNotSale(value(info, "notSale") != 0);
        equip.setOnly(value(info, "only") != 0);
        equip.setTradeBlock(value(info, "tradeBlock") != 0);
        equip.setTradeAvailable((byte) value(info, "tradeAvailable"));
        equip.setEquipTradeBlock(value(info, "equipTradeBlock") != 0);
        equip.setFixedPotential(value(info, "fixedPotential") != 0);
        equip.setNoPotential(value(info, "noPotential") != 0);
        equip.setBossReward(value(info, "bossReward") != 0);
        equip.setSuperiorEqp(value(info, "superiorEqp") != 0);
        equip.setJokerToSetItem(value(info, "jokerToSetItem") != 0);
        equip.setOnlyEquip(value(info, "onlyEquip") != 0);

        equip.setiReduceReq((byte) value(info, "reduceReq"));
        equip.setFixedGrade(value(info, "fixedGrade"));
        equip.setSpecialGrade(value(info, "specialGrade"));

        equip.setAndroid(value(info, "android"));
        equip.setAndroidGrade(value(info, "grade"));

        // Potential options: Swordie expects seven entries.
        List<Integer> options = new ArrayList<>(
                Collections.nCopies(7, 0)
        );

        Element optionNode = findChild(info, "imgdir", "option");

        if (optionNode != null) {

            NodeList children = optionNode.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {

                Node node = children.item(i);

                if (!(node instanceof Element optionEntry)) {
                    continue;
                }

                try {
                    int index = Integer.parseInt(
                            optionEntry.getAttribute("name")
                    );

                    if (index >= 0 && index < options.size()) {
                        options.set(
                                index,
                                value(optionEntry, "option")
                        );
                    }

                } catch (NumberFormatException ignored) {
                    // Ignore non-numeric option entries.
                }
            }
        }

        equip.setOptions(options);

        return equip;
    }

    private static void writeReport(
            int itemId,
            EquipItemInfo equip
    ) throws Exception {

        StringBuilder report = new StringBuilder();

        report.append("XML IMPORT TEST\n");
        report.append("====================\n\n");

        report.append("Item ID: ").append(equip.getItemId()).append("\n");
        report.append("Required job: ").append(equip.getrJob()).append("\n");
        report.append("Required level: ").append(equip.getrLevel()).append("\n");
        report.append("Required STR: ").append(equip.getrStr()).append("\n");
        report.append("Required DEX: ").append(equip.getrDex()).append("\n\n");

        report.append("STR: ").append(equip.getiStr()).append("\n");
        report.append("DEX: ").append(equip.getiDex()).append("\n");
        report.append("PDD: ").append(equip.getiPDD()).append("\n");
        report.append("PAD: ").append(equip.getiPad()).append("\n");
        report.append("IED: ").append(equip.getImdr()).append("\n\n");

        report.append("Upgrade slots: ").append(equip.getTuc()).append("\n");
        report.append("Set item ID: ").append(equip.getSetItemID()).append("\n");
        report.append("Charm EXP: ").append(equip.getCharmEXP()).append("\n");
        report.append("Boss reward: ").append(equip.isBossReward()).append("\n");
        report.append("Equip trade block: ").append(equip.isEquipTradeBlock()).append("\n");
        report.append("Trade available: ").append(equip.getTradeAvailable()).append("\n\n");

        report.append("Runtime conversion: SUCCESS\n");

        Path reportPath = Path.of(
                ServerConstants.DIR,
                "test-import",
                itemId + "-import-test.txt"
        );

        Files.writeString(
                reportPath,
                report.toString(),
                StandardCharsets.UTF_8
        );

        System.out.println("Report written: " + reportPath);
    }

    private static int value(
            Element parent,
            String propertyName
    ) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (!(node instanceof Element element)) {
                continue;
            }

            if (propertyName.equals(element.getAttribute("name"))) {

                String value = element.getAttribute("value");

                if (value == null || value.isBlank()) {
                    return 0;
                }

                return Integer.parseInt(value);
            }
        }

        // Matches Swordie's existing default-value behaviour.
        return 0;
    }

    private static Element findChild(
            Element parent,
            String tagName,
            String childName
    ) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (node instanceof Element element
                    && tagName.equals(element.getTagName())
                    && childName.equals(element.getAttribute("name"))) {

                return element;
            }
        }

        return null;
    }
}


package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.jwzlib.WzFile;
import net.swordie.ms.jwzlib.WzObject;

public class CapWzTest {

    public static void main(String[] args) {

        String path = ServerConstants.WZ_DIR
                + "/Data/Wz/Cap/Cap_000.wz";

        System.out.println("Opening: " + path);

        WzFile file = new WzFile(path);

        try {
            System.out.println("\n--- Top-level nodes ---");

            for (WzObject<?, ?> child : file.getChildren().values()) {
                System.out.println(child.getName());
            }

            System.out.println("\nTest completed.");

        } finally {
            file.clear();
        }
    }
}

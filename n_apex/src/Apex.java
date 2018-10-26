import java.nio.*;
import java.util.*;
import java.io.*;
import java.io.File;

public class Apex {

    // Block size in KB
    static int blockSize = 4;

    static File mount = new File("../ApexMountDir");

    public static void reset(){
        if (!mount.exists()) {
            mount.mkdir();
        }
        else {
            String[]entries = mount.list();
            for(String s: entries){
                File currentFile = new File(mount.getPath(),s);
                currentFile.delete();
            }
            mount.delete();
            if (!mount.exists()) {
                mount.mkdir();
            }
        }
    }

    public static void main (String[] args) throws Exception{

        // Input scanner for operations
        Scanner input = new Scanner(System.in);

        // File Streams for blocks
        FileOutputStream oDir = new FileOutputStream("ApexDir.ser");
        FileInputStream iDir = new FileInputStream("ApexDir.ser");

        // File Streams for FAT
        FileOutputStream oMft = new FileOutputStream("AFAT.ser");
        FileInputStream iMft = new FileInputStream("AFAT.ser");

        // Reset Apex mount directory
        reset();

        // Initializing 256 MB memory
        ApexMemory memory = new ApexMemory(16, 16);

    }
}


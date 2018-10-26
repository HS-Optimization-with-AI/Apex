import java.nio.*;
import java.util.*;
import java.io.*;
import java.io.File;

public class Apex {

    // Block size in MB
    static int blockSize = 1;

    static File mount = new File("../ApexMountDir");

    // File Streams for ApexMemory
    static FileOutputStream oDir;
    static FileInputStream iDir;

    // ApexMemory
    static ApexMemory memory;

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

    public static void dumpMemory() throws Exception{
        // Delete old Apex directory
        File oldDir = new File("../ApexDir.ser");
        oldDir.delete();

        FileOutputStream fos = new FileOutputStream("../ApexDir.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(memory);
        out.close();
        fos.close();
    }

    public static void loadMemory()throws Exception{
        ApexMemory temp;
        FileInputStream fis = new FileInputStream("../ApexDir.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        temp = (ApexMemory) in.readObject();
        in.close();
        fis.close();
        memory = temp;
    }

    public static void main (String[] args) throws Exception{

        // Reset Apex mount directory
        reset();

        // Input scanner for operations
        Scanner input = new Scanner(System.in);

        // User response - integer
        int response;

        System.out.print("Welcome to Apex File System\nApex is an adaptive FS optimized for data recovery.\n\n");
        System.out.print("Would you like to start a new disk (1) or resume from earlier (2) ? : ");

        response = input.nextInt();

        if(response == 1){
            // Initializing 4GB memory
             memory = new ApexMemory(64  , 64);
             dumpMemory();
        }
        else{
            // Load memory from binary
            loadMemory();
        }




    }
}


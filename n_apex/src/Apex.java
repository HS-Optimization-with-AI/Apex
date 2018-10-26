import java.nio.*;
import java.nio.file.Files;
import java.util.*;
import java.io.*;
import java.io.File;

public class Apex {

    // Block size in MB
    static int blockSize = 1;

    // Apex mount Directory
    static File mount = new File("../ApexMountDir");

    // Input scanner for operations
    static Scanner input = new Scanner(System.in);

    // File Streams for ApexMemory
    static FileOutputStream oDir;
    static FileInputStream iDir;

    // ApexMemory
    static ApexMemory memory;

    private static void reset(){
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

    private static void dumpMemory() throws Exception{
        // Delete old Apex directory
        File oldDir = new File("../ApexDir.ser");
        oldDir.delete();

        FileOutputStream fos = new FileOutputStream("../ApexDir.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(memory);
        out.close();
        fos.close();
    }

    private static void loadMemory()throws Exception{
        ApexMemory temp;
        FileInputStream fis = new FileInputStream("../ApexDir.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        temp = (ApexMemory) in.readObject();
        in.close();
        fis.close();
        memory = temp;
    }

    private static boolean checkFile(String filename){
        String[]entries = mount.list();
        for(String s: entries){
            if (s.equals(filename))
                return true;
        }
        return false;
    }

    private static void printFiles(){
        System.out.println("Files in Apex Mount Directory : ");
        String[]entries = mount.list();
        for(String s: entries){
            System.out.println(s);
        }
    }

    private static void printMemoryFiles(){
        System.out.println("File in Apex Directory : ");
        memory.printCurFiles();
    }

    private static void printDelMemFiles(){
        System.out.println("Recoverable deleted files in Apex Directory : ");
        memory.printDelFile();
        System.out.println("Non recoverable deleted files in Apex Directory : ");
        memory.printObsFile();
    }

    private static int getResponse(){
        System.out.print("Enter the following options :\n1 = create file\n2 = delete file\n3 = read file\n4 = list mount dir files\n5 = list Apex dir files\n6 = list deleted files\noption : ");
        return input.nextInt();
    }
    private static void create() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        if(!checkFile(name)){
            System.out.println("No such file in mount dir!");
        }
        else if(memory.checkFile(name)) {
            System.out.println("File already in Apex dir!");
        }
        else{
            File file = new File("myFile");
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
            memory.createFile(name, 0, fileData);
            dumpMemory();
        }
    }

    private static void delete() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else{
            memory.deleteFile(name);
            dumpMemory();
        }
    }

    private static void read() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else{
            memory.readWriteFile(name);
            dumpMemory();
        }
    }

    public static void main (String[] args) throws Exception{

        // Reset Apex mount directory
        reset();

        // User response - integer
        int response;

        System.out.print("Welcome to Apex File System\nApex is an adaptive FS optimized for data recovery.\n\n");
        System.out.print("Would you like to start a new disk (1) or resume from earlier (2) ? : ");

        response = input.nextInt();

        if(response == 1){
            // Initializing 4GB memory
             memory = new ApexMemory(64  , 64);
             memory.updateParams(4, 7, 1, 9);
             dumpMemory();
        }
        else{
            // Load memory from binary
            loadMemory();
        }

        while(true){
            response = getResponse();

            if(response == 1){
                create();
            }
            else if(response == 2){
                delete();
            }
            else if(response == 3){
                read();
            }
            else if(response == 4){
                printFiles();
            }
            else if(response == 5){
                printMemoryFiles();
            }
            else if(response == 6){
                printDelMemFiles();
            }
        }
    }
}


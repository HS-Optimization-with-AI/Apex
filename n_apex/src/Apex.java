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
            if (filename.equals(mount.getPath() + "\\" + s))
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
        System.out.print("\n\nEnter the following options :\n1 = create file\n2 = delete file\n3 = read file\n4 = list mount dir files\n5 = list Apex dir files\n6 = list deleted files\n7 = recover deleted file\n8 = flush\noption : ");
        int a = input.nextInt();
        System.out.println();
        return a;
    }
    private static void create() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        name = mount.getPath() + "\\" + name;
        if(!checkFile(name)){
            System.out.println("No such file in mount dir!");
        }
        else if(memory.checkFile(name)) {
            System.out.println("File already in Apex dir!");
        }
        else{
            File file = new File(name);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
            memory.createFile(name, 0, fileData);
            System.out.println("Memory Utilization = " + memory.memUsage());
        }
    }

    private static void delete() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        name = mount.getPath() + "\\" + name;
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else{
            memory.deleteFile(name);
        }
    }

    private static void read() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        name = mount.getPath() + "\\" + name;
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else if(checkFile(name)){
            System.out.println("First delete file from mount dir!");
        }
        else{
            memory.readWriteFile(name);
            byte[] fileData = memory.getFileBytes(name);
            File file = new File(name);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
    }

    private static void recover() throws Exception{
        System.out.println("Enter name of file : ");
        String name = input.next();
        name = mount.getPath() + "\\" + name;
        if(!memory.checkDelFile(name)){
            System.out.println("No such deleted file in Apex dir!");
        }
        else if(checkFile(name)){
            System.out.println("First delete file from mount dir!");
        }
        else{
            memory.recoverFile(name);
            byte[] fileData = memory.getFileBytes(name);
            File file = new File(name);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
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
             System.out.println("Initialising 4GB contiguous space on disk...\n");
             memory = new ApexMemory(64  , 64);
             memory.updateParams(4, 7, 1, 9);
             dumpMemory();
             System.out.println("Formatting complete.");
        }
        else{
            // Load memory from binary
            System.out.println("Loading disk...\n");
            loadMemory();
            System.out.println("Disk load successful.");
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
            else if(response == 7){
                recover();
            }
            else{
                dumpMemory();
            }
        }
    }
}


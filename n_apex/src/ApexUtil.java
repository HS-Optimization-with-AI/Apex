import java.io.*;
import java.util.Scanner;

import static javafx.application.Platform.exit;

public class ApexUtil {

    // Block size in MB
    static int blockSize = 1;

    // Apex mount Directory
    static File mount = new File("./ApexMountDir");

    // Input scanner for operations
    static Scanner input = new Scanner(System.in);

    // File Streams for ApexMemory
    static FileOutputStream oDir;
    static FileInputStream iDir;

    // ApexMemory
    static ApexMemory memory;

    public static void reset(){
        if (!mount.exists()) {
            mount.mkdir();
        }
    }

    public static void dumpMemory() throws Exception{
        // Delete old Apex directory
        File oldDir = new File("./ApexDir.ser");
        oldDir.delete();

        FileOutputStream fos = new FileOutputStream("./ApexDir.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(memory);
        out.close();
        fos.close();
    }

    public static void loadMemory()throws Exception{
        ApexMemory temp;
        FileInputStream fis = new FileInputStream("./ApexDir.ser");
        ObjectInputStream in = new ObjectInputStream(fis);
        temp = (ApexMemory) in.readObject();
        in.close();
        fis.close();
        memory = temp;
    }

    public static boolean checkFile(String filename){
        String[]entries = mount.list();
        for(String s: entries){
            if (filename.equals(mount.getPath() + "\\" + s))
                return true;
        }
        return false;
    }

    public static String[] printFiles(){
        System.out.println("Files in Apex Mount Directory : ");
        String[]entries = mount.list();
        return entries;
    }

    public static String[] printMemoryFiles(){
        System.out.println("File in Apex Directory : ");
        return memory.getCurFiles();
    }

    public static int[] printMemoryFilesColors(){
        return memory.getCurFilesColors();
    }

    public static int[][] getMem(){
        return memory.getMemory();
    }

    public static String[] getLegend() { return  memory.getLegend(); }

    public static String[] getDelMemFiles(){
        System.out.println("Recoverable deleted files in Apex Directory : ");
        return memory.getDelFile();
    }

    public static String[] getObsMemFiles(){
        System.out.println("Non recoverable deleted files in Apex Directory : ");
        return memory.getObsFile();
    }

    public static int getResponse(){
        System.out.print("\n\nEnter the following options :\n1 = create file\n2 = delete file\n3 = read file\n4 = list mount dir files\n5 = list Apex dir files\n6 = list deleted files\n7 = recover deleted file\n8 = flush\n9 = print full memory\n10 = exit\noption : ");
        int a = input.nextInt();
        System.out.println();
        return a;
    }
    public static void create(String path, String name) throws Exception{
        if(memory.checkFile(name)) {
            System.out.println("File already in Apex dir!");
        }
        else{
            File file = new File(path);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
            memory.createFile(name, 0, fileData);
            System.out.println("Memory Utilization = " + memory.memUsage());
        }
    }

    public static void delete(String name){
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else{
            memory.deleteFile(name);
        }
    }

    public static void read(String path, String name) throws Exception{
        if(!memory.checkFile(name)){
            System.out.println("No such file in Apex dir!");
        }
        else{
            memory.readWriteFile(name);
            byte[] fileData = memory.getFileBytes(name);
            File file = new File(path+name);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
    }

    public static void recover(String path, String name) throws Exception{
        if(!memory.checkDelFile(name)){
            System.out.println("No such deleted file in Apex dir!");
        }
        else{
            byte[] fileData = memory.recoverFile(name);
            File file = new File(path+name);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
    }

    public static void main (String[] args){

        // Reset Apex mount directory
//        reset();
        // Initializing 4GB memory
        System.out.println("Initialising 4GB contiguous space on disk...\n");
        memory = new ApexMemory(64  , 64);
        memory.updateParams(4, 7, 1, 9);
        System.out.println("Formatting complete.");
    }
}


import sun.java2d.ScreenUpdateManager;

import javax.rmi.CORBA.Stub;
import java.io.*;
import java.util.Scanner;
import java.awt.Desktop;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

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

    // StubMemory
    static StubMemory stub;

    // Log string
    static String logs;

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
        updateLogs("Apex Memory dumped to disk");
    }

    public static void dumpStubMemory() throws Exception{
        // Delete old Stub directory
        File oldDir = new File("./StubDir.ser");
        oldDir.delete();

        FileOutputStream fos = new FileOutputStream("./StubDir.ser");
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(stub);
        out.close();
        fos.close();
        updateLogs("Stub Memory dumped to disk");
    }

    public static void updateLogs(String s){
        logs = ">> " + s + "\n" + logs;
    }

    public static String Logs(){
        return logs;
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

    public static int[] printStubFilesColors(){
        return stub.getCurFilesColors();
    }

    public static String[] printDeletedFiles(){
        System.out.println("File in Apex Directory : ");
        return memory.getDelFile();
    }

    public static int[] printDeletedFilesColors(){
        return memory.getDelFilesColors();
    }

    public static int[] printDeletedStubFilesColors(){
        return stub.getDelFilesColors();
    }

    public static int[][] getMem(){
        return memory.getMemory();
    }

    public static int[][] getStub() {return stub.getMemory(); }

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
            updateLogs("File already in Apex dir!");
        }
        else{
            int color = ThreadLocalRandom.current().nextInt(1, 64 + 1);
            File file = new File(path);
            byte[] fileData = new byte[(int) file.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
            updateLogs("Created new file in Apex Dir :\npath : " + path + "\nfilename : " + name);
            memory.createFile(name, 0, fileData, color);
            stub.createFile(name, 0, fileData, color);
            updateLogs("Memory Utilization = " + memory.memUsage());
        }
    }

    public static void delete(String name){
        if(!memory.checkFile(name)){
            updateLogs("No such file in Apex dir!");
        }
        else{
            int color = -1 * ThreadLocalRandom.current().nextInt(10, 60 + 1);
            updateLogs("Deleted file with name : " + name);
            updateLogs("Memory Utilization = " + memory.memUsage());
            memory.deleteFile(name, color);
            stub.deleteFile(name, color);
        }
    }

    public static void read(String path, String name) throws Exception{
        if(!memory.checkFile(name)){
            updateLogs("No such file in Apex dir!");
        }
        else {
            memory.readWriteFile(name);
            byte[] fileData = memory.getFileBytes(name);
            File file = new File(path + name);
            updateLogs("Reading file :\nname : " + name + "\nto directory : " + path);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
        Desktop desktop = Desktop.getDesktop();
        File file = new File(path+name);
        desktop.open(file);
    }


    public static void recover(String path, String name) throws Exception{
        if(!memory.checkDelFile(name)){
            updateLogs("No such deleted file in Apex dir!");
        }
        else{
            byte[] fileData = memory.recoverFile(name);
            File file = new File(path+name);
            updateLogs("Recovering Apex file : \nname : " + name + "\nto directory : " + path);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
    }

    public static void recoverStub(String path, String name) throws Exception{
        if(!stub.checkDelFile(name)){
            updateLogs("No such deleted file in Apex dir!");
        }
        else{
            byte[] fileData = stub.recoverFile(name);
            File file = new File(path+name);
            updateLogs("Recovering Stub file : \nname : " + name + "\nto directory : " + path);
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(fileData);
            dos.close();
        }
    }

    public static void main (String[] args){

        // Reset Apex mount directory
//        reset();
        // Initializing 4GB memory
        logs = new String();
        updateLogs("Initialising 1GB contiguous space on disk for each File System...");
        memory = new ApexMemory(64  , 64);
        stub = new StubMemory(64, 64);
        memory.updateParams(4, 7, 1, 9);
        stub.updateParams(4, 7, 1, 9);
        updateLogs("Formatting complete.");
    }
}


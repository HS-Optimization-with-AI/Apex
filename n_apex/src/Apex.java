import java.nio.*;
import java.util.*;
import java.io.*;

public class Apex {

    // Block size in KB
    static int blockSize = 4;


    public static void main (String[] args) throws Exception{

        // File Streams for blocks
        FileOutputStream oDir = new FileOutputStream("ApexDir.ser");
        FileInputStream iDir = new FileInputStream("ApexDir.ser");

        // File Streams for FAT
        FileOutputStream oMft = new FileOutputStream("AFAT.ser");
        FileInputStream iMft = new FileInputStream("AFAT.ser");

        // Initializing 256 MB memory
        Memory memory = new Memory(16, 16);
        
    }
}


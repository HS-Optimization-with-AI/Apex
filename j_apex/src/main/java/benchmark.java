import java.util.*;
import java.lang.*;
import java.io.*;

public class benchmark{


	public static int fileSize = 1024; // File Size in bytes
	public static int fileNumbers = 1000; // Number of files to be created
	public static String absoluteFilePath = "./mountdir/file.txt";

	public static void main(String args[]) throws Exception{

		fileSize = Integer.parseInt(args[0]);
		fileNumbers = Integer.parseInt(args[1]);

		System.out.println("Number of files :  " + fileNumbers);
		System.out.println("File size in bytes : " + fileSize);


		// WRITE TEST

		long start = System.currentTimeMillis();

		for(int i = 1; i <= fileNumbers; i++){

        	FileOutputStream fos = new FileOutputStream(absoluteFilePath + i);
        	for(int j = 0; j < fileSize; j++){
        		fos.write('0');
        	}
			
			fos.flush();
			fos.close();

		}

		long end = System.currentTimeMillis();
		System.out.println("Total write time : " + (end - start) + " milliseconds");



		// DELETE TEST

		start = System.currentTimeMillis();

		for(int i = 1; i <= fileNumbers; i++){

			File file = new File(absoluteFilePath + i); 

        	file.delete();
		}

		end = System.currentTimeMillis();
		System.out.println("Total delete time : " + (end - start) + " milliseconds");

	}
}
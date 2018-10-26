import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;

public class ApexMemory implements java.io.Serializable{

    // Memory size parameters

    static int MAX_PARAM = 9;
    static int MIN_PARAM = 0;

    int width;
    int height;

    double mem_util;

    int lambda, sigma, rho, mu;

    //blocks
    ApexBlock[][] blocks;

    // ApexBlock heap
    PriorityQueue<ApexBlock> unusedApexBlocks;

    HashSet<ApexBlock> usedApexBlocks;
//    PriorityQueue<ApexBlock> usedApexBlocks;

    //List of all files
    ArrayList<ApexFile> currentFileList;
    ArrayList<ApexFile> deletedFileList;

    ArrayList<Pair<Integer, Integer>> directions = new ArrayList<Pair<Integer, Integer>>(8);

    int totalCreatedFiles;

    ApexMemory(int w, int h) {
        this.width = w;
        this.height = h;
        this.blocks = new ApexBlock[w][h];

        this.mem_util = 0.0;

        this.lambda = 1;
        this.sigma = 1;
        this.rho = 1;
        this.mu = 1;

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                this.blocks[i][j] = new ApexBlock();
                this.blocks[i][j].i = i;
                this.blocks[i][j].j = j;
            }
        }

        this.currentFileList = new ArrayList<>();
        this.deletedFileList = new ArrayList<>();
        //put all blocks in unused heap now
        this.usedApexBlocks = new HashSet<>(w * h);

        this.unusedApexBlocks = new PriorityQueue<>(w * h, new ApexBlockComparator());
//        for ()
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                this.unusedApexBlocks.add(this.blocks[i][j]);
            }
        }

        this.directions.add(new Pair<>(-1, -1));
        this.directions.add(new Pair<>(-1, 0));
        this.directions.add(new Pair<>(-1, 1));
        this.directions.add(new Pair<>(0, -1));
//  this.directions.add(new Pair<>( 0, 0));
        this.directions.add(new Pair<>(0, 1));
        this.directions.add(new Pair<>(1, -1));
        this.directions.add(new Pair<>(1, 0));
        this.directions.add(new Pair<>(1, 1));

        this.totalCreatedFiles = 0;
    }

    void createFile(String filename, int link_factor, int num_blocks) {

        HashSet<ApexBlock> block_list = new HashSet<>(num_blocks);

        for (int i = 0; i < num_blocks; i++) {
            ApexBlock b = this.unusedApexBlocks.poll();

            if (b == null) {
                throw new java.lang.RuntimeException("Memory Full!");
            }

            block_list.add(b);
            this.usedApexBlocks.add(b);
        }

        //if enough blocks
        ApexFile f = new ApexFile(filename, block_list, link_factor);

        this.currentFileList.add(f);

        this.totalCreatedFiles++;

        //Refresh the memory pf, sf etc;
        this.refresh();
    }

    void deleteFile(int fileIndex) {
        ApexFile cf = this.currentFileList.get(fileIndex);
        this.deletedFileList.add(cf);
        this.currentFileList.remove(fileIndex);
        assert cf != null;
        cf.deleteFile();
        for (ApexBlock b : cf.blockList) {
            this.usedApexBlocks.remove(b);
            this.unusedApexBlocks.add(b);
        }
        this.refresh();
    }

    // Update factors of all blocks per transaction

    void readWriteFile(int fileIndex) {
        this.currentFileList.get(fileIndex).readWriteFile();
        this.refresh();
    }

    void updateSF() {
        // neighbouring blocks's pf's avg in sf of this block

        int count = 0;
        double sum = 0.0;

        int newi = 0;
        int newj = 0;

        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {

                sum = 0.0;
                count = 0;

                for (int t = 0; t < directions.size(); t++) {
                    newi = i + directions.get(t).getKey();
                    newj = i + directions.get(t).getValue();

                    if (newi >= 0 && newi < this.width && newj >= 0 && newj < this.height) {
                        sum += this.blocks[newi][newj].pf;
                        count++;
                    }
                }
                this.blocks[j][i].setSF((int) (sum / count));

            }
        }

    }

    void printMemory() {

        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                ApexBlock b = this.blocks[i][j];
                if (!b.used) {
                    System.out.print("(-,");
                }
                else{
                    System.out.print("(" + this.currentFileList.indexOf(b.parentFile) + ",");
                }
                System.out.print(b.hf + "," + b.uf + "," + b.sf + "," + b.lf + ":" + b.pf + ")\t");
            }
            System.out.println();
        }

        System.out.println("Number of current files : " + this.currentFileList.size());
        System.out.println("Number of deleted files : " + this.deletedFileList.size());
        int obsolete = 0;
        for(ApexFile file : this.deletedFileList){
            if (file.fileState == ApexFile.STATE.OBSOLETE){
                obsolete+=1;
            }
        }
        System.out.println("Number of obselete files out of deleted files : " + obsolete);
        System.out.println("Memory Utilization : " + this.mem_util);
        System.out.println("Recovery ratios of the deleted (not obsolete) files : " + (this.deletedFileList.size() - obsolete));
        double sumrr = 0;
        for(int i = 0; i < this.deletedFileList.size(); i++){
            System.out.print(this.deletedFileList.get(i).getRecoveryRatio() + ", ");
            sumrr += this.deletedFileList.get(i).getRecoveryRatio();
        }
        System.out.println("\nSum of recovery ratios : " + sumrr);
    }

    void refresh() {
        this.updateSF();

        PriorityQueue<ApexBlock> newHeap = new PriorityQueue<>(this.width * this.height, new ApexBlockComparator());

        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                ApexBlock b = this.blocks[i][j];
                b.updatepf(this.lambda, this.sigma, this.rho, this.mu);
                if (b.used == false) {
                    newHeap.add(this.blocks[i][j]);
                }
            }
        }

        this.unusedApexBlocks = newHeap;

        this.mem_util = ((double) (this.usedApexBlocks.size())) * 100 / (this.width * this.height);

    }

    void updateParams(int lambda, int sigma, int rho, int mu) {
        assert (lambda <= MAX_PARAM) && (lambda >= MIN_PARAM);
        assert (sigma <= MAX_PARAM) && (sigma >= MIN_PARAM);
        assert (rho <= MAX_PARAM) && (rho >= MIN_PARAM);
        assert (mu <= MAX_PARAM) && (mu >= MIN_PARAM);

        this.lambda = lambda;
        this.sigma = sigma;
        this.rho = rho;
        this.mu = mu;

    }

    // Give block with maximum priority score
//    ApexBlock giveMaxPriority(){
//
//    }

    // Memory Usage percentage
    double memUsage() {
        return mem_util;
    }

}

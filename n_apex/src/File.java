import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static javafx.application.Platform.exit;

public class File {

    public

    static int lf_max = 10;
    static int lf_min = 0;

    //doesn't change after file's initial allocation
    int original_size;

    // Block list
    HashSet<Block> blockList ;//= new ArrayList<>();

    int uf = 0;

    enum STATE{
        USED,  //Currently allocated blocks
        DELETED, // File deleted and blocks can be overwritten
        OBSOLETE; // No blocks allocated, file ignored
    }
    STATE fileState;

    //only 0 or 10
    int linking_factor;

    double slm;


    File(HashSet<Block> block_list, int lf){

        for (Block b: block_list){
            assert (b.used == false);
            b.allocate(this, lf);
        }

        this.blockList = block_list;
        this.fileState = STATE.USED;
        this.linking_factor = lf;

        //not changed
        this.original_size = block_list.size();

        this.computeSlm();
        this.uf = 1;

        for (Block block: this.blockList){
            block.increaseUF();
        }

    }

    void deleteFile(){
        if(!(this.fileState == STATE.USED)){
            System.out.println("Trying to Delete an existing or obsolete file!"); exit();
        }

        this.fileState = STATE.DELETED;
        //deallocate file
        for (Block b : this.blockList){
            //deallocate not delete because, deleteBlock if final delete
            // doesn't change parent file pointer of this block
            b.setUnused();
        }
    }

    //delete this block from me becuase, allocating to someone else
    void deleteBlock(Block b){
        assert this.blockList.contains(b);

        this.blockList.remove(b);

        for (Block block: this.blockList){
            block.increaseHF();
        }

        if (this.blockList.size() == 0){
            this.fileState = STATE.OBSOLETE;
        }
    }

    void readWriteFile(){
        for (Block block: this.blockList) {
            block.increaseUF();
        }
    }

    double getRecoveryRatio(){
        assert fileState == STATE.DELETED;

        //current size
        int cs = this.blockList.size();
        if(cs == 0)
            return 0;

        double rr = 0;
        switch (linking_factor){
            case 0: rr = (double)(cs)/((double)(this.original_size)); break;
            case 10: rr = (cs < this.original_size) ? 0: 1 ;
        }

        this.uf = 0;
        for (Block b: this.blockList){
            this.uf += b.uf;
            break;
        }
        this.uf = this.uf / this.blockList.size();
        return  rr;
    }

    void computeSlm(){
        double ui = 0.0, uj = 0.0;

        for (Block b: this.blockList){
            ui += b.i; uj += b.j;
        }

        int numElem = this.blockList.size();
        ui /= numElem;
        uj /= numElem;

        double varTotal = 0.0;

        for (Block b: this.blockList){
            varTotal += Math.pow(b.i - ui, 2) + Math.pow(b.j - uj, 2);
        }

        varTotal /= numElem;

        this.slm = Math.pow(varTotal, 0.5);

    }

    double  getSlm(){
        //
        assert this.fileState == STATE.USED;

        //Return the RMS deviations of all blocks in 2D
        return (this.slm);
    }
}

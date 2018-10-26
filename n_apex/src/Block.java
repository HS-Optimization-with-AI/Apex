import java.math.BigInteger;
import java.util.*;

public class Block {

    public
    static int MAX_PF = 1000 ;
    static int MIN_PF = -1000;
    static int MAX_PARAM = 9;
    static int MIN_PARAM = 0;
    static int MAX_FACTORS = 100000;
    static int MIN_FACTORS = -100000;

    // Factors
    int hf ; // History Factor
    int uf ; // Usage Factor
    int sf ; // Spatial Factor
    int lf ; // Linking Factor

    // Used boolean
    boolean used ;

    // Priority Score
    int pf ;

    int i, j;

    // Pointer to file
    File parentFile ;

    Block(){
        this.hf = 0; // History Factor
        this.uf = 1; // Usage Factor
        this.sf = 0; // Spatial Factor
        this.lf = 1; // Linking Factor

        this.used = false;
        this.pf = 0;
        this.parentFile = null ;

        //we'll change mannually i, j after init
//        this.i = i_;
//        this.j = j_;
    }

    //allocate this block to a file (UNUSED TO USED TRANSITION)
    void allocate(File parent_file, int link_factor){
        assert this.used == false;

        //delete the block from parent file

        // CHECK THAT IF THE PARENT FILE IS NULL THEN DO NOT DO THE DELETE BLOCK STEP
        if(this.parentFile != null){
            this.parentFile.deleteBlock(this);
        }

        this.parentFile = parent_file;
        this.used = true;
        this.hf = 1;//reset
        this.uf = 1;//reset
        this.lf = link_factor;//binaries or non binaries
    }

    //allocate this block to a file (USED TO UNUSED TRANSITION)
    // Set unused
    void setUnused(){
        assert this.used == true;

        //let the parent pointer remain
        this.hf = 0;
        this.used = false;

    }

    void increaseHF(){
        assert this.used == false;

        this.hf = Math.min(MAX_FACTORS, this.hf + 1);
    }

    void increaseUF(){
        assert this.used == true;

        this.uf = Math.min(MAX_FACTORS, this.uf + 1);
    }

    void setSF(int val){
        this.sf = val;
    }

    // Update Priority Score
    void updatepf(int lambda, int sigma, int rho, int mu){

        //ranges of l, sig, rho , mu
        //ranges of pf check
        // ASSERTING THIS IN THE MEMORY FUNCTION
//        assert (lambda <= MAX_PARAM) && (lambda >= MIN_PARAM );
//        assert (sigma  <= MAX_PARAM) && (sigma  >= MIN_PARAM );
//        assert (rho    <= MAX_PARAM) && (rho    >= MIN_PARAM );
//        assert (mu     <= MAX_PARAM) && (mu     >= MIN_PARAM );

        int temp = lambda*this.hf - sigma*this.uf + rho*this.sf + mu*this.lf ;

        this.pf = Math.min(Math.max(temp, MIN_PF), MAX_PF);

    }


}

class BlockComparator implements Comparator<Block>{
    public int compare(Block b1, Block b2){
        if(b1.pf < b2.pf){
            return 1;
        }
        else if(b1.pf == b2.pf){
            if(b1.j < b2.j){
                return 1;
            }
            else if(b1.j == b2.j){
                if(b1.i < b2.i){
                    return 1;
                }
                else if(b1.i == b2.i){
                    return 0;
                }
                else{
                    return -1;
                }
            }
            else{
                return -1;
            }
        }
        else{
            return -1;
        }

    }
}

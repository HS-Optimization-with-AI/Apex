import javafx.util.Pair;
import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;

import jnr.ffi.Memory;

import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import static javafx.application.Platform.exit;
import static jnr.ffi.Platform.OS.LINUX;
import static jnr.ffi.Platform.OS.WINDOWS;


public class ApexFS extends FuseStubFS {

    public
    // A class which has the common features of dir's and files
    abstract class ApexPath {
        public

        String name;
        ApexDir parent;

        ApexPath(String name){
            this(name, null);
        }

        ApexPath(String name, ApexDir parent){
            this.name = name;
            this.parent = parent;
        }

        // Synch used in case it's threaded by default, but intended to run on a single thread
        synchronized void delete(){
            if (parent != null) {
                parent.deleteChild(this);
                parent = null;
            }
        }

        // Not sure, what this method is for right now
        ApexPath find(String path){
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.equals(name) || path.isEmpty()) {
                return this;
            }
            return null;
        }

        // This was supposed to be abstract class therefore it's implementation was left right now
        abstract void getattr(FileStat stat);

        void rename(String newName){
            while (newName.startsWith("/")) {
                newName = newName.substring(1);
            }
            name = newName;
        }

    }

    // Didn't think about dir's before, because it consisted solely of files
    class ApexDir extends ApexPath{
        public
        //How will this directory's sub(path's) will be stored ?
                // As a list for now // ApexPaths has both dir and files
        List<ApexPath> contents = new ArrayList<>();

        ApexDir(String name){ super(name);}
        ApexDir(String name, ApexDir parent){super(name, parent);}

        synchronized void add(ApexPath p){
            contents.add(p);
            p.parent = this;
        }

        synchronized void deleteChild(ApexPath child){ contents.remove(child);}

        // depends on implementation of directory?
        // recursively searching in 'apexpaths'
        @Override
        ApexPath find(String path) {
            // We don't have to increase usage factors here
            if (super.find(path) != null) {
                return super.find(path);
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            synchronized (this) {
                if (!path.contains("/")) {
                    for (ApexPath p : contents) {
                        if (p.name.equals(path)) {
                            return p;
                        }
                    }
                    return null;
                }
                String nextName = path.substring(0, path.indexOf("/"));
                String rest = path.substring(path.indexOf("/"));
                for (ApexPath p : contents) {
                    if (p.name.equals(nextName)) {
                        return p.find(rest);
                    }
                }
            }
            return null;
        }

        @Override
        public void getattr(FileStat stat){
            // Don't have to increase the usage factor here?
            stat.st_mode.set(FileStat.S_IFDIR | 0777);
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
        }

        synchronized void mkdir(String lastComponent) {
            contents.add(new ApexDir(lastComponent, this));
        }

        synchronized void mkfile(String lastComponent){
            //incerase usage & hist factor of the file in constructor
            contents.add(new ApexFile(lastComponent, this));
        }

        synchronized void read(Pointer buf, FuseFillDir filler){
            for(ApexPath p : contents){
                // didn't understand fully but may be used to display debug information
                filler.apply(buf, p.name, null, 0);
            }
        }

        // Delete method was already in the abstract class,
        // could be used to change factors or deal with indivisual blocks etc
    }

    enum STATE{
        USED,  //Currently allocated blocks
        DELETED, // File deleted and blocks can be overwritten
        OBSOLETE; // No blocks allocated, file ignored
    }

    // this a file
    class ApexFile extends ApexPath{
        public

        final int lf_max = 10;
        final int lf_min = 0;
        int original_size;// requrired in prev code, don't know the exact pourpose
        int uf = 0;
        STATE fileState;
        //only 0 or 10
        int linking_factor;
        double slm;
        ArrayList<Block> blocklist;//manage this list properly

        //Constructors and methods
        ApexFile(String name){ super(name); }
        ApexFile(String name, ApexDir parent) { super(name, parent); }
        ApexFile(String name, ApexDir parent, Block b, int linking_factor) {
            super(name, parent);
            assert(b.used == false);
            b.allocate(this, linking_factor);
            this.blocklist.add(b);
            this.fileState = STATE.USED;
            this.linking_factor = linking_factor;
//            this.computeSlm();
            this.uf = 1;
            this.original_size = 1;

            //Todo : Write FIILENAME ON THIS 1 BLOCK
            b.write(this.name + " " + this.parent.name);
            // increase block's usage factor
//            ApexFS.memory.putChar()
        }
//        ApexFile(String name, String text) {
//            super(name);
//            // todo SPLIT THE TEXT INTO BYTES/ CHUNKS and request and store them in the
//            //  'unused' blocks from the heap
//            // save into bytes by the Pointer/Memory class
//            try {
//                byte[] contentBytes = text.getBytes("UTF-8");
//                contents = ByteBuffer.wrap(contentBytes);
//            } catch (UnsupportedEncodingException e) {
//                // Not going to happen
//            }
//        }

        void deleteFile(){
            if(!(this.fileState == STATE.USED)){
                System.out.println("Trying to Delete an existing or obsolete file!");
                exit();
            }

            this.fileState = STATE.DELETED;
            //deallocate file
            for (Block b : this.blocklist){
                //deallocate not delete because, deleteBlock if final delete
                // doesn't change parent file pointer of this block
                b.setUnused();
            }
        }

        void deleteBlock(Block b){
            assert this.blocklist.contains(b);

            this.blocklist.remove(b);
            // increasing HF of all the other blocks ?
            for (Block block: this.blocklist){
                block.increaseHF();
            }

            if (this.blocklist.size() == 0){
                this.fileState = STATE.OBSOLETE;
            }
        }
        // IDK yet what exactly is this doing ..
        @Override
        protected void getattr(FileStat stat) {
            // todo increase usage, history factor etc if required
            stat.st_mode.set(FileStat.S_IFREG | 0777);

            // size might be the Total number of bytes and that can be taken from number of chunks into size of each chunk
            stat.st_size.set(memSize);
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
        }

        private int read(Pointer buffer, long size, long offset) {
            // change factors of the blocks and the sourrounding blocks too ?
            // ONLY INCREASE USAGE FACTOR OF THE FILE
            long capacity = this.blocklist.size() * CHUNK_SIZE;
            int bytesToRead = (int) Math.min(capacity - offset, size);
//            byte[] bytesRead = new byte[bytesToRead];
            int maxIdx = (int)size/CHUNK_SIZE;
            int rem = (int)size - maxIdx*CHUNK_SIZE;

            synchronized (this) {
//                contents.position((int) offset);
//                contents.get(bytesRead, 0, bytesToRead);
//                buffer.put(0, bytesRead, 0, bytesToRead);
//                contents.position(0); // Rewind
                int i = 0;
                for(i = 0; (i < this.blocklist.size()) && (i < maxIdx); i++){
                    Block b_ = this.blocklist.get(i);
                    byte[] nb = b_.read();
//                    b_.increaseUF();
                    buffer.put(offset+i*CHUNK_SIZE, nb, 0, CHUNK_SIZE);
                }

                //Getting the first 'rem' bytes of next block
                if (rem > 0){
                    Block b_ = this.blocklist.get(i);
                    byte[] nb = b_.read();
//                    b_.increaseUF();
                    //get first rem bytes of nb
                    byte[] nbf = new byte[rem];
                    for(int k = 0; k < rem; k++) {
                        nbf[k] = nb[k];
                    }
                    buffer.put(offset+i*CHUNK_SIZE, nbf, 0, rem);
                }

            }

            //Don't increase usage factor of all blocks just the ones which were read
            // Increase USAGE FACTOR OF ALL BLOCKS BECAUSE file level concept
            for (Block block: this.blocklist) {
                block.increaseUF();
            }

            return bytesToRead;
        }

        // IDK about it's workings but we need to put the contents in/out the buffer as required
        synchronized void truncate(long size) {
            //TRUNCATE THE file to the requird size

            // todo to change the factors here too

            // 10 orig, trunc to 20, inc usage factor factor of all 10
            // 10 orig, trunc to 5, inc usage factor of first 5 and inc HF of last 5 (5 times, 1 each time any  block is deleted), and deallocate them

            long numBlocks = (size/CHUNK_SIZE);
            int rem = (int)(size - numBlocks*CHUNK_SIZE);
            //ceiling of size/chunk_size
            long num = rem == 0 ? numBlocks : numBlocks + 1 ;

            if (num >= this.blocklist.size()){
                for(Block b: this.blocklist){
                    b.increaseUF();
                }
            }
            else{
                int i = 0;
                for(i = 0; i < num; i++){
                    Block b_ = this.blocklist.get(i);
                    b_.increaseUF();
                }
                for( ; i < this.blocklist.size(); i++){
                    Block b_ = this.blocklist.get(i);
                    this.deleteBlock(b_);
//                    b_.setUnused();
                }
                // Done: CALL THE DEALLOCATE BLOCK FUNCTION IN FILE BUT ThAT' does many things

            }

            // Write this case ?
            if (size == 0){

            }
//            if (rem == 0){
//                if (numBlocks < this.blocklist.size()){
//
//                }
//            }
//            else{ // non zero remainder
//
//            }

//            if (size < contents.capacity()) {
//                // Need to create a new, smaller buffer
//                ByteBuffer newContents = ByteBuffer.allocate((int) size);
//                byte[] bytesRead = new byte[(int) size];
//                contents.get(bytesRead);
//                newContents.put(bytesRead);
//                contents = newContents;
//            }
        }

        int write(Pointer buffer, long bufSize, long writeOffset) {
            int maxWriteIndex = (int) (writeOffset + bufSize);

            synchronized (this) {
                long numBlocks = bufSize/CHUNK_SIZE;
                int rem = (int)(bufSize - numBlocks * CHUNK_SIZE);
                long i;
                for(i = 0 ; i < numBlocks; i++){
                    //get a block from unused blocks
                    Block b;
                    try{
                        b = ApexFS.unusedBlocks.poll();
                    }
                    catch (Exception e){
                        System.out.println("Memory full, no more unused blocks");
                        return -1;
                    }
                    byte[] bytesToWrite = new byte[(int) CHUNK_SIZE];
                    buffer.get((i*CHUNK_SIZE), bytesToWrite, 0, CHUNK_SIZE);
                    b.write(bytesToWrite);
//                    for(int j = 0; j < CHUNK_SIZE; j++){
//                        b.write();
//                    }
                    this.blocklist.add(b);
                    b.allocate(this, this.linking_factor);
                }
                if(rem>0){
                    Block b;
                    try{
                        b = ApexFS.unusedBlocks.poll();
                    }
                    catch (Exception e){
                        System.out.println("Memory full, no more unused blocks");
                        return -1;
                    }
                    byte[] bytesToWrite = new byte[rem];
                    buffer.get((i*CHUNK_SIZE), bytesToWrite, 0, rem);
                    b.write(bytesToWrite);
                    this.blocklist.add(b);
                    b.allocate(this, this.linking_factor);
                }
//                if (maxWriteIndex > contents.capacity()) {
//                    // Need to create a new, larger buffer
//                    ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
//                    newContents.put(contents);
//                    contents = newContents;
//                }
//                buffer.get(0, bytesToWrite, 0, (int) bufSize);
//                contents.position((int) writeOffset);
//                contents.put(bytesToWrite);
//                contents.position(0); // Rewind
            }

            for (Block block: this.blocklist) {
                block.increaseUF();
            }
            return (int) bufSize;
        }

        double getRecoveryRatio(){
            assert fileState == STATE.DELETED;

            //current size
            int cs = this.blocklist.size();
            if(cs == 0)
                return 0;

            double rr = 0;
            switch (linking_factor){
                case 0: rr = (double)(cs)/((double)(this.original_size)); break;
                case 10: rr = (cs < this.original_size) ? 0: 1 ;
            }

            this.uf = 0;
            for (Block b: this.blocklist){
                this.uf += b.uf;
                break;
            }
            this.uf = this.uf / this.blocklist.size();
            return  rr;
        }

    }

//    Memory a = Memory(5);

    // MEMBERS OF THE FILESYSTEM ITSELF
    //
    // this a file
    static int CHUNK_SIZE = 1; //size of the chunk in number of bytes

    static ApexDir rootDir ;
    static int MAX_PARAM = 9;
    static int MIN_PARAM = 0;

    static int memSize;
    static double mem_util;
    static int lambda, sigma, rho, mu;
    static ArrayList<Block> blocks;
    // Block heap
    static PriorityQueue<Block> unusedBlocks;
    static HashSet<Block> usedBlocks;
//    PriorityQueue<Block> usedBlocks;
    //List of all files
    static ArrayList<ApexFile> currentFileList;
    static ArrayList<ApexFile> deletedFileList;
//    ArrayList<Pair<Integer, Integer>> directions = new ArrayList<Pair<Integer, Integer>>(8);
    static int totalCreatedFiles;

    static ByteBuffer memory;

    public void init(int mem_size, int cs){
        memSize = mem_size;
        memory = ByteBuffer.allocate(mem_size);
        mem_util = 0.0;
        CHUNK_SIZE = cs;

        lambda = 1;
        sigma = 1;
        rho = 1;
        mu = 1;

        int numBlocks = mem_size/CHUNK_SIZE;// hopint it to be integers
        blocks = new ArrayList<>(numBlocks);
        //initialize new blocks etc
        for(int i = 0; i < numBlocks; i++){
            Block b = new Block(i, CHUNK_SIZE);
            blocks.add(b);
        }
        // offset of each block is index*CHUNK SIZE;

        currentFileList = new ArrayList<>();
        deletedFileList = new ArrayList<>();

        usedBlocks = new HashSet<>(numBlocks);
        unusedBlocks = new PriorityQueue<>(numBlocks, new BlockComparator());

        // We could have done this in 1 loop above, but more structure this way
        for(int i = 0; i < numBlocks; i++){
            Block b = blocks.get(i);
            unusedBlocks.add(b);
        }

        totalCreatedFiles = 0;
    }

    //constructor
    ApexFS(){
        // make some new files and diretories
        rootDir = new ApexDir("");
        init(1024, 1);
//        rootDirectory.add(new MemoryFile("Sample file.txt", "Hello there, feel free to look around.\n"));
//        rootDirectory.add(new MemoryDirectory("Sample directory"));
//        MemoryDirectory dirWithFiles = new MemoryDirectory("Directory with files");
//        rootDirectory.add(dirWithFiles);
//        dirWithFiles.add(new MemoryFile("hello.txt", "This is some sample text.\n"));
//        dirWithFiles.add(new MemoryFile("hello again.txt", "This another file with text in it! Oh my!\n"));
//        MemoryDirectory nestedDirectory = new MemoryDirectory("Sample nested directory");
//        dirWithFiles.add(nestedDirectory);
//        nestedDirectory.add(new MemoryFile("So deep.txt", "Man, I'm like, so deep in this here file structure.\n"));

    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        if (getPath(path) != null) {
            return -ErrorCodes.EEXIST();
        }
        ApexPath parent = getParentPath(path);

        if (parent instanceof ApexDir) {
            Block b = this.unusedBlocks.poll();

            //Compute lf(linking_factor) by fileinfo fi
            int lf = 0;
            ApexFile af = new ApexFile(path, (ApexDir) parent, b, lf);

//            //num blocks is calculated by the text, but at time of creation there is no text
//            HashSet<Block> block_list = new HashSet<>(num_blocks);
//
//            for (int i = 0; i < num_blocks; i++) {
//                Block b = this.unusedBlocks.poll();
//                if (b == null) {
//                    throw new java.lang.RuntimeException("Memory Full!");
//                }
//                block_list.add(b);
//                this.usedBlocks.add(b);
//            }
//
//            //if enough blocks
//            File f = new File(block_list, link_factor);
            this.currentFileList.add(af);
            this.totalCreatedFiles++;
//            //Refresh the memory pf, sf etc;
            this.refresh();

            //This just adds, file to the directory
            ((ApexDir) parent).mkfile(getLastComponent(path));
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int getattr(String path, FileStat stat) {
        ApexPath p = getPath(path);
        if (p != null) {
            p.getattr(stat);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    String getLastComponent(String path) {
        while (path.substring(path.length() - 1).equals("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "";
        }
        return path.substring(path.lastIndexOf("/") + 1);
    }

    ApexPath getParentPath(String path) {
        return rootDir.find(path.substring(0, path.lastIndexOf("/")));
    }

    ApexPath getPath(String path) {
        return rootDir.find(path);
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
        if (getPath(path) != null) {
            return -ErrorCodes.EEXIST();
        }
        ApexPath parent = getParentPath(path);
        if (parent instanceof ApexDir) {
            ((ApexDir) parent).mkdir(getLastComponent(path));
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof ApexFile)) {
            return -ErrorCodes.EISDIR();
        }
        // we read the file here and change all the factors, of the blocks inside the APex File's read function
        return ((ApexFile) p).read(buf, size, offset);
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof ApexDir)) {
            return -ErrorCodes.ENOTDIR();
        }
        filter.apply(buf, ".", null, 0);
        filter.apply(buf, "..", null, 0);
        //Don't have to change the factors here
        ((ApexDir) p).read(buf, filter);
        return 0;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        // todo : understand about this function and then proceed
        if (Platform.getNativePlatform().getOS() == WINDOWS) {
            // statfs needs to be implemented on Windows in order to allow for copying
            // data from other devices because winfsp calculates the volume size based
            // on the statvfs call.
            // see https://github.com/billziss-gh/winfsp/blob/14e6b402fe3360fdebcc78868de8df27622b565f/src/dll/fuse/fuse_intf.c#L654
            if ("/".equals(path)) {
                stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
                stbuf.f_frsize.set(1024);        // fs block size
                stbuf.f_bfree.set(1024 * 1024);  // free blocks in fs
            }
        }
        return super.statfs(path, stbuf);
    }

    @Override
    public int rename(String path, String newName) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        ApexPath newParent = getParentPath(newName);
        if (newParent == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(newParent instanceof ApexDir)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        p.rename(newName.substring(newName.lastIndexOf("/")));
        ((ApexDir) newParent).add(p);
        // We don't have to change the factors here, do we? todo: ask shreshth
        return 0;
    }

    @Override
    public int rmdir(String path) {

        // todo : properly implement this function , with changing the factors,
        // check where exactly, a FILE's delete function is called

        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof ApexDir)) {
            return -ErrorCodes.ENOTDIR();
        }
        p.delete();
        return 0;
    }

    @Override
    public int truncate(String path, long offset) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof ApexFile)) {
            return -ErrorCodes.EISDIR();
        }
        // Handles everthing inside
        ((ApexFile) p).truncate(offset);
        return 0;
    }

    @Override
    public int unlink(String path) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        // todo : check pourpose of this method ? Maybe not used in MemfS, but elsewhere?
        // WHY DIDN'T WE OPEN THE FILE ETC HERE? COPY INTO BUFFER ?
        return 0;
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        ApexPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof ApexFile)) {
            return -ErrorCodes.EISDIR();
        }
        // todo: check this but , mostly alloc/ dealloc is taken care of inside this
        return ((ApexFile) p).write(buf, size, offset);
    }
    // auxillary functions

    void updateSF() {
        // neighbouring blocks's pf's avg in sf of this block

        int count = 0;
        double sum = 0.0;

        int newi = 0;
        int newj = 0;

        //todo : linearize below function
//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//
//                sum = 0.0;
//                count = 0;
//
//                for (int t = 0; t < directions.size(); t++) {
//                    newi = i + directions.get(t).getKey();
//                    newj = i + directions.get(t).getValue();
//
//                    if (newi >= 0 && newi < width && newj >= 0 && newj < height) {
//                        sum += blocks[newi][newj].pf;
//                        count++;
//                    }
//                }
//                blocks[j][i].setSF((int) (sum / count));
//
//            }
//        }

    }

    void printMemory() {

        //todo : proper linearization
        for(int i = 0; i < blocks.size(); i++){
            Block b = blocks.get(i);
                if (!b.used) {
                    System.out.print("(-,");
                }
                else{
                    System.out.print("(" + currentFileList.indexOf(b.parentFile) + ",");
                }
                System.out.print(b.hf + "," + b.uf + "," + b.sf + "," + b.lf + ":" + b.pf + ")\t");
        }
//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                Block b = blocks[i][j];
//                if (!b.used) {
//                    System.out.print("(-,");
//                }
//                else{
//                    System.out.print("(" + currentFileList.indexOf(b.parentFile) + ",");
//                }
//                System.out.print(b.hf + "," + b.uf + "," + b.sf + "," + b.lf + ":" + b.pf + ")\t");
//            }
//            System.out.println();
//        }

        System.out.println("Number of current files : " + currentFileList.size());
        System.out.println("Number of deleted files : " + deletedFileList.size());

        int obsolete = 0;
        for(ApexFile file : deletedFileList){
            if (file.fileState == STATE.OBSOLETE){
                obsolete+=1;
            }
        }

        System.out.println("Number of obselete files out of deleted files : " + obsolete);
        System.out.println("Memory Utilization : " + mem_util);
        System.out.println("Recovery ratios of the deleted (not obsolete) files : " + (deletedFileList.size() - obsolete));
        double sumrr = 0;
        for(int i = 0; i < deletedFileList.size(); i++){
            System.out.print(deletedFileList.get(i).getRecoveryRatio() + ", ");
            sumrr += deletedFileList.get(i).getRecoveryRatio();
        }
        System.out.println("\nSum of recovery ratios : " + sumrr);
    }

    void refresh() {
        updateSF();

        int numBlocks = memSize/CHUNK_SIZE;
        PriorityQueue<Block> newHeap = new PriorityQueue<>(numBlocks, new BlockComparator());

//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                Block b = blocks[i][j];
//                b.updatepf(lambda, sigma, rho, mu);
//                if (b.used == false) {
//                    newHeap.add(blocks[i][j]);
//                }
//            }
//        }
        for (int i = 0; i < blocks.size(); i++){
            Block b = blocks.get(i);
            b.updatepf(lambda, sigma, rho , mu);
            if (b.used == false){
                newHeap.add(b);
            }
        }
        unusedBlocks = newHeap;

        mem_util = ((double) (usedBlocks.size())) * 100 / (numBlocks);

    }

    void updateParams(int lambda, int sigma, int rho, int mu) {
        assert (lambda <= MAX_PARAM) && (lambda >= MIN_PARAM);
        assert (sigma <= MAX_PARAM) && (sigma >= MIN_PARAM);
        assert (rho <= MAX_PARAM) && (rho >= MIN_PARAM);
        assert (mu <= MAX_PARAM) && (mu >= MIN_PARAM);

        lambda = lambda;
        sigma = sigma;
        rho = rho;
        mu = mu;

    }

    // Memory Usage percentage
    double memUsage() {
        return mem_util;
    }

    public static void main(String[] args) {
        ApexFS fs = new ApexFS();
        try {
            String path;
            switch (Platform.getNativePlatform().getOS()) {
                case WINDOWS:
                    path = "J:\\";
                    break;
                default:
                    path = "./mountdir";
            }
            fs.mount(Paths.get(path), true, true);
        } finally {
            fs.umount();
        }
    }

}

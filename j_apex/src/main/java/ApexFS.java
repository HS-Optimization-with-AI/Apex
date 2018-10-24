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
    final int CHUNK_SIZE = 1; //size of the chunk in number of bytes

    class ApexFile extends ApexPath{
        public

        final int lf_max = 10;
        final int lf_min = 0;
        int original_size;// requrired in prev code, don't know the exact pourpose
        // Block list
        HashSet<Block> blockList ;//= new ArrayList<>();
        int uf = 0;
        STATE fileState;
        //only 0 or 10
        int linking_factor;
        double slm;
        ArrayList<Block> blocklist;//manage this list properly

        //Constructors and methods
        ApexFile(String name){ super(name); }
        private ApexFile(String name, ApexDir parent) { super(name, parent); }
        ApexFile(String name, String text) {
            super(name);
            // todo SPLIT THE TEXT INTO BYTES/ CHUNKS and request and store them in the
            //  'unused' blocks from the heap
            // save into bytes by the Pointer/Memory class
            try {
                byte[] contentBytes = text.getBytes("UTF-8");
                contents = ByteBuffer.wrap(contentBytes);
            } catch (UnsupportedEncodingException e) {
                // Not going to happen
            }
        }

        // IDK yet what exactly is this doing ..
        @Override
        protected void getattr(FileStat stat) {
            // todo increase usage, history factor etc if required
            stat.st_mode.set(FileStat.S_IFREG | 0777);

            // size might be the Total number of bytes and that can be taken from number of chunks into size of each chunk
            stat.st_size.set(contents.capacity());
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
        }

        private int read(Pointer buffer, long size, long offset) {
            // change factors of the blocks and the sourrounding blocks too ?

            int bytesToRead = (int) Math.min(contents.capacity() - offset, size);
            byte[] bytesRead = new byte[bytesToRead];
            synchronized (this) {
                contents.position((int) offset);
                contents.get(bytesRead, 0, bytesToRead);
                buffer.put(0, bytesRead, 0, bytesToRead);
                contents.position(0); // Rewind
            }
            return bytesToRead;
        }

        // I'm not exactly sure about it's workings but we need to put the contents in/out the buffer as required
        synchronized void truncate(long size) {
            // todo to change the factors here too
            if (size < contents.capacity()) {
                // Need to create a new, smaller buffer
                ByteBuffer newContents = ByteBuffer.allocate((int) size);
                byte[] bytesRead = new byte[(int) size];
                contents.get(bytesRead);
                newContents.put(bytesRead);
                contents = newContents;
            }
        }

        int write(Pointer buffer, long bufSize, long writeOffset) {
            //todo change the factors, split and write properly into bytes by the Pointer/Memory class..
            int maxWriteIndex = (int) (writeOffset + bufSize);
            byte[] bytesToWrite = new byte[(int) bufSize];
            synchronized (this) {
                if (maxWriteIndex > contents.capacity()) {
                    // Need to create a new, larger buffer
                    ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
                    newContents.put(contents);
                    contents = newContents;
                }
                buffer.get(0, bytesToWrite, 0, (int) bufSize);
                contents.position((int) writeOffset);
                contents.put(bytesToWrite);
                contents.position(0); // Rewind
            }
            return (int) bufSize;
        }

    }

//    Memory a = Memory(5);

    // MEMBERS OF THE FILESYSTEM ITSELF
    //

    ApexDir rootDir = new ApexDir("");
    static int MAX_PARAM = 9;
    static int MIN_PARAM = 0;

    int memSize;
    double mem_util;
    int lambda, sigma, rho, mu;
    ArrayList<Block> blocks;
    // Block heap
    PriorityQueue<Block> unusedBlocks;
    HashSet<Block> usedBlocks;
//    PriorityQueue<Block> usedBlocks;
    //List of all files
    ArrayList<ApexFile> currentFileList;
    ArrayList<ApexFile> deletedFileList;
    ArrayList<Pair<Integer, Integer>> directions = new ArrayList<Pair<Integer, Integer>>(8);
    int totalCreatedFiles;
    //constructor
    ApexFS(){
        // make some new files and diretories
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        if (getPath(path) != null) {
            return -ErrorCodes.EEXIST();
        }
        ApexPath parent = getParentPath(path);

        if (parent instanceof ApexDir) {
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
//            this.currentFileList.add(f);
//            this.totalCreatedFiles++;
//            //Refresh the memory pf, sf etc;
//            this.refresh();

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
        ((ApexDir) p).read(buf, filter);
        return 0;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
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
        return 0;
    }

    @Override
    public int rmdir(String path) {
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
        return ((ApexFile) p).write(buf, size, offset);
    }
}

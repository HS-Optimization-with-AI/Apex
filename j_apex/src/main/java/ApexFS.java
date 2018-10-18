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

import static jnr.ffi.Platform.OS.LINUX;

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


    // this a file
    class ApexFile extends ApexPath{
        public

        final int CHUNK_SIZE = 1; //size of the chunk in number of bytes

        final int lf_max = 10;
        final int lf_min = 0;

        int original_size;// requrired in prev code, don't know the exact pourpose

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

        ArrayList<Block> blocks;//manage this list properly


        //Constructors and methods
        ApexFile(String name){ super(name); }

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

//    ApexBlock class is the Block class in the same folder
//    class ApexBlock{
//        int offset;
//
//    }
//    Memory a = Memory(5);

    // MEMBERS OF THE FILESYSTEM ITSELF
    //



}

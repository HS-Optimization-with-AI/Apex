import jnr.ffi.Platform;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
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
import java.util.List;

import static jnr.ffi.Platform.OS.LINUX;

public class Apex extends FuseStubFS {

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
}

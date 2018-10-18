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

    class ApexDir{

    }
}

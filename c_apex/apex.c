
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <fuse.h>
#include <libgen.h>
#include <limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#ifdef HAVE_SYS_XATTR_H
#include <sys/xattr.h>
#endif

#include "config.h"
#include "apexparams.h"
#include "apexopt.h"
#include "log.h"


struct fuse_operations apex_oper = {
    // Some of them will be deleted

  .getattr = apex_getattr,
  .readlink = apex_readlink,
  // no .getdir -- that's deprecated
  .getdir = NULL,
  .mknod = apex_mknod,
  .mkdir = apex_mkdir,
  .unlink = apex_unlink,
  .rmdir = apex_rmdir,
  .symlink = apex_symlink,
  .rename = apex_rename,
  .link = apex_link,
  .chmod = apex_chmod,
  .chown = apex_chown,
  .truncate = apex_truncate,
  .utime = apex_utime,
  .open = apex_open,
  .read = apex_read,
  .write = apex_write,
  /** Just a placeholder, don't set */ // huh???
  .statfs = apex_statfs,
  .flush = apex_flush,
  .release = apex_release,
  .fsync = apex_fsync,
  
#ifdef HAVE_SYS_XATTR_H
  .setxattr = apex_setxattr,
  .getxattr = apex_getxattr,
  .listxattr = apex_listxattr,
  .removexattr = apex_removexattr,
#endif
  
  .opendir = apex_opendir,
  .readdir = apex_readdir,
  .releasedir = apex_releasedir,
  .fsyncdir = apex_fsyncdir,
  .init = apex_init,
  .destroy = apex_destroy,
  .access = apex_access,
  .ftruncate = apex_ftruncate,
  .fgetattr = apex_fgetattr
};

void apex_usage()
{
    fprintf(stderr, "usage:  apex [FUSE and mount options] rootDir mountPoint\n");
    abort();
}

int main(int argc, char *argv[])
{
    int fuse_stat;
    struct apex_state *apex_data;

    // apex doesn't do any access checking on its own (the comment
    // blocks in fuse.h mention some of the functions that need
    // accesses checked -- but note there are other functions, like
    // chown(), that also need checking!).  Since running apex as root
    // will therefore open Metrodome-sized holes in the system
    // security, we'll check if root is trying to mount the filesystem
    // and refuse if it is.  The somewhat smaller hole of an ordinary
    // user doing it with the allow_other flag is still there because
    // I don't want to parse the options string.
    if ((getuid() == 0) || (geteuid() == 0)) {
    	fprintf(stderr, "Running apex as root opens unnacceptable security holes\n");
    	return 1;
    }

    // See which version of fuse we're running
    fprintf(stderr, "Fuse library version %d.%d\n", FUSE_MAJOR_VERSION, FUSE_MINOR_VERSION);
    
    // Perform some sanity checking on the command line:  make sure
    // there are enough arguments, and that neither of the last two
    // start with a hyphen (this will break if you actually have a
    // rootpoint or mountpoint whose name starts with a hyphen, but so
    // will a zillion other programs)
    if ((argc < 3) || (argv[argc-2][0] == '-') || (argv[argc-1][0] == '-'))
	apex_usage();

    apex_data = malloc(sizeof(struct apex_state));
    if (apex_data == NULL) {
        perror("main calloc");
        abort();
    }

    // Pull the rootdir out of the argument list and save it in my
    // internal data
    apex_data->rootdir = realpath(argv[argc-2], NULL);
    argv[argc-2] = argv[argc-1];
    argv[argc-1] = NULL;
    argc--;
    
    // apex_data->logfile = log_open();
    
    //initialize all the hashmaps, lists etc and such
    init_data(apex);


    // turn over control to fuse
    fprintf(stderr, "about to call fuse_main\n");
    fuse_stat = fuse_main(argc, argv, &apex_oper, apex_data);
    fprintf(stderr, "fuse_main returned %d\n", fuse_stat);
    
    return fuse_stat;
}

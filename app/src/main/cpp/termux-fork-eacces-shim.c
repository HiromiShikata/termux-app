#define _GNU_SOURCE
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>

static const char UPSTREAM_PREFIX[] = "/data/data/com.termux/files/";
#define UPSTREAM_PREFIX_LEN (sizeof(UPSTREAM_PREFIX) - 1)

static int path_is_under_upstream_prefix(const char *path) {
    if (path == NULL) return 0;
    return strncmp(path, UPSTREAM_PREFIX, UPSTREAM_PREFIX_LEN) == 0;
}

static int translate_eacces_to_enoent_if_upstream(const char *path, int saved_errno) {
    if (saved_errno == EACCES && path_is_under_upstream_prefix(path)) return ENOENT;
    return saved_errno;
}

typedef int (*scandir_fn)(const char *, struct dirent ***,
                          int (*)(const struct dirent *),
                          int (*)(const struct dirent **, const struct dirent **));

int scandir(const char *dirp, struct dirent ***namelist,
            int (*filter)(const struct dirent *),
            int (*compar)(const struct dirent **, const struct dirent **)) {
    static scandir_fn real_scandir = NULL;
    if (real_scandir == NULL) real_scandir = (scandir_fn) dlsym(RTLD_NEXT, "scandir");
    if (real_scandir == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_scandir(dirp, namelist, filter, compar);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(dirp, errno);
    return result;
}

typedef DIR *(*opendir_fn)(const char *);

DIR *opendir(const char *name) {
    static opendir_fn real_opendir = NULL;
    if (real_opendir == NULL) real_opendir = (opendir_fn) dlsym(RTLD_NEXT, "opendir");
    if (real_opendir == NULL) {
        errno = ENOSYS;
        return NULL;
    }
    DIR *result = real_opendir(name);
    if (result == NULL) errno = translate_eacces_to_enoent_if_upstream(name, errno);
    return result;
}

#define _GNU_SOURCE
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

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

typedef int (*open_fn)(const char *, int, ...);

int open(const char *pathname, int flags, ...) {
    static open_fn real_open = NULL;
    if (real_open == NULL) real_open = (open_fn) dlsym(RTLD_NEXT, "open");
    if (real_open == NULL) {
        errno = ENOSYS;
        return -1;
    }
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0 || (flags & O_TMPFILE) == O_TMPFILE) {
        va_list ap;
        va_start(ap, flags);
        mode = (mode_t) va_arg(ap, int);
        va_end(ap);
    }
    int result = real_open(pathname, flags, mode);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*openat_fn)(int, const char *, int, ...);

int openat(int dirfd, const char *pathname, int flags, ...) {
    static openat_fn real_openat = NULL;
    if (real_openat == NULL) real_openat = (openat_fn) dlsym(RTLD_NEXT, "openat");
    if (real_openat == NULL) {
        errno = ENOSYS;
        return -1;
    }
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0 || (flags & O_TMPFILE) == O_TMPFILE) {
        va_list ap;
        va_start(ap, flags);
        mode = (mode_t) va_arg(ap, int);
        va_end(ap);
    }
    int result = real_openat(dirfd, pathname, flags, mode);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*access_fn)(const char *, int);

int access(const char *pathname, int mode) {
    static access_fn real_access = NULL;
    if (real_access == NULL) real_access = (access_fn) dlsym(RTLD_NEXT, "access");
    if (real_access == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_access(pathname, mode);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*faccessat_fn)(int, const char *, int, int);

int faccessat(int dirfd, const char *pathname, int mode, int flags) {
    static faccessat_fn real_faccessat = NULL;
    if (real_faccessat == NULL) real_faccessat = (faccessat_fn) dlsym(RTLD_NEXT, "faccessat");
    if (real_faccessat == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_faccessat(dirfd, pathname, mode, flags);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*stat_fn)(const char *, struct stat *);

int stat(const char *pathname, struct stat *statbuf) {
    static stat_fn real_stat = NULL;
    if (real_stat == NULL) real_stat = (stat_fn) dlsym(RTLD_NEXT, "stat");
    if (real_stat == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_stat(pathname, statbuf);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*lstat_fn)(const char *, struct stat *);

int lstat(const char *pathname, struct stat *statbuf) {
    static lstat_fn real_lstat = NULL;
    if (real_lstat == NULL) real_lstat = (lstat_fn) dlsym(RTLD_NEXT, "lstat");
    if (real_lstat == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_lstat(pathname, statbuf);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

typedef int (*fstatat_fn)(int, const char *, struct stat *, int);

int fstatat(int dirfd, const char *pathname, struct stat *statbuf, int flags) {
    static fstatat_fn real_fstatat = NULL;
    if (real_fstatat == NULL) real_fstatat = (fstatat_fn) dlsym(RTLD_NEXT, "fstatat");
    if (real_fstatat == NULL) {
        errno = ENOSYS;
        return -1;
    }
    int result = real_fstatat(dirfd, pathname, statbuf, flags);
    if (result < 0) errno = translate_eacces_to_enoent_if_upstream(pathname, errno);
    return result;
}

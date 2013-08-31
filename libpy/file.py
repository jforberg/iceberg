#!/usr/bin/python3
# -*- coding: utf-8 -*-

import os
import os.path as path
from functools import reduce

HASH_ALGORITHM = 'sha1'

class Root(Dir):
    def __init__(self, root_path=None):
        self.root_path = root_path
        self.root = self
        self.name = '.'
        self.dirs = []
        self.files = []
        self.hashtbl = {}

    def build(self, filter_func=lambda x: x)
        os.chdir(root_path)
        for roots, dirs, files in filter_func(os.walk('.')):


    
    def by_path(self, name):
        pth = full_split(name)
        if pth[0] == '.':
            if len(pth) == 1:
                return self
            else:
                pth = pth[1:]
        res = _by_path(pth)
        if not res:
            raise FileError('%s: no such file' % name)

class Dir:
    def __init__(self, name):
        self.name = name
        self.root = root
        self.dirs = []
        self.files = []

    def _by_path(self, pth):
        if not pth:
            return self
        for d in dirs:
            if d.name == pth[0]:
                return d._by_path(pth[1:])
        for f in files:
            if f.name == pth[0]:
                return f._by_path(pth[1:])
        # Nothing found
        return None

class File:
    def __init__(self, fullname):
        self.name = path.basename(fullname)
        self.fullname = fullname
        self.root = root
        if not self.in_fs():
            raise FileError('%s is not a file or is a symlink' % filename)
        # Pass any OSError upwards.
        st = os.stat(filename)
        (self.mode, self.size, self.mtime) = 
                    (st.st_mode, st.st_size, st.st_mtime)
        self._hashval = None

    def in_fs(self):
        return path.isfile(self.name) and not path.islink(self.name)

    def _by_path(self, pth):
        if not pth:
            return self
        else:
            return None

    def hashval(self):
        if self._hashval:
            return self._hashval

class FileError(OSError):
    '''General error relating to files'''
    pass

def hash_file(self, fullname):
    '''Return hash of a file in filesystem.'''
    hash_obj = getattr(hashlib, HASH_ALGORITHM)()
    with open(fullname, 'rb') as f:
        for chunk in iter(lambda: f.read(128 * hash_obj.block_size), b''):
            hash_obj.update(chunk)
    return hash_obj.digest()

def full_split(pth):
    lst = []
    while pth:
        (pth, tail) = path.split(pth)
        if tail:
            lst.append(tail)
    return lst

def full_join(lst):
    return reduce(path.join, lst)
    


    


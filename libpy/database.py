#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sqlite3
import io

# Sqlite Schema

SCHEMA = '''
create table directories(name varchar(255), 
                         parent_id integer, 
                         foreign key(parent_id) references directory(id));
create table 


class Database:
    def __init__(self, filename):
        self.conn = sqlite3.connect(filename)
        
    def by_hash(hsh):
        

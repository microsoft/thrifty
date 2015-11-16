package com.bendb.thrifty;

import java.util.ArrayList;
import java.util.List;

class Linker {
    private final List<Program> programs;

    Linker(List<Program> programs) {
        this.programs = new ArrayList<>(programs);
    }

    public void link() {
        for (Program program : programs) {

        }
    }

    ThriftType resolveType(String type) {

    }
}

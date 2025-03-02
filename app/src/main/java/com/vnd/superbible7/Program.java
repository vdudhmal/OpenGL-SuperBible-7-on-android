package com.vnd.superbible7;

import java.util.ArrayList;
import java.util.List;

public class Program {
    private final String name;
    private final String className;
    private final List<Program> children;

    public Program(String name, String className) {
        this.name = name;
        this.className = className;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public List<Program> getChildren() {
        return children;
    }

    public void addChild(Program child) {
        children.add(child);
    }
}

package org.dimer.compiler.data;

public record Variable(String name, String type, Object value, Integer index) {

    public Variable(String name, String type) {
        this(name, type, null, null);
    }

    public Variable(String name, String type, Object value) {
        this(name, type, value, null);
    }
}

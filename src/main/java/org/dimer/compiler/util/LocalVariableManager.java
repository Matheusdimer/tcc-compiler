package org.dimer.compiler.util;

import org.dimer.compiler.data.Variable;

import java.util.HashMap;
import java.util.Map;

public class LocalVariableManager {
    private final Map<String, Variable> variablesByName = new HashMap<>();

    private int nextIndex = 1;

    public int allocate(Variable variable) {
        int index = nextIndex++;
        variable = new Variable(variable.name(), variable.type(), null, index);
        variablesByName.put(variable.name(), variable);
        return index;
    }

    public Variable load(String name) {
        return variablesByName.get(name);
    }
}

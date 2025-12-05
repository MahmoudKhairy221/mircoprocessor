package com.example.tomasulo.components;

import com.example.tomasulo.utils.Constants;
import com.example.tomasulo.utils.RegisterType;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterFile {
    private Map<String, Double> integerRegisters;
    private Map<String, Double> fpRegisters;
    private Map<String, String> integerTags; // Tag tracking for Tomasulo
    private Map<String, String> fpTags;
    
    public RegisterFile() {
        integerRegisters = new LinkedHashMap<>();
        fpRegisters = new LinkedHashMap<>();
        integerTags = new LinkedHashMap<>();
        fpTags = new LinkedHashMap<>();
        
        // Initialize all registers to 0 (user can set via GUI)
        for (int i = 0; i < Constants.NUM_INTEGER_REGISTERS; i++) {
            String regName = "R" + i;
            integerRegisters.put(regName, 0.0);
            integerTags.put(regName, null);
        }
        
        // Initialize all floating point registers to 0
        for (int i = 0; i < Constants.NUM_FP_REGISTERS; i++) {
            String regName = "F" + i;
            fpRegisters.put(regName, 0.0);
            fpTags.put(regName, null);
        }
        
        // R0 is always 0
        integerRegisters.put("R0", 0.0);
    }
    
    public RegisterType getRegisterType(String regName) {
        if (regName.startsWith("R")) {
            return RegisterType.INTEGER;
        } else if (regName.startsWith("F")) {
            return RegisterType.FLOATING_POINT;
        }
        return null;
    }
    
    public Double getValue(String regName) {
        RegisterType type = getRegisterType(regName);
        if (type == RegisterType.INTEGER) {
            return integerRegisters.getOrDefault(regName, 0.0);
        } else if (type == RegisterType.FLOATING_POINT) {
            return fpRegisters.getOrDefault(regName, 0.0);
        }
        return 0.0;
    }
    
    public void setValue(String regName, Double value) {
        RegisterType type = getRegisterType(regName);
        if (type == RegisterType.INTEGER) {
            if (!regName.equals("R0")) { // R0 is always 0
                integerRegisters.put(regName, value);
            }
        } else if (type == RegisterType.FLOATING_POINT) {
            fpRegisters.put(regName, value);
        }
    }
    
    public String getTag(String regName) {
        RegisterType type = getRegisterType(regName);
        if (type == RegisterType.INTEGER) {
            return integerTags.getOrDefault(regName, null);
        } else if (type == RegisterType.FLOATING_POINT) {
            return fpTags.getOrDefault(regName, null);
        }
        return null;
    }
    
    public void setTag(String regName, String tag) {
        RegisterType type = getRegisterType(regName);
        if (type == RegisterType.INTEGER) {
            integerTags.put(regName, tag);
        } else if (type == RegisterType.FLOATING_POINT) {
            fpTags.put(regName, tag);
        }
    }
    
    public void clearTag(String regName) {
        setTag(regName, null);
    }
    
    public Map<String, Double> getIntegerRegisters() {
        return integerRegisters;
    }
    
    public Map<String, Double> getFpRegisters() {
        return fpRegisters;
    }
    
    public Map<String, String> getIntegerTags() {
        return integerTags;
    }
    
    public Map<String, String> getFpTags() {
        return fpTags;
    }
    
    public void reset() {
        // Reset all registers to 0 (user can initialize via GUI)
        for (int i = 0; i < Constants.NUM_INTEGER_REGISTERS; i++) {
            String regName = "R" + i;
            integerRegisters.put(regName, 0.0);
            integerTags.put(regName, null);
        }
        
        for (int i = 0; i < Constants.NUM_FP_REGISTERS; i++) {
            String regName = "F" + i;
            fpRegisters.put(regName, 0.0);
            fpTags.put(regName, null);
        }
        
        // R0 is always 0
        integerRegisters.put("R0", 0.0);
    }
    
    public void preloadRegister(String regName, Double value) {
        setValue(regName, value);
    }
}





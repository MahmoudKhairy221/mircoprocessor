package com.example.tomasulo.utils;

public enum InstructionType {
    // Floating Point Double Precision
    ADD_D("ADD.D", InstructionCategory.FP_ADD_SUB, 2),
    SUB_D("SUB.D", InstructionCategory.FP_ADD_SUB, 2),
    MUL_D("MUL.D", InstructionCategory.FP_MUL_DIV, 10),
    DIV_D("DIV.D", InstructionCategory.FP_MUL_DIV, 40),
    
    // Floating Point Single Precision
    ADD_S("ADD.S", InstructionCategory.FP_ADD_SUB, 2),
    SUB_S("SUB.S", InstructionCategory.FP_ADD_SUB, 2),
    MUL_S("MUL.S", InstructionCategory.FP_MUL_DIV, 10),
    DIV_S("DIV.S", InstructionCategory.FP_MUL_DIV, 40),
    
    // Integer Operations
    ADDI("ADDI", InstructionCategory.INTEGER_ALU, 1),
    DADDI("DADDI", InstructionCategory.INTEGER_ALU, 1),
    SUBI("SUBI", InstructionCategory.INTEGER_ALU, 1),
    DSUBI("DSUBI", InstructionCategory.INTEGER_ALU, 1),
    
    // Load Operations
    L_D("L.D", InstructionCategory.LOAD, 2),
    L_S("L.S", InstructionCategory.LOAD, 2),
    LW("LW", InstructionCategory.LOAD, 2),
    LD("LD", InstructionCategory.LOAD, 2),
    
    // Store Operations
    S_D("S.D", InstructionCategory.STORE, 2),
    S_S("S.S", InstructionCategory.STORE, 2),
    SW("SW", InstructionCategory.STORE, 2),
    SD("SD", InstructionCategory.STORE, 2),
    
    // Branch Operations
    BEQ("BEQ", InstructionCategory.BRANCH, 1),
    BNE("BNE", InstructionCategory.BRANCH, 1);
    
    private final String mnemonic;
    private final InstructionCategory category;
    private final int defaultLatency;
    
    InstructionType(String mnemonic, InstructionCategory category, int defaultLatency) {
        this.mnemonic = mnemonic;
        this.category = category;
        this.defaultLatency = defaultLatency;
    }
    
    public String getMnemonic() {
        return mnemonic;
    }
    
    public InstructionCategory getCategory() {
        return category;
    }
    
    public int getDefaultLatency() {
        return defaultLatency;
    }
    
    public enum InstructionCategory {
        FP_ADD_SUB,
        FP_MUL_DIV,
        INTEGER_ALU,
        LOAD,
        STORE,
        BRANCH
    }
}


package com.example.tomasulo.core;

import com.example.tomasulo.utils.InstructionType;

public class Instruction {
    private InstructionType type;
    private String destRegister;
    private String srcRegister1;
    private String srcRegister2;
    private int immediate; // For loads/stores and ADDI/SUBI
    private String baseRegister; // For loads/stores
    private String branchTarget; // For branches (label or address)
    private int instructionAddress; // PC address of this instruction
    private int issueCycle = -1;
    private int executeStartCycle = -1;
    private int executeEndCycle = -1;
    private int writeBackCycle = -1;
    private boolean completed = false;
    
    public Instruction(InstructionType type) {
        this.type = type;
    }
    
    // Getters and Setters
    public InstructionType getType() {
        return type;
    }
    
    public void setType(InstructionType type) {
        this.type = type;
    }
    
    public String getDestRegister() {
        return destRegister;
    }
    
    public void setDestRegister(String destRegister) {
        this.destRegister = destRegister;
    }
    
    public String getSrcRegister1() {
        return srcRegister1;
    }
    
    public void setSrcRegister1(String srcRegister1) {
        this.srcRegister1 = srcRegister1;
    }
    
    public String getSrcRegister2() {
        return srcRegister2;
    }
    
    public void setSrcRegister2(String srcRegister2) {
        this.srcRegister2 = srcRegister2;
    }
    
    public int getImmediate() {
        return immediate;
    }
    
    public void setImmediate(int immediate) {
        this.immediate = immediate;
    }
    
    public String getBaseRegister() {
        return baseRegister;
    }
    
    public void setBaseRegister(String baseRegister) {
        this.baseRegister = baseRegister;
    }
    
    public String getBranchTarget() {
        return branchTarget;
    }
    
    public void setBranchTarget(String branchTarget) {
        this.branchTarget = branchTarget;
    }
    
    public int getInstructionAddress() {
        return instructionAddress;
    }
    
    public void setInstructionAddress(int instructionAddress) {
        this.instructionAddress = instructionAddress;
    }
    
    public int getIssueCycle() {
        return issueCycle;
    }
    
    public void setIssueCycle(int issueCycle) {
        this.issueCycle = issueCycle;
    }
    
    public int getExecuteStartCycle() {
        return executeStartCycle;
    }
    
    public void setExecuteStartCycle(int executeStartCycle) {
        this.executeStartCycle = executeStartCycle;
    }
    
    public int getExecuteEndCycle() {
        return executeEndCycle;
    }
    
    public void setExecuteEndCycle(int executeEndCycle) {
        this.executeEndCycle = executeEndCycle;
    }
    
    public int getWriteBackCycle() {
        return writeBackCycle;
    }
    
    public void setWriteBackCycle(int writeBackCycle) {
        this.writeBackCycle = writeBackCycle;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public Instruction copy() {
        Instruction copy = new Instruction(this.type);
        copy.destRegister = this.destRegister;
        copy.srcRegister1 = this.srcRegister1;
        copy.srcRegister2 = this.srcRegister2;
        copy.immediate = this.immediate;
        copy.baseRegister = this.baseRegister;
        copy.branchTarget = this.branchTarget;
        copy.instructionAddress = this.instructionAddress;
        // Do not copy cycle information or completion status
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getMnemonic()).append(" ");
        
        switch (type.getCategory()) {
            case FP_ADD_SUB:
            case FP_MUL_DIV:
                sb.append(destRegister).append(", ").append(srcRegister1).append(", ").append(srcRegister2);
                break;
            case INTEGER_ALU:
                sb.append(destRegister).append(", ").append(srcRegister1).append(", ").append(immediate);
                break;
            case LOAD:
                sb.append(destRegister).append(", ").append(immediate).append("(").append(baseRegister).append(")");
                break;
            case STORE:
                sb.append(srcRegister1).append(", ").append(immediate).append("(").append(baseRegister).append(")");
                break;
            case BRANCH:
                sb.append(srcRegister1).append(", ").append(srcRegister2).append(", ").append(branchTarget);
                break;
        }
        
        return sb.toString();
    }
}


package com.example.tomasulo.components;

import com.example.tomasulo.core.Instruction;
import com.example.tomasulo.utils.InstructionType;

public class ReservationStation {
    private String name; // e.g., "Add1", "Mul1"
    private boolean busy;
    private InstructionType.InstructionCategory category;
    private String operation; // Instruction mnemonic
    private String vj, vk; // Value of source operands
    private String qj, qk; // Reservation station producing source operands
    private String destination; // Destination register
    private Instruction instruction; // The instruction in this station
    private int cyclesRemaining; // Cycles left for execution
    private int issueCycle;
    
    public ReservationStation(String name, InstructionType.InstructionCategory category) {
        this.name = name;
        this.category = category;
        this.busy = false;
        this.cyclesRemaining = 0;
    }
    
    public boolean isReady() {
        return busy && (qj == null || qj.isEmpty()) && (qk == null || qk.isEmpty()) && cyclesRemaining == 0;
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    
    public String getName() {
        return name;
    }
    
    public InstructionType.InstructionCategory getCategory() {
        return category;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getVj() {
        return vj;
    }
    
    public void setVj(String vj) {
        this.vj = vj;
    }
    
    public String getVk() {
        return vk;
    }
    
    public void setVk(String vk) {
        this.vk = vk;
    }
    
    public String getQj() {
        return qj;
    }
    
    public void setQj(String qj) {
        this.qj = qj;
    }
    
    public String getQk() {
        return qk;
    }
    
    public void setQk(String qk) {
        this.qk = qk;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Instruction getInstruction() {
        return instruction;
    }
    
    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }
    
    public int getCyclesRemaining() {
        return cyclesRemaining;
    }
    
    public void setCyclesRemaining(int cyclesRemaining) {
        this.cyclesRemaining = cyclesRemaining;
    }
    
    public void decrementCycles() {
        if (cyclesRemaining > 0) {
            cyclesRemaining--;
        }
    }
    
    public int getIssueCycle() {
        return issueCycle;
    }
    
    public void setIssueCycle(int issueCycle) {
        this.issueCycle = issueCycle;
    }
    
    public void clear() {
        this.busy = false;
        this.operation = null;
        this.vj = null;
        this.vk = null;
        this.qj = null;
        this.qk = null;
        this.destination = null;
        this.instruction = null;
        this.cyclesRemaining = 0;
        this.issueCycle = -1;
    }
    
    @Override
    public String toString() {
        if (!busy) {
            return name + ": Empty";
        }
        return String.format("%s: Op=%s, Vj=%s, Vk=%s, Qj=%s, Qk=%s, Dest=%s, Cycles=%d",
                name, operation, vj, vk, qj, qk, destination, cyclesRemaining);
    }
}











package com.example.tomasulo.core;

import java.util.ArrayList;
import java.util.List;

public class ExecutionState {
    private int currentCycle;
    private int instructionPointer;
    private List<Instruction> instructions; // The static program
    private List<Instruction> trace; // The dynamic execution trace
    private List<String> executionLog; // Text log of execution events
    private Instruction currentInstruction;
    private boolean simulationComplete;
    private String statusMessage;
    
    public ExecutionState() {
        this.currentCycle = 0;
        this.instructionPointer = 0;
        this.instructions = new ArrayList<>();
        this.trace = new ArrayList<>();
        this.executionLog = new ArrayList<>();
        this.simulationComplete = false;
        this.statusMessage = "";
    }
    
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    public void setCurrentCycle(int currentCycle) {
        this.currentCycle = currentCycle;
    }
    
    public void incrementCycle() {
        this.currentCycle++;
    }
    
    public int getInstructionPointer() {
        return instructionPointer;
    }
    
    public void setInstructionPointer(int instructionPointer) {
        this.instructionPointer = instructionPointer;
    }
    
    public void incrementInstructionPointer() {
        this.instructionPointer++;
    }
    
    public List<Instruction> getInstructions() {
        return instructions;
    }
    
    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
    
    public List<Instruction> getTrace() {
        return trace;
    }
    
    public void addToTrace(Instruction instruction) {
        this.trace.add(instruction);
    }
    
    public void clearTrace() {
        this.trace.clear();
        this.executionLog.clear();
    }

    public List<String> getExecutionLog() {
        return executionLog;
    }

    public void addLogMessage(String message) {
        this.executionLog.add(message);
    }
    
    public Instruction getCurrentInstruction() {
        return currentInstruction;
    }
    
    public void setCurrentInstruction(Instruction currentInstruction) {
        this.currentInstruction = currentInstruction;
    }
    
    public boolean isSimulationComplete() {
        return simulationComplete;
    }
    
    public void setSimulationComplete(boolean simulationComplete) {
        this.simulationComplete = simulationComplete;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}


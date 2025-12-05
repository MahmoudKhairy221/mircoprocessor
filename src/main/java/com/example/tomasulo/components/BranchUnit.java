package com.example.tomasulo.components;

import com.example.tomasulo.core.Instruction;
import com.example.tomasulo.utils.InstructionType;

public class BranchUnit {
    private Instruction currentBranch;
    private boolean branchResolved;
    private boolean branchTaken;
    private int targetAddress;
    private int cyclesRemaining;
    
    public BranchUnit() {
        reset();
    }
    
    public boolean isProcessingBranch() {
        return currentBranch != null && !branchResolved;
    }
    
    public void startBranch(Instruction branch, double reg1Value, double reg2Value) {
        this.currentBranch = branch;
        this.branchResolved = false;
        this.cyclesRemaining = 1; // Branch resolution takes 1 cycle
        
        // Evaluate branch condition
        boolean conditionMet = false;
        if (branch.getType() == InstructionType.BEQ) {
            conditionMet = (reg1Value == reg2Value);
        } else if (branch.getType() == InstructionType.BNE) {
            conditionMet = (reg1Value != reg2Value);
        }
        
        this.branchTaken = conditionMet;
        
        // Calculate target address
        if (branchTaken) {
            // Branch target is PC + 4 + (immediate * 4)
            int pc = branch.getInstructionAddress();
            int offset = branch.getImmediate();
            this.targetAddress = pc + 4 + (offset * 4);
        } else {
            // Not taken, continue to next instruction
            this.targetAddress = branch.getInstructionAddress() + 4;
        }
    }
    
    public void tick() {
        if (isProcessingBranch() && cyclesRemaining > 0) {
            cyclesRemaining--;
            if (cyclesRemaining == 0) {
                branchResolved = true;
            }
        }
    }
    
    public boolean isBranchResolved() {
        return branchResolved;
    }
    
    public boolean isBranchTaken() {
        return branchTaken;
    }
    
    public int getTargetAddress() {
        return targetAddress;
    }
    
    public Instruction getCurrentBranch() {
        return currentBranch;
    }
    
    public void reset() {
        this.currentBranch = null;
        this.branchResolved = true;
        this.branchTaken = false;
        this.targetAddress = -1;
        this.cyclesRemaining = 0;
    }
    
    public void clear() {
        reset();
    }
}











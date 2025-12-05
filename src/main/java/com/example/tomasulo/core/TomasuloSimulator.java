package com.example.tomasulo.core;

import com.example.tomasulo.components.*;
import com.example.tomasulo.utils.Constants;
import com.example.tomasulo.utils.InstructionType;
import com.example.tomasulo.utils.RegisterType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TomasuloSimulator {
    private ReservationStationManager rsManager;
    private RegisterFile registerFile;
    private Cache cache;
    private LoadStoreBuffer loadStoreBuffer;
    private BranchUnit branchUnit;
    private ExecutionState state;
    private List<Instruction> instructions;
    private boolean branchStall;
    
    // Track simultaneous completion groups
    // Key: executeEndCycle, Value: Set of reservation station names that finished at that cycle
    private Map<Integer, Set<String>> simultaneousCompletionGroups;
    // Track which reservation station names belong to incomplete simultaneous completion groups
    private Set<String> incompleteGroupMembers;
    // Track which RS names wrote back this cycle and are in incomplete groups
    private Set<String> incompleteGroupMembersWrittenBackThisCycle;
    // Track which dependent RS names should wait because their dependency is in an incomplete group
    private Set<String> dependentRSWaitingForIncompleteGroup;
    
    public TomasuloSimulator() {
        this(Constants.DEFAULT_CACHE_SIZE, Constants.DEFAULT_BLOCK_SIZE);
    }
    
    public TomasuloSimulator(int cacheSize, int blockSize) {
        rsManager = new ReservationStationManager();
        registerFile = new RegisterFile();
        cache = new Cache(cacheSize, blockSize,
                Constants.DEFAULT_CACHE_HIT_LATENCY, Constants.DEFAULT_CACHE_MISS_PENALTY);
        loadStoreBuffer = new LoadStoreBuffer(Constants.DEFAULT_LOAD_STORE_BUFFER_SIZE);
        branchUnit = new BranchUnit();
        state = new ExecutionState();
        instructions = new ArrayList<>();
        branchStall = false;
        simultaneousCompletionGroups = new HashMap<>();
        incompleteGroupMembers = new HashSet<>();
    }
    
    public void loadInstructions(List<Instruction> insts) {
        this.instructions = new ArrayList<>(insts);
        state.setInstructions(instructions);
        state.setInstructionPointer(0);
        state.setCurrentCycle(0);
        reset();
    }
    
    public void reset() {
        rsManager.reset();
        registerFile.reset();
        cache.reset();
        loadStoreBuffer.reset();
        branchUnit.reset();
        state.setCurrentCycle(0);
        state.setInstructionPointer(0);
        state.clearTrace(); // Clear the trace
        state.setSimulationComplete(false);
        state.setStatusMessage("Ready");
        branchStall = false;
        simultaneousCompletionGroups.clear();
        incompleteGroupMembers.clear();
        incompleteGroupMembersWrittenBackThisCycle = new HashSet<>();
        dependentRSWaitingForIncompleteGroup = new HashSet<>();
        
        // Reset program instructions (just in case)
        for (Instruction inst : instructions) {
            inst.setIssueCycle(-1);
            inst.setExecuteStartCycle(-1);
            inst.setExecuteEndCycle(-1);
            inst.setWriteBackCycle(-1);
            inst.setCompleted(false);
        }
    }
    
    public void step() {
        if (state.isSimulationComplete()) {
            return;
        }
        
        state.incrementCycle();
        state.setStatusMessage("Cycle " + state.getCurrentCycle());
        
        // Clear the set of incomplete group members that wrote back this cycle
        // NOTE: Do NOT clear dependentRSWaitingForIncompleteGroup here; we keep
        // it until the corresponding group finishes write-back to ensure
        // dependents stay stalled across cycles.
        incompleteGroupMembersWrittenBackThisCycle.clear();
        
        // 1. Execute stage (try to start execution for ready instructions)
        execute();
        
        // 2. Issue stage (only if not stalled by branch)
        // Branch stall prevents new instructions from issuing while a branch is executing
        // The stall is cleared in write-back when the branch completes
        if (!branchStall) {
            issue();
        }
        
        // 3. Write-back stage (highest priority, but happens AFTER check to ensure cycle delay)
        writeBack();
        
        // 4. Update reservation stations (decrement cycles)
        rsManager.tick();
        tickLoadStoreBuffer();
        
        // 5. Check if execution ended (set executeEndCycle)
        checkExecutionEnd();
        
        // 6. Check if simulation is complete
        checkCompletion();
    }
    
    private void tickLoadStoreBuffer() {
        // Decrement cycles for load/store buffer entries
        for (LoadStoreBuffer.LoadStoreEntry entry : loadStoreBuffer.getEntries()) {
            if (entry.getCyclesRemaining() > 0) {
                entry.decrementCycles();
            }
        }
    }
    
    private void checkExecutionEnd() {
        // Check all reservation stations for completed execution
        for (ReservationStation rs : rsManager.getAllStations()) {
            if (!rs.isBusy()) {
                continue;
            }
            
            Instruction inst = rs.getInstruction();
            if (inst == null) {
                continue;
            }
            
            // Only check if execution has started but not ended
            if (inst.getExecuteStartCycle() == -1) {
                continue; // Execution hasn't started yet
            }
            
            if (inst.getExecuteEndCycle() != -1) {
                continue; // Execution already ended
            }
            
            // Check if execution just completed (cycles reached 0)
            // For stores, also check that the load/store buffer entry cycles are 0
            boolean executionComplete = false;
            if (inst.getType().getCategory() == InstructionType.InstructionCategory.STORE) {
                LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.getEntryByStation(rs.getName());
                // For stores, both RS cycles and entry cycles must be 0
                if (entry != null) {
                    if (rs.getCyclesRemaining() == 0 && entry.getCyclesRemaining() == 0) {
                        executionComplete = true;
                        
                        // PERFORM ACTUAL STORE TO CACHE/MEMORY HERE
                        int storeSize = getStoreSize(inst.getType());
                        int address = entry.getAddress();
                        double value = entry.getValue();
                        byte[] data = doubleToBytes(value, storeSize);
                        
                        // Actual state update
                        cache.store(address, data);
                    }
                } else {
                    // Entry doesn't exist yet - execution hasn't started
                    // This shouldn't happen if executeStartCycle is set, but handle it
                    if (rs.getCyclesRemaining() == 0) {
                        executionComplete = true;
                    }
                }
            } else if (inst.getType().getCategory() == InstructionType.InstructionCategory.BRANCH) {
                // For branches, check if cycles reached 0
                if (rs.getCyclesRemaining() == 0) {
                    executionComplete = true;
                    // Branch condition evaluation happens in write-back stage
                    // Vj and Vk already contain the register values, keep them for write-back
                }
            } else if (rs.getCyclesRemaining() == 0) {
                executionComplete = true;
            }
            
            if (executionComplete) {
                // Execution just ended
                inst.setExecuteEndCycle(state.getCurrentCycle());
                log("Completed execution of " + inst.toString());
                
                // For loads, prepare the result value from cache
                if (inst.getType().getCategory() == InstructionType.InstructionCategory.LOAD) {
                    LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.getEntryByStation(rs.getName());
                    if (entry != null && entry.getLoadData() != null) {
                        int loadSize = getLoadSize(inst.getType());
                        double value = bytesToDouble(entry.getLoadData(), loadSize);
                        rs.setVj(String.valueOf(value));
                        rs.setQj(null);
                    }
                }
                // For ALU operations, result is already in Vj/Vk, no action needed
                // For stores, execution end is set, write-back will happen later
                // For branches, execution end is set, write-back will handle IP update
            }
        }
    }
    
    private void issue() {
        if (state.getInstructionPointer() >= instructions.size()) {
            return;
        }
        
        // Get original instruction from program
        Instruction originalInst = instructions.get(state.getInstructionPointer());
        
        // Clone it for execution trace
        Instruction inst = originalInst.copy();
        inst.setIssueCycle(state.getCurrentCycle());
        state.addToTrace(inst);
        
        // Check if we can issue this instruction
        ReservationStation rs = rsManager.findAvailableStation(inst.getType());
        
        if (rs == null) {
            // No station available - stall issue
            // Remove from trace if failed
            state.getTrace().remove(state.getTrace().size() - 1);
            return; 
        }
        
        // Handle branches separately
        if (inst.getType().getCategory() == InstructionType.InstructionCategory.BRANCH) {
            issueBranch(inst, rs);
            return;
        }
        
        // Get source operands
        String src1 = inst.getSrcRegister1();
        String src2 = inst.getSrcRegister2();
        String baseReg = inst.getBaseRegister();
        String dest = inst.getDestRegister();
        
        switch (inst.getType().getCategory()) {
            case FP_ADD_SUB:
            case FP_MUL_DIV:
                rs.setBusy(true);
                rs.setOperation(inst.getType().getMnemonic());
                rs.setInstruction(inst);
                log("Issued " + inst.toString() + " to " + rs.getName());
                // Check if source registers are ready
                String tag1 = registerFile.getTag(src1);
                String tag2 = registerFile.getTag(src2);
                
                if (tag1 == null) {
                    rs.setVj(String.valueOf(registerFile.getValue(src1)));
                    rs.setQj(null);
                } else {
                    rs.setQj(tag1);
                }
                
                if (tag2 == null) {
                    rs.setVk(String.valueOf(registerFile.getValue(src2)));
                    rs.setQk(null);
                } else {
                    rs.setQk(tag2);
                }
                
                rs.setDestination(dest);
                registerFile.setTag(dest, rs.getName());
                break;
                
            case INTEGER_ALU:
                rs.setBusy(true);
                rs.setOperation(inst.getType().getMnemonic());
                rs.setInstruction(inst);
                log("Issued " + inst.toString() + " to " + rs.getName());
                tag1 = registerFile.getTag(src1);
                if (tag1 == null) {
                    rs.setVj(String.valueOf(registerFile.getValue(src1)));
                    rs.setQj(null);
                } else {
                    rs.setQj(tag1);
                }
                rs.setVk(String.valueOf(inst.getImmediate()));
                rs.setQk(null);
                rs.setDestination(dest);
                registerFile.setTag(dest, rs.getName());
                break;
                
            case LOAD:
                tag1 = registerFile.getTag(baseReg);
                Integer loadAddress = null;
                if (tag1 == null) {
                    double baseValue = registerFile.getValue(baseReg);
                    int address = (int) baseValue + inst.getImmediate();
                    loadAddress = address;
                    rs.setVj(String.valueOf(address));
                    rs.setQj(null);
                } else {
                    rs.setQj(tag1);
                }

                // If base register is ready, we know the address and can perform
                // an issue-time address clash check before reserving RS/LSB.
                if (loadAddress != null) {
                    int loadSize = getLoadSize(inst.getType());
                    if (loadStoreBuffer.hasAddressClashAtIssue(
                            loadAddress,
                            loadSize,
                            LoadStoreBuffer.LoadStoreType.LOAD,
                            inst.getIssueCycle())) {
                        // Hazard with earlier memory op: do NOT issue this LOAD yet.
                        state.getTrace().remove(state.getTrace().size() - 1);
                        return;
                    }
                }

                rs.setBusy(true);
                rs.setOperation(inst.getType().getMnemonic());
                rs.setInstruction(inst);
                log("Issued " + inst.toString() + " to " + rs.getName());
                rs.setDestination(dest);
                registerFile.setTag(dest, rs.getName());
                loadStoreBuffer.reserveEntry(inst, rs.getName());
                break;
                
            case STORE:
                tag1 = registerFile.getTag(src1);
                String tagBase = registerFile.getTag(baseReg);
                Double storeValue = null;
                Integer storeAddress = null;
                
                if (tag1 == null) {
                    double value = registerFile.getValue(src1);
                    storeValue = value;
                    rs.setVj(String.valueOf(value));
                    rs.setQj(null);
                } else {
                    rs.setQj(tag1);
                }
                
                if (tagBase == null) {
                    double baseValue = registerFile.getValue(baseReg);
                    int address = (int) baseValue + inst.getImmediate();
                    storeAddress = address;
                    rs.setVk(String.valueOf(address));
                    rs.setQk(null);
                } else {
                    rs.setQk(tagBase);
                }

                // If base register is ready, we know the address and can enforce ordering
                if (storeAddress != null) {
                    int storeSize = getStoreSize(inst.getType());
                    if (loadStoreBuffer.hasAddressClashAtIssue(
                            storeAddress,
                            storeSize,
                            LoadStoreBuffer.LoadStoreType.STORE,
                            inst.getIssueCycle())) {
                        // Hazard with earlier memory op: do NOT issue this STORE yet.
                        state.getTrace().remove(state.getTrace().size() - 1);
                        return;
                    }
                }

                rs.setBusy(true);
                rs.setOperation(inst.getType().getMnemonic());
                rs.setInstruction(inst);
                log("Issued " + inst.toString() + " to " + rs.getName());
                rs.setDestination(null);
                loadStoreBuffer.reserveEntry(inst, rs.getName());
                break;
        }
        
        state.incrementInstructionPointer();
    }
    
    private void issueBranch(Instruction inst, ReservationStation rs) {
        String src1 = inst.getSrcRegister1();
        String src2 = inst.getSrcRegister2();
        
        // Trim register names to handle any whitespace issues
        if (src1 != null) src1 = src1.trim();
        if (src2 != null) src2 = src2.trim();
        
        String tag1 = registerFile.getTag(src1);
        String tag2 = registerFile.getTag(src2);
        
        // Branch can only issue if both source registers are ready
        if (tag1 != null || tag2 != null) {
            // Stall until registers are ready
            // Remove from trace as we didn't issue
            state.getTrace().remove(state.getTrace().size() - 1);
            return; 
        }
        
        rs.setBusy(true);
        rs.setOperation(inst.getType().getMnemonic());
        rs.setInstruction(inst);
        
        rs.setVj(String.valueOf(registerFile.getValue(src1)));
        rs.setVk(String.valueOf(registerFile.getValue(src2)));
        rs.setQj(null);
        rs.setQk(null);
        
        // Start branch evaluation
        branchUnit.startBranch(inst, registerFile.getValue(src1), registerFile.getValue(src2));
        branchStall = true;
        
        state.incrementInstructionPointer();
    }
    
    private void execute() {
        // Process all reservation stations
        for (ReservationStation rs : rsManager.getAllStations()) {
            if (!rs.isBusy()) {
                continue;
            }
            
            Instruction inst = rs.getInstruction();
            if (inst == null) {
                continue;
            }
            
            // Handle load/store execution separately
            if (inst.getType().getCategory() == InstructionType.InstructionCategory.LOAD) {
                executeLoad(rs, inst);
            } else if (inst.getType().getCategory() == InstructionType.InstructionCategory.STORE) {
                executeStore(rs, inst);
            } else if (inst.getType().getCategory() == InstructionType.InstructionCategory.BRANCH) {
                // For branch operations, check if operands are ready
                if ((rs.getQj() == null || rs.getQj().isEmpty()) &&
                    (rs.getQk() == null || rs.getQk().isEmpty())) {
                    
                    // Check if any dependency belongs to an incomplete simultaneous completion group
                    if (isDependencyInIncompleteGroup(rs.getQj()) || 
                        isDependencyInIncompleteGroup(rs.getQk())) {
                        // Delay execution start - wait for all group members to write back
                        continue;
                    }
                    
                    // Check if this RS is waiting for an incomplete group member to finish writing back
                    if (dependentRSWaitingForIncompleteGroup.contains(rs.getName()) &&
                        !incompleteGroupMembers.isEmpty()) {
                        // This RS depends on an incomplete group member that wrote back this cycle
                        // Delay execution until all group members have written back
                        continue;
                    }
                    
                    if (inst.getExecuteStartCycle() == -1) {
                        inst.setExecuteStartCycle(state.getCurrentCycle());
                        int latency = rsManager.getInstructionLatency(inst.getType());
                        // Set cycles - these will be decremented in tick() AFTER this cycle
                        rs.setCyclesRemaining(latency);
                        log("Started execution of " + inst.toString());
                    }
                    
                    // Execution continues, cycles decremented in tick()
                }
            } else {
                // For ALU operations, check if operands are ready
                if ((rs.getQj() == null || rs.getQj().isEmpty()) &&
                    (rs.getQk() == null || rs.getQk().isEmpty())) {
                    
                    // Check if any dependency belongs to an incomplete simultaneous completion group
                    if (isDependencyInIncompleteGroup(rs.getQj()) || 
                        isDependencyInIncompleteGroup(rs.getQk())) {
                        // Delay execution start - wait for all group members to write back
                        continue;
                    }
                    
                    // Check if this RS is waiting for an incomplete group member to finish writing back
                    if (dependentRSWaitingForIncompleteGroup.contains(rs.getName()) &&
                        !incompleteGroupMembers.isEmpty()) {
                        // This RS depends on an incomplete group member that wrote back this cycle
                        // Delay execution until all group members have written back
                        continue;
                    }
                    
                    if (inst.getExecuteStartCycle() == -1) {
                        inst.setExecuteStartCycle(state.getCurrentCycle());
                        int latency = rsManager.getInstructionLatency(inst.getType());
                        // Set cycles - these will be decremented in tick() AFTER this cycle
                        rs.setCyclesRemaining(latency);
                        log("Started execution of " + inst.toString());
                    }
                    
                    // Execution continues, cycles decremented in tick()
                }
            }
        }
    }
    
    private void executeLoad(ReservationStation rs, Instruction inst) {
        // Check if base register is ready
        if (rs.getQj() != null && !rs.getQj().isEmpty()) {
            // Also check if the dependency belongs to an incomplete simultaneous completion group
            if (isDependencyInIncompleteGroup(rs.getQj())) {
                return; // Wait for all group members to write back
            }
            return; // Base register not ready - wait
        }
        
        // Vj should contain the address at this point
        if (rs.getVj() == null || rs.getVj().isEmpty()) {
            return; // Address not calculated yet
        }
        
        // Calculate address
        int address;
        try {
            address = Integer.parseInt(rs.getVj());
        } catch (NumberFormatException e) {
            return; // Address not ready
        }
        
        // Check for address clashes with stores
        // Update LSB entry with calculated address
        LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.updateLoadAddress(rs.getName(), address);
        
        if (entry == null) {
            // Should have been reserved at issue
             entry = loadStoreBuffer.addLoad(inst, address, rs.getName());
        }
        
        // Check address clashes (Load checking against Stores)
        int loadSize = getLoadSize(inst.getType());
        if (loadStoreBuffer.hasAddressClash(address, loadSize, entry, LoadStoreBuffer.LoadStoreType.LOAD)) {
            return; // Wait for store to complete
        }
        
        // If execution hasn't started, start it
        if (inst.getExecuteStartCycle() == -1) {
            inst.setExecuteStartCycle(state.getCurrentCycle());
            // Access cache
            Cache.CacheResult result = cache.load(address, loadSize);
            int instructionLatency = rsManager.getInstructionLatency(inst.getType());
            int totalLatency = instructionLatency + result.getLatency(); // 2 + cache_latency
            // Set cycles - these will be decremented in tick() AFTER this cycle
            entry.setCyclesRemaining(totalLatency);
            entry.setLoadData(result.getData());
            rs.setCyclesRemaining(totalLatency);
            // Vj currently has address, will be replaced with value when execution ends
            log("Started execution of " + inst.toString());
        }
    }
    
    private void executeStore(ReservationStation rs, Instruction inst) {
        // Check if operands are ready (Qj and Qk must be null/empty)
        if ((rs.getQj() != null && !rs.getQj().isEmpty()) ||
            (rs.getQk() != null && !rs.getQk().isEmpty())) {
            // Also check if any dependency belongs to an incomplete simultaneous completion group
            if (isDependencyInIncompleteGroup(rs.getQj()) || 
                isDependencyInIncompleteGroup(rs.getQk())) {
                return; // Wait for all group members to write back
            }
            return; // Operands not ready - wait for tags to clear
        }
        
        // Vj should contain value, Vk should contain address
        if (rs.getVj() == null || rs.getVj().isEmpty()) {
            return; // Value not ready
        }
        
        if (rs.getVk() == null || rs.getVk().isEmpty()) {
            return; // Address not ready
        }
        
        // Calculate address and value
        int address;
        double value;
        try {
            address = Integer.parseInt(rs.getVk());
            value = Double.parseDouble(rs.getVj());
        } catch (NumberFormatException e) {
            // Invalid address or value format - wait
            return;
        }
        
        // Check for address clashes with loads/stores
        // Update LSB entry with calculated address and value
        LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.updateStoreAddressAndValue(rs.getName(), address, value);
        
        if (entry == null) {
            // Should have been reserved at issue
            entry = loadStoreBuffer.addStore(inst, address, rs.getName(), value);
        }
        
        // Check address clashes (Store checking against Loads and Stores)
        int storeSize = getStoreSize(inst.getType());
        if (loadStoreBuffer.hasAddressClash(address, storeSize, entry, LoadStoreBuffer.LoadStoreType.STORE)) {
            return; // Wait for conflicting load/store
        }
        
        // If execution hasn't started, start it
        if (inst.getExecuteStartCycle() == -1) {
            inst.setExecuteStartCycle(state.getCurrentCycle());
            // Access cache - ONLY PROBE to calculate latency
            Cache.CacheResult result = cache.probeStore(address, storeSize);
            int instructionLatency = rsManager.getInstructionLatency(inst.getType());
            int totalLatency = instructionLatency + result.getLatency(); // 2 + cache_latency
            // Set cycles - these will be decremented in tick() AFTER this cycle
            entry.setCyclesRemaining(totalLatency);
            rs.setCyclesRemaining(totalLatency);
            
            // NOTE: Actual store to memory happens in checkExecutionEnd when cycles == 0
            log("Started execution of " + inst.toString());
        }
    }
    
    private void writeBack() {
        // Get stations that are ready for write-back (cycles = 0 AND execution ended)
        List<ReservationStation> readyStations = new ArrayList<>();
        for (ReservationStation rs : rsManager.getAllStations()) {
            if (rs.isBusy() && rs.getCyclesRemaining() == 0) {
                Instruction inst = rs.getInstruction();
                if (inst != null && inst.getExecuteEndCycle() != -1) {
                    // Execution has ended, ready for write-back
                    readyStations.add(rs);
                }
            }
        }
        
        // Detect simultaneous completions: if multiple instructions are ready for write-back,
        // check if they have the same executeEndCycle
        if (readyStations.size() > 1) {
            Map<Integer, List<ReservationStation>> groupsByEndCycle = new HashMap<>();
            for (ReservationStation rs : readyStations) {
                Instruction inst = rs.getInstruction();
                if (inst != null) {
                    int endCycle = inst.getExecuteEndCycle();
                    groupsByEndCycle.computeIfAbsent(endCycle, k -> new ArrayList<>()).add(rs);
                }
            }
            
            // For each group with multiple members, create a simultaneous completion group
            for (Map.Entry<Integer, List<ReservationStation>> entry : groupsByEndCycle.entrySet()) {
                if (entry.getValue().size() > 1) {
                    int endCycle = entry.getKey();
                    Set<String> groupMembers = simultaneousCompletionGroups.computeIfAbsent(
                        endCycle, k -> new HashSet<>());
                    
                    for (ReservationStation rs : entry.getValue()) {
                        groupMembers.add(rs.getName());
                        incompleteGroupMembers.add(rs.getName());
                    }
                }
            }
        }
        
        // Sort by issue cycle (FIFO) to handle simultaneous completion
        readyStations.sort(Comparator.comparing((ReservationStation rs) -> {
            Instruction inst = rs.getInstruction();
            return inst != null ? inst.getIssueCycle() : Integer.MAX_VALUE;
        }));
        
        // Write-back one instruction per cycle (handle bus conflicts)
        for (ReservationStation rs : readyStations) {
            Instruction inst = rs.getInstruction();
            if (inst == null) {
                continue;
            }
            
            String dest = rs.getDestination();
            
            if (dest != null && !dest.isEmpty()) {
                // Calculate result
                double result = 0.0;
                
                switch (inst.getType().getCategory()) {
                    case FP_ADD_SUB:
                    case FP_MUL_DIV:
                        double vj = Double.parseDouble(rs.getVj());
                        double vk = Double.parseDouble(rs.getVk());
                        result = performOperation(inst.getType(), vj, vk);
                        break;
                    case INTEGER_ALU:
                        double val = Double.parseDouble(rs.getVj());
                        int imm = inst.getImmediate();
                        if (inst.getType() == InstructionType.ADDI || inst.getType() == InstructionType.DADDI) {
                            result = val + imm;
                        } else {
                            result = val - imm;
                        }
                        break;
                    case LOAD:
                        result = Double.parseDouble(rs.getVj());
                        break;
                }
                
                // Write result to register file
                registerFile.setValue(dest, result);
                
                // Clear tag only if this reservation station is still the
                // current producer for the destination register. This avoids
                // an older writer (e.g., an earlier L.D) clearing the tag of
                // a later writer (e.g., ADD.D) that also targets the same
                // register.
                String currentTag = registerFile.getTag(dest);
                if (currentTag != null && currentTag.equals(rs.getName())) {
                    registerFile.clearTag(dest);
                }
                
                // Check if this RS is in an incomplete group before updating operands
                boolean isInIncompleteGroup = incompleteGroupMembers.contains(rs.getName());
                
                // If this RS is in an incomplete group, track which dependent RS are affected
                // BEFORE calling updateOperands() (which will clear Qj/Qk)
                if (isInIncompleteGroup) {
                    incompleteGroupMembersWrittenBackThisCycle.add(rs.getName());
                    // Track which dependent RS had their Qj/Qk cleared by this incomplete group member
                    for (ReservationStation dependentRS : rsManager.getAllStations()) {
                        if (dependentRS.isBusy() && 
                            (rs.getName().equals(dependentRS.getQj()) || 
                             rs.getName().equals(dependentRS.getQk()))) {
                            dependentRSWaitingForIncompleteGroup.add(dependentRS.getName());
                        }
                    }
                }
                
                // Update reservation stations waiting for this result
                rsManager.updateOperands(rs.getName(), result);
                
                // Mark instruction as complete
                inst.setWriteBackCycle(state.getCurrentCycle());
                inst.setCompleted(true);
                log("Write-back result " + result + " for " + inst.toString() + " to " + dest);
                
                // Remove from LoadStoreBuffer if it's a LOAD
                if (inst.getType().getCategory() == InstructionType.InstructionCategory.LOAD) {
                    LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.getEntryByStation(rs.getName());
                    if (entry != null) {
                        entry.setCompleted(true);
                        loadStoreBuffer.removeEntry(entry);
                    }
                }
                
                // Clear reservation station
                rs.clear();
                
                // Check if this RS was part of a simultaneous completion group
                // and if so, check if all members have written back
                checkAndCleanupGroup(rs.getName(), inst.getExecuteEndCycle());
                
                // Only write-back one instruction per cycle (bus conflict resolution)
                break;
            } else if (inst.getType().getCategory() == InstructionType.InstructionCategory.STORE) {
                // Store completed
                inst.setWriteBackCycle(state.getCurrentCycle());
                inst.setCompleted(true);
                log("Store completed for " + inst.toString());
                rs.clear();
                
                LoadStoreBuffer.LoadStoreEntry entry = loadStoreBuffer.getEntryByStation(rs.getName());
                if (entry != null) {
                    entry.setCompleted(true);
                    loadStoreBuffer.removeEntry(entry);
                }
                
                // Check if this RS is in an incomplete group
                boolean isInIncompleteGroup = incompleteGroupMembers.contains(rs.getName());
                if (isInIncompleteGroup) {
                    incompleteGroupMembersWrittenBackThisCycle.add(rs.getName());
                    // Track which dependent RS had their Qj/Qk cleared by this incomplete group member
                    // BEFORE the RS is cleared (stores don't have destination, so no updateOperands call)
                    // But we still need to track for cleanup purposes
                }
                
                // Check if this RS was part of a simultaneous completion group
                checkAndCleanupGroup(rs.getName(), inst.getExecuteEndCycle());
                
                break;
            } else if (inst.getType().getCategory() == InstructionType.InstructionCategory.BRANCH) {
                // Branch completed - evaluate condition and update instruction pointer if taken
                // CRITICAL: Read current register values from register file, not from Vj/Vk
                // Vj/Vk contain values captured at ISSUE time, which may be stale if registers
                // were modified by earlier instructions that completed between issue and write-back
                String src1 = inst.getSrcRegister1();
                String src2 = inst.getSrcRegister2();
                
                // CONSOLE LOG: Branch evaluation start
                System.out.println("\n========== BRANCH EVALUATION (Cycle " + state.getCurrentCycle() + ") ==========");
                System.out.println("Instruction: " + inst.toString());
                System.out.println("Branch Type: " + inst.getType().getMnemonic());
                System.out.println("Raw register names from instruction: src1='" + src1 + "' src2='" + src2 + "'");
                
                // Validate register names
                if (src1 == null || src2 == null) {
                    System.out.println("ERROR: Branch has null register names: src1=" + src1 + ", src2=" + src2);
                    log("ERROR: Branch has null register names: src1=" + src1 + ", src2=" + src2);
                    inst.setWriteBackCycle(state.getCurrentCycle());
                    inst.setCompleted(true);
                    branchStall = false;
                    rs.clear();
                    break;
                }
                
                // Trim register names to handle any whitespace issues
                src1 = src1.trim();
                src2 = src2.trim();
                System.out.println("Trimmed register names: src1='" + src1 + "' src2='" + src2 + "'");
                
                // CONSOLE LOG: Register file state before reading
                System.out.println("Register file state:");
                System.out.println("  Integer registers: " + registerFile.getIntegerRegisters());
                System.out.println("  FP registers: " + registerFile.getFpRegisters());
                
                // Read current values directly from register file
                // This ensures we get the most up-to-date values, even if registers were
                // modified by earlier instructions that completed between branch issue and write-back
                double reg1Value = registerFile.getValue(src1);
                double reg2Value = registerFile.getValue(src2);
                
                // CONSOLE LOG: Values read from register file
                System.out.println("Values read from register file:");
                System.out.println("  " + src1 + " = " + reg1Value);
                System.out.println("  " + src2 + " = " + reg2Value);
                
                // Log the values being compared for debugging
                log("Branch evaluation at write-back: " + inst.toString() + 
                    " | Register names: src1='" + src1 + "' src2='" + src2 + "'" +
                    " | Reading from register file: " + src1 + "=" + reg1Value + ", " + src2 + "=" + reg2Value);
                
                boolean branchTaken = false;
                
                // Use Double.compare for robust floating-point comparison
                // For integer registers stored as doubles, this handles edge cases
                int comparison = Double.compare(reg1Value, reg2Value);
                System.out.println("Double.compare(" + reg1Value + ", " + reg2Value + ") = " + comparison);
                
                if (inst.getType() == InstructionType.BEQ) {
                    branchTaken = (comparison == 0);
                    System.out.println("BEQ: comparison == 0? " + (comparison == 0) + " -> branchTaken = " + branchTaken);
                    log("BEQ comparison: Double.compare(" + reg1Value + ", " + reg2Value + ") = " + comparison + " -> " + branchTaken);
                } else if (inst.getType() == InstructionType.BNE) {
                    branchTaken = (comparison != 0);
                    System.out.println("BNE: comparison != 0? " + (comparison != 0) + " -> branchTaken = " + branchTaken);
                    log("BNE comparison: Double.compare(" + reg1Value + ", " + reg2Value + ") = " + comparison + " -> " + branchTaken);
                } else {
                    System.out.println("ERROR: Unknown branch type: " + inst.getType());
                    log("ERROR: Unknown branch type: " + inst.getType());
                }
                
                System.out.println("Branch decision: " + (branchTaken ? "TAKEN" : "NOT TAKEN"));
                log("Branch condition result: " + inst.getType().getMnemonic() + " -> " + 
                    (branchTaken ? "TAKEN" : "NOT TAKEN"));
                
                if (branchTaken) {
                    // Calculate target address and update instruction pointer
                    int branchAddress = inst.getInstructionAddress();
                    int offset = inst.getImmediate();
                    int targetAddress = branchAddress + 4 + (offset * 4);
                    
                    System.out.println("Branch TAKEN - calculating target:");
                    System.out.println("  Branch address: " + branchAddress);
                    System.out.println("  Offset: " + offset);
                    System.out.println("  Target address: " + branchAddress + " + 4 + (" + offset + " * 4) = " + targetAddress);
                    System.out.println("  Current IP before branch: " + state.getInstructionPointer());
                    
                    boolean found = false;
                    
                    // Try to find by exact address match
                    System.out.println("Searching for target address " + targetAddress + " in instructions:");
                    for (int i = 0; i < instructions.size(); i++) {
                        Instruction instr = instructions.get(i);
                        System.out.println("  [" + i + "] Address: " + instr.getInstructionAddress() + " - " + instr.toString());
                        if (instr.getInstructionAddress() == targetAddress) {
                            if (i >= 0 && i < instructions.size()) {
                                System.out.println("  FOUND! Setting IP to " + i);
                                state.setInstructionPointer(i);
                                found = true;
                                // Reset completion flag when looping back
                                if (state.isSimulationComplete()) {
                                    state.setSimulationComplete(false);
                                }
                            }
                            break;
                        }
                    }
                    
                    // Fallback: calculate index from branch offset
                    if (!found) {
                        System.out.println("Exact address match failed, trying offset calculation...");
                        int branchIndex = -1;
                        for (int i = 0; i < instructions.size(); i++) {
                            if (instructions.get(i).getInstructionAddress() == branchAddress &&
                                instructions.get(i).getType() == inst.getType()) {
                                branchIndex = i;
                                break;
                            }
                        }
                        
                        System.out.println("  Branch index: " + branchIndex);
                        if (branchIndex >= 0) {
                            int targetIndex = branchIndex + offset + 1;
                            System.out.println("  Target index: " + branchIndex + " + " + offset + " + 1 = " + targetIndex);
                            
                            if (targetIndex >= 0 && targetIndex < instructions.size()) {
                                System.out.println("  FOUND via offset! Setting IP to " + targetIndex);
                                state.setInstructionPointer(targetIndex);
                                found = true;
                                log("Branch target resolved by offset calculation: index " + targetIndex);
                                if (state.isSimulationComplete()) {
                                    state.setSimulationComplete(false);
                                }
                            } else {
                                System.out.println("  ERROR: Target index " + targetIndex + " out of bounds [0, " + instructions.size() + ")");
                            }
                        }
                    }
                    
                    if (!found) {
                        System.out.println("ERROR: Branch target address " + targetAddress + " not found - instruction pointer not updated");
                        log("ERROR: Branch target address " + targetAddress + " not found - instruction pointer not updated");
                    } else {
                        System.out.println("Branch TAKEN: IP updated to " + state.getInstructionPointer());
                        log("Branch TAKEN: jumping to address " + targetAddress);
                    }
                } else {
                    System.out.println("Branch NOT TAKEN: IP remains at " + state.getInstructionPointer());
                    log("Branch NOT TAKEN: continuing to next instruction");
                }
                
                System.out.println("Final IP after branch: " + state.getInstructionPointer());
                System.out.println("==========================================\n");
                
                // Mark branch as complete
                inst.setWriteBackCycle(state.getCurrentCycle());
                inst.setCompleted(true);
                
                // Clear branch stall if it was set
                branchStall = false;
                
                // Clear reservation station
                rs.clear();
                
                // Check if this RS was part of a simultaneous completion group
                checkAndCleanupGroup(rs.getName(), inst.getExecuteEndCycle());
                
                break;
            }
        }
    }
    
    /**
     * Check if a dependency (reservation station name) belongs to an incomplete
     * simultaneous completion group. If so, execution should be delayed.
     */
    private boolean isDependencyInIncompleteGroup(String dependencyRS) {
        if (dependencyRS == null || dependencyRS.isEmpty()) {
            return false;
        }
        return incompleteGroupMembers.contains(dependencyRS);
    }
    
    /**
     * Check if a reservation station was part of a simultaneous completion group,
     * and if all members have written back, mark the group as complete.
     */
    private void checkAndCleanupGroup(String rsName, int executeEndCycle) {
        Set<String> group = simultaneousCompletionGroups.get(executeEndCycle);
        if (group != null && group.contains(rsName)) {
            // Check if all members of this group have written back
            boolean allWrittenBack = true;
            for (String memberRS : group) {
                // Find the reservation station by name
                ReservationStation memberStation = null;
                for (ReservationStation rs : rsManager.getAllStations()) {
                    if (rs.getName().equals(memberRS)) {
                        memberStation = rs;
                        break;
                    }
                }
                
                if (memberStation != null && memberStation.isBusy()) {
                    // Station is still busy - check if instruction has written back
                    Instruction memberInst = memberStation.getInstruction();
                    if (memberInst != null && memberInst.getWriteBackCycle() == -1) {
                        // Instruction hasn't written back yet
                        allWrittenBack = false;
                        break;
                    }
                }
                // If station is not busy or not found, it has been cleared (written back)
            }
            
            if (allWrittenBack) {
                // All members have written back - mark group as complete
                for (String memberRS : group) {
                    incompleteGroupMembers.remove(memberRS);
                }
                // Clear dependent RS waiting flags since the group is now complete
                dependentRSWaitingForIncompleteGroup.clear();
            }
        }
    }
    
    private double performOperation(InstructionType type, double vj, double vk) {
        switch (type) {
            case ADD_D:
            case ADD_S:
                return vj + vk;
            case SUB_D:
            case SUB_S:
                return vj - vk;
            case MUL_D:
            case MUL_S:
                return vj * vk;
            case DIV_D:
            case DIV_S:
                return vk != 0 ? vj / vk : 0;
            default:
                return 0;
        }
    }
    
    private int getLoadSize(InstructionType type) {
        switch (type) {
            case L_D:
            case LD:
                return 8;
            case L_S:
            case LW:
                return 4;
            default:
                return 0;
        }
    }
    
    private int getStoreSize(InstructionType type) {
        switch (type) {
            case S_D:
            case SD:
                return 8;
            case S_S:
            case SW:
                return 4;
            default:
                return 0;
        }
    }
    
    /**
     * Encode a numeric value into bytes for memory/cache.
     *
     * Semantics:
     * - Interpret the double as an integer value (truncating toward zero).
     * - Lay out the integer in little-endian order: byte 0 holds the lowest 8 bits,
     *   byte 1 the next 8 bits, etc.
     * - The lowest {@code size} bytes are kept; any higher bits are discarded.
     *
     * Example:
     *  value = 100 -> binary ...0001100100 -> bytes[0] = 0x64, bytes[1..7] = 0x00.
     */
    private byte[] doubleToBytes(double value, int size) {
        long intValue = (long) value; // caller expected to pass integral values
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) ((intValue >> (8 * i)) & 0xFF);
        }
        return result;
    }
    
    /**
     * Decode bytes from memory/cache back into a numeric value.
     *
     * Semantics:
     * - Treat up to {@code size} bytes as a little-endian unsigned integer.
     * - Convert that integer to a double with the same numeric value.
     *
     * Example:
     *  bytes = {0x64, 0, 0, 0, 0, 0, 0, 0} -> value = 100.0
     */
    private double bytesToDouble(byte[] bytes, int size) {
        long intValue = 0L;
        int len = Math.min(size, bytes.length);
        for (int i = 0; i < len; i++) {
            intValue |= (long) (bytes[i] & 0xFF) << (8 * i);
        }
        return (double) intValue;
    }
    
    private void checkCompletion() {
        // Check if we still have instructions to issue
        if (state.getInstructionPointer() < instructions.size()) {
            // If instruction pointer is before end, there's more work - ensure not marked complete
            if (state.isSimulationComplete()) {
                state.setSimulationComplete(false);
            }
            return;
        }
        
        // Check if all issued instructions are completed
        boolean allCompleted = true;
        for (Instruction inst : state.getTrace()) {
            if (!inst.isCompleted()) {
                allCompleted = false;
                break;
            }
        }
        
        // Also check if all reservation stations are empty
        boolean allStationsEmpty = true;
        for (ReservationStation rs : rsManager.getAllStations()) {
            if (rs.isBusy()) {
                allStationsEmpty = false;
                break;
            }
        }
        
        // Also check if Load/Store Buffer is empty (pending memory operations)
        boolean loadStoreBufferEmpty = loadStoreBuffer.isEmpty();
        
        // Only mark as complete if ALL conditions are met
        if (allCompleted && allStationsEmpty && loadStoreBufferEmpty) {
            state.setSimulationComplete(true);
            state.setStatusMessage("Simulation Complete - Total Cycles: " + state.getCurrentCycle());
        } else {
            // If any condition is not met, ensure we're not marked as complete
            // This handles cases where completion was incorrectly set earlier
            if (state.isSimulationComplete()) {
                state.setSimulationComplete(false);
            }
        }
    }
    
    public void initializeStations(int fpAddSub, int fpMulDiv, int integerALU, int load, int store) {
        rsManager.initializeStations(fpAddSub, fpMulDiv, integerALU, load, store);
    }

    public void setInstructionLatency(String mnemonic, int latency) {
        rsManager.setInstructionLatency(mnemonic, latency);
    }

    /**
     * Clear the reservation station holding the given branch instruction.
     * This is used after branch resolution so the RS is freed for subsequent issues.
     */
    private void clearBranchReservation(Instruction branchInst) {
        if (branchInst == null) {
            return;
        }
        for (ReservationStation rs : rsManager.getAllStations()) {
            if (rs.isBusy() && rs.getInstruction() == branchInst) {
                rs.clear();
                break;
            }
        }
    }

    private void log(String message) {
        state.addLogMessage("Cycle " + state.getCurrentCycle() + ": " + message);
    }
    
    // Getters
    public ReservationStationManager getRsManager() {
        return rsManager;
    }
    
    public RegisterFile getRegisterFile() {
        return registerFile;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public LoadStoreBuffer getLoadStoreBuffer() {
        return loadStoreBuffer;
    }
    
    public BranchUnit getBranchUnit() {
        return branchUnit;
    }
    
    public ExecutionState getState() {
        return state;
    }
    
    public List<Instruction> getInstructions() {
        return instructions;
    }
}

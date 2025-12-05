package com.example.tomasulo.components;

import com.example.tomasulo.core.Instruction;
import com.example.tomasulo.utils.Constants;
import com.example.tomasulo.utils.InstructionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationStationManager {
    private List<ReservationStation> fpAddSubStations;
    private List<ReservationStation> fpMulDivStations;
    private List<ReservationStation> integerALUStations;
    private List<ReservationStation> loadStations;
    private List<ReservationStation> storeStations;
    
    private Map<String, Integer> instructionLatencies;
    
    public ReservationStationManager() {
        this.fpAddSubStations = new ArrayList<>();
        this.fpMulDivStations = new ArrayList<>();
        this.integerALUStations = new ArrayList<>();
        this.loadStations = new ArrayList<>();
        this.storeStations = new ArrayList<>();
        this.instructionLatencies = new HashMap<>();
        
        // Initialize with default sizes
        initializeStations(Constants.DEFAULT_FP_ADD_SUB_STATIONS,
                Constants.DEFAULT_FP_MUL_DIV_STATIONS,
                Constants.DEFAULT_INTEGER_ALU_STATIONS,
                Constants.DEFAULT_LOAD_STATIONS,
                Constants.DEFAULT_STORE_STATIONS);
        
        // Initialize default latencies
        initializeDefaultLatencies();
    }
    
    public void initializeStations(int fpAddSub, int fpMulDiv, int integerALU, int load, int store) {
        fpAddSubStations.clear();
        fpMulDivStations.clear();
        integerALUStations.clear();
        loadStations.clear();
        storeStations.clear();
        
        for (int i = 0; i < fpAddSub; i++) {
            fpAddSubStations.add(new ReservationStation("Add" + (i + 1), InstructionType.InstructionCategory.FP_ADD_SUB));
        }
        for (int i = 0; i < fpMulDiv; i++) {
            fpMulDivStations.add(new ReservationStation("Mult" + (i + 1), InstructionType.InstructionCategory.FP_MUL_DIV));
        }
        for (int i = 0; i < integerALU; i++) {
            integerALUStations.add(new ReservationStation("Int" + (i + 1), InstructionType.InstructionCategory.INTEGER_ALU));
        }
        for (int i = 0; i < load; i++) {
            loadStations.add(new ReservationStation("Load" + (i + 1), InstructionType.InstructionCategory.LOAD));
        }
        for (int i = 0; i < store; i++) {
            storeStations.add(new ReservationStation("Store" + (i + 1), InstructionType.InstructionCategory.STORE));
        }
    }
    
    private void initializeDefaultLatencies() {
        for (InstructionType type : InstructionType.values()) {
            instructionLatencies.put(type.getMnemonic(), type.getDefaultLatency());
        }
    }
    
    public void setInstructionLatency(String mnemonic, int latency) {
        instructionLatencies.put(mnemonic, latency);
    }
    
    public int getInstructionLatency(InstructionType type) {
        return instructionLatencies.getOrDefault(type.getMnemonic(), type.getDefaultLatency());
    }
    
    public ReservationStation findAvailableStation(InstructionType type) {
        List<ReservationStation> stations = getStationsForType(type);
        for (ReservationStation rs : stations) {
            if (!rs.isBusy()) {
                return rs;
            }
        }
        return null;
    }
    
    public List<ReservationStation> getStationsForType(InstructionType type) {
        switch (type.getCategory()) {
            case FP_ADD_SUB:
                return fpAddSubStations;
            case FP_MUL_DIV:
                return fpMulDivStations;
            case INTEGER_ALU:
            case BRANCH:
                return integerALUStations;
            case LOAD:
                return loadStations;
            case STORE:
                return storeStations;
            default:
                return new ArrayList<>();
        }
    }
    
    public List<ReservationStation> getAllStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(fpAddSubStations);
        all.addAll(fpMulDivStations);
        all.addAll(integerALUStations);
        all.addAll(loadStations);
        all.addAll(storeStations);
        return all;
    }
    
    public List<ReservationStation> getReadyStations() {
        List<ReservationStation> ready = new ArrayList<>();
        for (ReservationStation rs : getAllStations()) {
            if (rs.isReady()) {
                ready.add(rs);
            }
        }
        return ready;
    }
    
    public void updateOperands(String stationName, double value) {
        for (ReservationStation rs : getAllStations()) {
            if (rs.getQj() != null && rs.getQj().equals(stationName)) {
                rs.setQj(null);
                
                // For loads, Qj is the base register - calculate address
                if (rs.getCategory() == InstructionType.InstructionCategory.LOAD) {
                    Instruction inst = rs.getInstruction();
                    if (inst != null) {
                        double baseValue = value;
                        int address = (int) baseValue + inst.getImmediate();
                        rs.setVj(String.valueOf(address));
                    }
                } else {
                    // For stores and ALU, Qj is the source register - set value
                    rs.setVj(String.valueOf(value));
                }
            }
            if (rs.getQk() != null && rs.getQk().equals(stationName)) {
                rs.setQk(null);
                
                // For stores, Qk is the base register - calculate address
                if (rs.getCategory() == InstructionType.InstructionCategory.STORE) {
                    Instruction inst = rs.getInstruction();
                    if (inst != null) {
                        double baseValue = value;
                        int address = (int) baseValue + inst.getImmediate();
                        rs.setVk(String.valueOf(address));
                    }
                } else {
                    // For ALU operations, Qk is the second source register - set value
                    rs.setVk(String.valueOf(value));
                }
            }
        }
    }
    
    public void tick() {
        // Decrement cycles for executing instructions
        for (ReservationStation rs : getAllStations()) {
            if (rs.isBusy() && rs.getCyclesRemaining() > 0) {
                rs.decrementCycles();
            }
        }
    }
    
    public void reset() {
        for (ReservationStation rs : getAllStations()) {
            rs.clear();
        }
    }
}


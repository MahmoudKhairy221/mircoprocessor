package com.example.tomasulo.components;

import com.example.tomasulo.core.Instruction;
import com.example.tomasulo.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoadStoreBuffer {
    private List<LoadStoreEntry> entries;
    private int maxSize;
    
    public LoadStoreBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.entries = new ArrayList<>();
    }
    
    public boolean isFull() {
        return entries.size() >= maxSize;
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    public void reserveEntry(Instruction instruction, String stationName) {
        LoadStoreEntry entry = new LoadStoreEntry();
        entry.setInstruction(instruction);
        entry.setStationName(stationName);
        entry.setIssueCycle(instruction.getIssueCycle());
        entry.setSize(getSizeForInstruction(instruction));
        entry.setCompleted(false);
        entry.setAddressValid(false); // Address not known yet
        
        // Determine type based on instruction
        switch (instruction.getType().getCategory()) {
            case LOAD:
                entry.setType(LoadStoreType.LOAD);
                break;
            case STORE:
                entry.setType(LoadStoreType.STORE);
                break;
            default:
                // Should not happen for LSB
                break;
        }
        
        entries.add(entry);
    }

    public LoadStoreEntry updateLoadAddress(String stationName, int address) {
        LoadStoreEntry entry = getEntryByStation(stationName);
        if (entry != null) {
            entry.setAddress(address);
            entry.setAddressValid(true);
        }
        return entry;
    }
    
    public LoadStoreEntry updateStoreAddressAndValue(String stationName, int address, double value) {
        LoadStoreEntry entry = getEntryByStation(stationName);
        if (entry != null) {
            entry.setAddress(address);
            entry.setValue(value);
            entry.setAddressValid(true);
        }
        return entry;
    }
    
    public LoadStoreEntry addLoad(Instruction instruction, int address, String stationName) {
        if (isFull()) {
            return null;
        }
        
        LoadStoreEntry entry = new LoadStoreEntry();
        entry.setInstruction(instruction);
        entry.setAddress(address);
        entry.setAddressValid(true);
        entry.setStationName(stationName);
        entry.setType(LoadStoreType.LOAD);
        entry.setIssueCycle(instruction.getIssueCycle());
        entry.setSize(getSizeForInstruction(instruction));
        entry.setCompleted(false);
        entries.add(entry);
        return entry;
    }
    
    public LoadStoreEntry addStore(Instruction instruction, int address, String stationName, double value) {
        if (isFull()) {
            return null;
        }
        
        LoadStoreEntry entry = new LoadStoreEntry();
        entry.setInstruction(instruction);
        entry.setAddress(address);
        entry.setAddressValid(true);
        entry.setStationName(stationName);
        entry.setType(LoadStoreType.STORE);
        entry.setValue(value);
        entry.setIssueCycle(instruction.getIssueCycle());
        entry.setSize(getSizeForInstruction(instruction));
        entry.setCompleted(false);
        entries.add(entry);
        return entry;
    }
    
    public List<LoadStoreEntry> getEntries() {
        return entries;
    }
    
    public LoadStoreEntry getEntryByStation(String stationName) {
        for (LoadStoreEntry entry : entries) {
            if (entry.getStationName().equals(stationName)) {
                return entry;
            }
        }
        return null;
    }
    
    public void removeEntry(LoadStoreEntry entry) {
        entries.remove(entry);
    }
    
    /**
     * Checks for address clashes.
     * @param address The address to check
     * @param size The size of the access
     * @param excludeEntry The entry corresponding to the instruction checking for clashes
     * @param type The type of the instruction checking for clashes (LOAD or STORE)
     * @return true if a clash exists, false otherwise
     */
    public boolean hasAddressClash(int address, int size, LoadStoreEntry excludeEntry, LoadStoreType type) {
        for (LoadStoreEntry entry : entries) {
            if (entry == excludeEntry) {
                continue;
            }
            
            // Only check against EARLIER instructions (lower issue cycle)
            // Or if issue cycle is same (program order), we assume entry order in list reflects program order
            if (entry.getIssueCycle() > excludeEntry.getIssueCycle()) {
                continue;
            }

            // Ignore entries that have already completed (no longer impose hazards)
            if (entry.isCompleted()) {
                continue;
            }
            
            // Check if addresses overlap
            if (!entry.isAddressValid()) {
                // If address is unknown, we must assume a potential clash to be safe (conservative disambiguation)
                // Especially if we are a STORE checking against unknown LOAD/STORE, or LOAD checking against unknown STORE
                if (type == LoadStoreType.STORE) {
                    return true; // Store must wait for all earlier instructions to resolve address
                } else {
                    // We are a LOAD
                    if (entry.getType() == LoadStoreType.STORE) {
                        return true; // Load must wait for earlier STORE to resolve address
                    }
                    // Load vs Load is fine even if address unknown (no hazard)
                    continue;
                }
            }

            int entryStart = entry.getAddress();
            int entryEnd = entryStart + entry.getSize();
            int checkStart = address;
            int checkEnd = address + size;
            
            if ((checkStart >= entryStart && checkStart < entryEnd) ||
                (checkEnd > entryStart && checkEnd <= entryEnd) ||
                (checkStart <= entryStart && checkEnd >= entryEnd) ||
                (entryStart >= checkStart && entryStart < checkEnd)) { // Cover case where entry is inside check
                return true;
            }
        }
        return false;
    }
    
    /**
     * Issue-time check for address clashes.
     * This is used before an instruction is even placed into the Load/Store buffer,
     * to prevent issuing memory instructions that would violate ordering with
     * earlier, incomplete memory operations to the same address.
     *
     * @param address     Effective address of the instruction being considered for issue
     * @param size        Size of the memory access
     * @param type        LOAD or STORE for the instruction being issued
     * @param issueCycle  The cycle at which the instruction would be issued
     * @return true if an earlier incomplete memory instruction may clash, false otherwise
     */
    public boolean hasAddressClashAtIssue(int address, int size, LoadStoreType type, int issueCycle) {
        for (LoadStoreEntry entry : entries) {
            // Only check against EARLIER instructions (lower issue cycle)
            if (entry.getIssueCycle() >= issueCycle) {
                continue;
            }

            // Ignore entries that have already completed (no longer impose hazards)
            if (entry.isCompleted()) {
                continue;
            }

            // If earlier instruction's address is unknown, be conservative
            if (!entry.isAddressValid()) {
                if (type == LoadStoreType.STORE) {
                    // A STORE cannot pass any earlier unknown LOAD/STORE
                    return true;
                } else {
                    // We are a LOAD: cannot pass an earlier STORE with unknown address
                    if (entry.getType() == LoadStoreType.STORE) {
                        return true;
                    }
                    // LOAD vs LOAD with unknown address is allowed
                    continue;
                }
            }

            // Both addresses are known; check overlap
            int entryStart = entry.getAddress();
            int entryEnd = entryStart + entry.getSize();
            int checkStart = address;
            int checkEnd = address + size;

            if ((checkStart >= entryStart && checkStart < entryEnd) ||
                (checkEnd > entryStart && checkEnd <= entryEnd) ||
                (checkStart <= entryStart && checkEnd >= entryEnd) ||
                (entryStart >= checkStart && entryStart < checkEnd)) {
                return true;
            }
        }
        return false;
    }
    
    private int getSizeForInstruction(Instruction inst) {
        switch (inst.getType()) {
            case L_D:
            case S_D:
            case LD:
            case SD:
                return 8;
            case L_S:
            case S_S:
            case LW:
            case SW:
                return 4;
            default:
                return 0;
        }
    }
    
    public void reset() {
        entries.clear();
    }

    public LoadStoreBuffer snapshot() {
        LoadStoreBuffer copy = new LoadStoreBuffer(this.maxSize);
        for (LoadStoreEntry entry : this.entries) {
            copy.entries.add(entry.snapshot());
        }
        return copy;
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public enum LoadStoreType {
        LOAD,
        STORE
    }
    
    public static class LoadStoreEntry {
        private Instruction instruction;
        private int address;
        private String stationName;
        private LoadStoreType type;
        private double value; // For stores
        private byte[] loadData; // For loads
        private int issueCycle;
        private int cyclesRemaining;
        private boolean ready;
        private int size;
        private boolean completed;
        private boolean addressValid;
        
        public Instruction getInstruction() {
            return instruction;
        }
        
        public void setInstruction(Instruction instruction) {
            this.instruction = instruction;
        }
        
        public int getAddress() {
            return address;
        }
        
        public void setAddress(int address) {
            this.address = address;
        }
        
        public String getStationName() {
            return stationName;
        }
        
        public void setStationName(String stationName) {
            this.stationName = stationName;
        }
        
        public LoadStoreType getType() {
            return type;
        }
        
        public void setType(LoadStoreType type) {
            this.type = type;
        }
        
        public double getValue() {
            return value;
        }
        
        public void setValue(double value) {
            this.value = value;
        }
        
        public int getIssueCycle() {
            return issueCycle;
        }
        
        public void setIssueCycle(int issueCycle) {
            this.issueCycle = issueCycle;
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
        
        public boolean isReady() {
            return ready;
        }
        
        public void setReady(boolean ready) {
            this.ready = ready;
        }
        
        public byte[] getLoadData() {
            return loadData;
        }
        
        public void setLoadData(byte[] loadData) {
            this.loadData = loadData;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean isAddressValid() {
            return addressValid;
        }

        public void setAddressValid(boolean addressValid) {
            this.addressValid = addressValid;
        }

        public LoadStoreEntry snapshot() {
            LoadStoreEntry copy = new LoadStoreEntry();
            if (this.instruction != null) {
                copy.instruction = this.instruction.copy();
            }
            copy.address = this.address;
            copy.stationName = this.stationName;
            copy.type = this.type;
            copy.value = this.value;
            if (this.loadData != null) {
                copy.loadData = Arrays.copyOf(this.loadData, this.loadData.length);
            }
            copy.issueCycle = this.issueCycle;
            copy.cyclesRemaining = this.cyclesRemaining;
            copy.ready = this.ready;
            copy.size = this.size;
            copy.completed = this.completed;
            copy.addressValid = this.addressValid;
            return copy;
        }
    }
}

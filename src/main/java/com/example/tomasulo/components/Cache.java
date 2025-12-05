package com.example.tomasulo.components;

import com.example.tomasulo.utils.Constants;

import java.util.Arrays;

public class Cache {
    private CacheBlock[] blocks;
    private int cacheSize;
    private int blockSize;
    private int numBlocks;
    private int hitLatency;
    private int missPenalty;
    private byte[] memory; // Main memory
    private int hits;
    private int misses;
    
    public Cache(int cacheSize, int blockSize, int hitLatency, int missPenalty) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.blocks = new CacheBlock[numBlocks];
        this.memory = new byte[Constants.MEMORY_SIZE];
        
        for (int i = 0; i < numBlocks; i++) {
            blocks[i] = new CacheBlock(blockSize);
        }
        
        Arrays.fill(memory, (byte) 0);
        hits = 0;
        misses = 0;
    }
    
    public CacheResult load(int address, int size) {
        // In this model, a single cache block always holds a full value
        // (e.g., 8-byte double) starting exactly at the requested address.
        // The block does not span multiple logical values across blocks.
        byte[] data = new byte[size];

        int cacheIndex = getCacheIndex(address);
        CacheBlock block = blocks[cacheIndex];

        boolean hit = block.isValid()
                && address >= block.getBaseAddress()
                && (address + size) <= (block.getBaseAddress() + block.getBlockSize());

        if (!hit) {
            misses++;
            // Write-back on eviction if needed
            writeBackIfDirty(block);

            // Load a new block starting exactly at this address
            int baseAddress = address;
            byte[] blockData = new byte[blockSize];
            int len = Math.min(blockSize, memory.length - baseAddress);
            if (len > 0) {
                System.arraycopy(memory, baseAddress, blockData, 0, len);
            }

            block.setData(blockData);
            block.setTag(computeTag(address));
            block.setBaseAddress(baseAddress);
            block.setValid(true);
            block.setDirty(false);

            // Entire access is treated as a miss
            System.arraycopy(blockData, 0, data, 0, Math.min(size, blockSize));
            return new CacheResult(false, missPenalty, data);
        } else {
            hits++;
            int offset = address - block.getBaseAddress();
            byte[] chunk = block.readBytes(offset, size);
            System.arraycopy(chunk, 0, data, 0, chunk.length);
            return new CacheResult(true, hitLatency, data);
        }
    }
    
    /**
     * Probes the cache to determine if a store would be a hit or miss,
     * without modifying the cache state. Used for latency calculation.
     */
    public CacheResult probeStore(int address, int size) {
        int cacheIndex = getCacheIndex(address);
        CacheBlock block = blocks[cacheIndex];

        boolean hit = block.isValid()
                && address >= block.getBaseAddress()
                && (address + size) <= (block.getBaseAddress() + block.getBlockSize());

        if (hit) {
            return new CacheResult(true, hitLatency, null);
        } else {
            return new CacheResult(false, missPenalty, null);
        }
    }
    
    public CacheResult store(int address, byte[] data) {
        int size = data.length;

        int cacheIndex = getCacheIndex(address);
        CacheBlock block = blocks[cacheIndex];

        boolean hit = block.isValid()
                && address >= block.getBaseAddress()
                && (address + size) <= (block.getBaseAddress() + block.getBlockSize());

        if (!hit) {
            misses++;
            // Evict and write-back if needed
            writeBackIfDirty(block);

            // Write-allocate: bring the block from memory, starting exactly at this address
            int baseAddress = address;
            byte[] blockData = new byte[blockSize];
            int len = Math.min(blockSize, memory.length - baseAddress);
            if (len > 0) {
                System.arraycopy(memory, baseAddress, blockData, 0, len);
            }

            block.setData(blockData);
            block.setTag(computeTag(address));
            block.setBaseAddress(baseAddress);
            block.setValid(true);
            block.setDirty(false);
        } else {
            hits++;
        }

        // Perform the store into the cache block and mark it dirty
        int offset = address - block.getBaseAddress();
        block.writeBytes(offset, data);
        block.setDirty(true);

        // Write-back cache: do not update main memory now, it will be updated on eviction
        return new CacheResult(hit, hit ? hitLatency : missPenalty, null);
    }
    
    public void writeMemory(int address, byte[] data) {
        for (int i = 0; i < data.length && address + i < memory.length; i++) {
            memory[address + i] = data[i];
        }
    }
    
    /**
     * Initialize memory at a specific address with a numeric value.
     *
     * Semantics:
     * - Interpret {@code value} as an integer.
     * - Lay out the integer in little-endian order across {@code size} bytes,
     *   i.e., the first byte is the least significant 8 bits, the next is the
     *   next 8 bits, etc.
     */
    public void initializeMemory(int address, double value, int size) {
        if (address < 0 || address >= memory.length) {
            return; // Invalid address
        }
        
        long intValue = (long) value;
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((intValue >> (8 * i)) & 0xFF);
        }
        
        writeMemory(address, data);
    }
    
    public byte[] readMemory(int address, int size) {
        byte[] result = new byte[size];
        for (int i = 0; i < size && address + i < memory.length; i++) {
            result[i] = memory[address + i];
        }
        return result;
    }
    
    public int getCacheIndex(int address) {
        int blockNumber = address / blockSize;
        int index = blockNumber % numBlocks;
        // Handle negative indices (can happen with negative addresses)
        if (index < 0) {
            index += numBlocks;
        }
        return index;
    }
    
    public CacheBlock getBlock(int index) {
        if (index >= 0 && index < numBlocks) {
            return blocks[index];
        }
        return null;
    }
    
    public int getNumBlocks() {
        return numBlocks;
    }
    
    public int getHits() {
        return hits;
    }
    
    public int getMisses() {
        return misses;
    }
    
    public double getHitRate() {
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * Compute a tag value for a given address. For direct-mapped caches this
     * effectively captures the high-order bits beyond the index.
     */
    private int computeTag(int address) {
        int blockNumber = address / blockSize;
        return blockNumber / numBlocks;
    }

    /**
     * If the given block is valid and dirty, write its contents back to main
     * memory using its baseAddress as the starting address.
     */
    private void writeBackIfDirty(CacheBlock block) {
        if (!block.isValid() || !block.isDirty()) {
            return;
        }

        int baseAddress = block.getBaseAddress();
        byte[] data = block.getData();

        int len = Math.min(data.length, memory.length - baseAddress);
        if (len > 0) {
            System.arraycopy(data, 0, memory, baseAddress, len);
        }

        block.setDirty(false);
    }
    
    public void reset() {
        for (CacheBlock block : blocks) {
            block.clear();
        }
        // Note: Memory is NOT cleared here to preserve user initialization
        // If you need to clear memory, use resetMemory() method
        hits = 0;
        misses = 0;
    }
    
    /**
     * Reset memory to all zeros. This is separate from reset() to allow
     * memory initialization to persist through cache resets.
     */
    public void resetMemory() {
        Arrays.fill(memory, (byte) 0);
    }
    
    public void setHitLatency(int hitLatency) {
        this.hitLatency = hitLatency;
    }
    
    public void setMissPenalty(int missPenalty) {
        this.missPenalty = missPenalty;
    }
    
    public int getHitLatency() {
        return hitLatency;
    }
    
    public int getMissPenalty() {
        return missPenalty;
    }
    
    public static class CacheResult {
        private boolean hit;
        private int latency;
        private byte[] data;
        
        public CacheResult(boolean hit, int latency, byte[] data) {
            this.hit = hit;
            this.latency = latency;
            this.data = data;
        }
        
        public boolean isHit() {
            return hit;
        }
        
        public int getLatency() {
            return latency;
        }
        
        public byte[] getData() {
            return data;
        }
    }
}

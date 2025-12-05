package com.example.tomasulo.components;

import java.util.Arrays;

public class CacheBlock {
    private byte[] data;
    private int tag;
    private boolean valid;
    private boolean dirty;
    /**
     * Starting memory address covered by this cache block.
     * This allows blocks to start at arbitrary addresses instead of being
     * forced to align on fixed multiples of blockSize.
     */
    private int baseAddress;
    private int blockSize;
    
    public CacheBlock(int blockSize) {
        this.blockSize = blockSize;
        this.data = new byte[blockSize];
        this.tag = -1;
        this.valid = false;
        this.dirty = false;
        this.baseAddress = 0;
        Arrays.fill(data, (byte) 0);
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        if (data.length == blockSize) {
            this.data = Arrays.copyOf(data, data.length);
        } else if (data.length < blockSize) {
             // Partial update or smaller block, handle gracefully by padding or copying valid part
             System.arraycopy(data, 0, this.data, 0, data.length);
        } else {
            // Truncate if too large (should be handled by Cache)
            System.arraycopy(data, 0, this.data, 0, blockSize);
        }
    }
    
    public int getTag() {
        return tag;
    }
    
    public void setTag(int tag) {
        this.tag = tag;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public int getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(int baseAddress) {
        this.baseAddress = baseAddress;
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public void writeByte(int offset, byte value) {
        if (offset >= 0 && offset < blockSize) {
            data[offset] = value;
        }
    }
    
    public byte readByte(int offset) {
        if (offset >= 0 && offset < blockSize) {
            return data[offset];
        }
        return 0;
    }
    
    public void writeBytes(int offset, byte[] values) {
        for (int i = 0; i < values.length && (offset + i) < blockSize; i++) {
            data[offset + i] = values[i];
        }
    }
    
    public byte[] readBytes(int offset, int length) {
        // Ensure we don't read past block end
        int validLength = Math.min(length, blockSize - offset);
        if (validLength <= 0) return new byte[0];
        
        byte[] result = new byte[validLength];
        for (int i = 0; i < validLength; i++) {
            result[i] = data[offset + i];
        }
        return result;
    }
    
    public void clear() {
        Arrays.fill(data, (byte) 0);
        tag = -1;
        valid = false;
        dirty = false;
        baseAddress = 0;
    }
}

package com.example.tomasulo.core;

import com.example.tomasulo.utils.InstructionType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstructionParser {
    private Map<String, Integer> labelMap; // Maps labels to instruction addresses
    
    public InstructionParser() {
        this.labelMap = new HashMap<>();
    }
    
    private List<String> originalLines; // Store original lines with labels for display
    
    public List<Instruction> parseFile(String filename) throws IOException {
        List<Instruction> instructions = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        originalLines = new ArrayList<>(); // Store original lines
        
        // Clear label map before parsing
        labelMap.clear();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String originalLine = line; // Keep original for display
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                    originalLines.add(originalLine.trim()); // Store trimmed original
                }
            }
        }
        
        System.out.println("\n========== PARSING FILE: " + filename + " ==========");
        System.out.println("Raw lines from file:");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("  [" + i + "] '" + lines.get(i) + "'");
        }
        
        // First pass: identify labels
        int address = 0;
        System.out.println("\nFirst pass: Identifying labels...");
        for (String line : lines) {
            int labelIndex = line.indexOf(':');
            if (labelIndex != -1) {
                String label = line.substring(0, labelIndex).trim();
                labelMap.put(label, address);
                System.out.println("  Found label: '" + label + "' at address " + address);
                
                String remaining = line.substring(labelIndex + 1).trim();
                if (!remaining.isEmpty()) {
                    address += 4;
                    System.out.println("    Instruction after label: '" + remaining + "' -> address incremented to " + address);
                } else {
                    System.out.println("    No instruction after label (label-only line)");
                }
            } else {
                System.out.println("  No label in line: '" + line + "' -> address " + address);
                address += 4;
            }
        }
        System.out.println("Label map after first pass: " + labelMap);
        
        // Second pass: parse instructions
        System.out.println("\nSecond pass: Parsing instructions...");
        address = 0;
        for (String line : lines) {
            int labelIndex = line.indexOf(':');
            String instructionPart = line;
            
            if (labelIndex != -1) {
                instructionPart = line.substring(labelIndex + 1).trim();
                System.out.println("  Line with label: '" + line + "' -> instruction part: '" + instructionPart + "'");
            } else {
                System.out.println("  Line without label: '" + line + "'");
            }
            
            if (instructionPart.isEmpty()) {
                System.out.println("    Skipping (empty instruction part)");
                continue;
            }
            
            Instruction inst = parseInstruction(instructionPart, address);
            if (inst != null) {
                System.out.println("    Parsed instruction at address " + address + ": " + inst.toString());
                instructions.add(inst);
                address += 4;
            } else {
                System.out.println("    Failed to parse instruction");
            }
        }
        
        System.out.println("\nTotal instructions parsed: " + instructions.size());
        System.out.println("Label map before resolving branch targets: " + labelMap);
        
        // Resolve branch targets
        resolveBranchTargets(instructions);
        
        System.out.println("==========================================\n");
        return instructions;
    }
    
    public List<Instruction> parseText(String text) {
        List<Instruction> instructions = new ArrayList<>();
        String[] lines = text.split("\n");
        List<String> cleanLines = new ArrayList<>();
        originalLines = new ArrayList<>(); // Store original lines
        
        // Clean and collect lines
        for (String line : lines) {
            String originalLine = line; // Keep original
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                cleanLines.add(line);
                originalLines.add(originalLine.trim()); // Store trimmed original
            }
        }
        
        System.out.println("\n========== PARSING TEXT ==========");
        System.out.println("Raw lines from text:");
        for (int i = 0; i < cleanLines.size(); i++) {
            System.out.println("  [" + i + "] '" + cleanLines.get(i) + "'");
        }
        
        // First pass: identify labels
        labelMap.clear();
        int address = 0;
        System.out.println("\nFirst pass: Identifying labels...");
        for (String line : cleanLines) {
            int labelIndex = line.indexOf(':');
            if (labelIndex != -1) {
                String label = line.substring(0, labelIndex).trim();
                labelMap.put(label, address);
                System.out.println("  Found label: '" + label + "' at address " + address);
                
                // Check if there's code after the label
                String remaining = line.substring(labelIndex + 1).trim();
                if (!remaining.isEmpty()) {
                    address += 4;
                    System.out.println("    Instruction after label: '" + remaining + "' -> address incremented to " + address);
                } else {
                    System.out.println("    No instruction after label (label-only line)");
                }
            } else {
                System.out.println("  No label in line: '" + line + "' -> address " + address);
                address += 4;
            }
        }
        System.out.println("Label map after first pass: " + labelMap);
        
        // Second pass: parse instructions
        System.out.println("\nSecond pass: Parsing instructions...");
        address = 0;
        for (String line : cleanLines) {
            int labelIndex = line.indexOf(':');
            String instructionPart = line;
            
            if (labelIndex != -1) {
                instructionPart = line.substring(labelIndex + 1).trim();
                System.out.println("  Line with label: '" + line + "' -> instruction part: '" + instructionPart + "'");
            } else {
                System.out.println("  Line without label: '" + line + "'");
            }
            
            if (instructionPart.isEmpty()) {
                System.out.println("    Skipping (empty instruction part - label-only line)");
                continue; // Label only line
            }
            
            Instruction inst = parseInstruction(instructionPart, address);
            if (inst != null) {
                System.out.println("    Parsed instruction at address " + address + ": " + inst.toString());
                instructions.add(inst);
                address += 4;
            } else {
                System.out.println("    Failed to parse instruction");
            }
        }
        
        System.out.println("\nTotal instructions parsed: " + instructions.size());
        System.out.println("Label map before resolving branch targets: " + labelMap);
        
        // Resolve branch targets
        resolveBranchTargets(instructions);
        
        System.out.println("==========================================\n");
        return instructions;
    }
    
    private Instruction parseInstruction(String line, int address) {
        // Remove comments
        int commentIndex = line.indexOf('#');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
        }
        
        if (line.isEmpty()) {
            return null;
        }
        
        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        
        String mnemonic = parts[0].toUpperCase();
        InstructionType type = findInstructionType(mnemonic);
        
        if (type == null) {
            return null;
        }
        
        Instruction inst = new Instruction(type);
        inst.setInstructionAddress(address);
        
        try {
            switch (type.getCategory()) {
                case FP_ADD_SUB:
                case FP_MUL_DIV:
                    // Format: OP Fd, Fs1, Fs2
                    if (parts.length >= 4) {
                        inst.setDestRegister(parts[1].replace(",", ""));
                        inst.setSrcRegister1(parts[2].replace(",", ""));
                        inst.setSrcRegister2(parts[3]);
                    }
                    break;
                    
                case INTEGER_ALU:
                    // Format: OP Rd, Rs, imm or OP Rd, Rs, imm
                    if (parts.length >= 4) {
                        inst.setDestRegister(parts[1].replace(",", ""));
                        inst.setSrcRegister1(parts[2].replace(",", ""));
                        inst.setImmediate(Integer.parseInt(parts[3]));
                    }
                    break;
                    
                case LOAD:
                    // Format: OP Fd/Rd, imm(Rs)
                    if (parts.length >= 3) {
                        inst.setDestRegister(parts[1].replace(",", ""));
                        parseLoadStoreAddress(parts[2], inst);
                    }
                    break;
                    
                case STORE:
                    // Format: OP Fs/Rs, imm(Rs)
                    if (parts.length >= 3) {
                        inst.setSrcRegister1(parts[1].replace(",", ""));
                        parseLoadStoreAddress(parts[2], inst);
                    }
                    break;
                    
                case BRANCH:
                    // Format: OP Rs1, Rs2, target
                    if (parts.length >= 4) {
                        inst.setSrcRegister1(parts[1].replace(",", "").trim());
                        inst.setSrcRegister2(parts[2].replace(",", "").trim());
                        inst.setBranchTarget(parts[3].trim());
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error parsing instruction: " + line + " - " + e.getMessage());
            return null;
        }
        
        return inst;
    }
    
    private void parseLoadStoreAddress(String addrStr, Instruction inst) {
        // Format: imm(Rs) or imm(Rs)
        Pattern pattern = Pattern.compile("(-?\\d+)\\(([RF]\\d+)\\)");
        Matcher matcher = pattern.matcher(addrStr);
        
        if (matcher.matches()) {
            inst.setImmediate(Integer.parseInt(matcher.group(1)));
            inst.setBaseRegister(matcher.group(2));
        }
    }
    
    private InstructionType findInstructionType(String mnemonic) {
        for (InstructionType type : InstructionType.values()) {
            if (type.getMnemonic().equalsIgnoreCase(mnemonic)) {
                return type;
            }
        }
        return null;
    }
    
    private void resolveBranchTargets(List<Instruction> instructions) {
        System.out.println("\n========== RESOLVING BRANCH TARGETS ==========");
        System.out.println("Label map contents: " + labelMap);
        
        for (Instruction inst : instructions) {
            if (inst.getType().getCategory() == InstructionType.InstructionCategory.BRANCH) {
                String target = inst.getBranchTarget();
                System.out.println("\nBranch instruction: " + inst.toString() + 
                    " at address " + inst.getInstructionAddress());
                System.out.println("  Target string: '" + target + "'");
                
                if (target != null) {
                    target = target.trim(); // Trim whitespace
                    System.out.println("  Trimmed target: '" + target + "'");
                }
                
                if (labelMap.containsKey(target)) {
                    // Convert label to address offset
                    int targetAddress = labelMap.get(target);
                    int currentAddress = inst.getInstructionAddress();
                    int offset = (targetAddress - currentAddress - 4) / 4; // Branch offset in instructions
                    inst.setImmediate(offset);
                    System.out.println("  ✓ Label found! targetAddress=" + targetAddress + 
                        ", currentAddress=" + currentAddress + ", offset=" + offset);
                } else {
                    System.out.println("  ✗ Label '" + target + "' NOT FOUND in labelMap!");
                    System.out.println("  Available labels: " + labelMap.keySet());
                    // Try to parse as immediate offset
                    try {
                        int parsedOffset = Integer.parseInt(target);
                        inst.setImmediate(parsedOffset);
                        System.out.println("  Parsed as immediate offset: " + parsedOffset);
                    } catch (NumberFormatException e) {
                        System.out.println("  ERROR: Could not parse as offset, keeping as label string");
                        // Keep as label string - but this will cause issues!
                        inst.setImmediate(0); // Default to 0 if label not found
                    }
                }
            }
        }
        System.out.println("==========================================\n");
    }
    
    public Map<String, Integer> getLabelMap() {
        return labelMap;
    }
    
    /**
     * Get the original lines with labels preserved for display
     * @return List of original lines as they appeared in the file
     */
    public List<String> getOriginalLines() {
        return originalLines != null ? new ArrayList<>(originalLines) : new ArrayList<>();
    }
}


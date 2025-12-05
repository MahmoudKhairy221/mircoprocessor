# How to Run the Tomasulo Algorithm Simulator

## ✅ Verified: All Required Instructions Are Implemented

All 20 required instructions are fully implemented:
- ✅ DADDI, DSUBI
- ✅ ADD.D, ADD.S, SUB.D, SUB.S, MUL.D, MUL.S, DIV.D, DIV.S
- ✅ LW, LD, L.S, L.D
- ✅ SW, SD, S.S, S.D
- ✅ BNE, BEQ

## Quick Start Guide

### Step 1: Build the Project

Open PowerShell or Command Prompt in the project directory and run:

```bash
.\gradlew.bat build
```

This will compile all Java files and create the executable JAR file.

### Step 2: Run the Application

Run the application using Gradle:

```bash
.\gradlew.bat run
```

**OR** run the JAR file directly:

```bash
java -jar build\libs\javafx-test-1.0-SNAPSHOT.jar
```

The GUI window should open automatically.

## Using the Simulator

### 1. Load Instructions

**Option A: Load from File**
1. Click the **"Load File"** button
2. Select one of the test case files:
   - `test_case_1.txt` - Sequential code with RAW hazards
   - `test_case_2.txt` - Sequential code with multiple dependencies  
   - `test_case_3.txt` - Loop code with branches
3. Instructions will be loaded and displayed

**Option B: Enter Instructions Manually**
1. Type instructions in the text area (one per line)
2. Example:
   ```
   L.D F6, 0(R2)
   ADD.D F7, F1, F3
   MUL.D F0, F2, F4
   ```
3. Click **"Load Instructions from Text"**

### 2. Configure Settings (Optional)

In the right panel, you can configure:
- **Cache Size**: Total cache size in bytes (default: 1024)
- **Block Size**: Cache block size in bytes (default: 8)
- **Hit Latency**: Cycles for cache hit (default: 1)
- **Miss Penalty**: Cycles for cache miss (default: 10)

Click **"Apply Cache Settings"** after making changes.

### 3. Run the Simulation

**Step-by-Step Execution:**
- Click **"Step"** to execute one cycle at a time
- Watch the tables update after each step
- Check the cycle counter at the top

**Automatic Execution:**
- Click **"Run"** to automatically execute cycles (500ms delay between cycles)
- Click **"Stop"** (button changes to Stop) to pause execution

**Reset:**
- Click **"Reset"** to restart the simulation from cycle 0

### 4. View Results

The GUI displays 5 main tables:

1. **Instruction Queue** (Top Left)
   - Shows all instructions
   - Displays Issue, Execute Start, Execute End, Write Back cycles
   - Highlights current instruction being processed

2. **Reservation Stations** (Top Right)
   - Shows status of all reservation stations
   - Displays: Name, Busy, Operation, Vj, Vk, Qj, Qk, Destination, Cycles Remaining
   - Separate stations for FP Add/Sub, FP Mul/Div, Integer ALU, Load, Store

3. **Register File** (Bottom Left)
   - Shows all integer registers (R0-R31) and FP registers (F0-F31)
   - Displays current values and tags (reservation station names)

4. **Cache** (Bottom Center)
   - Shows cache blocks with index, valid bit, tag, and data
   - Displays cache statistics: Hits, Misses, Hit Rate

5. **Load/Store Buffer** (Bottom Right)
   - Shows pending memory operations
   - Displays: Station name, Type (Load/Store), Address, Value, Cycles Remaining

## Example Instruction Formats

### Integer Operations
```
DADDI R1, R1, 24
DSUBI R1, R1, 8
```

### Floating Point Operations
```
ADD.D F6, F8, F2
SUB.D F8, F2, F6
MUL.D F0, F2, F4
DIV.D F10, F0, F6
ADD.S F1, F2, F3
```

### Load Operations
```
L.D F6, 0(R2)      # Load double (8 bytes)
L.S F2, 4(R2)      # Load single (4 bytes)
LW R1, 100(R2)     # Load word (4 bytes)
LD R1, 200(R2)     # Load double (8 bytes)
```

### Store Operations
```
S.D F6, 8(R2)      # Store double (8 bytes)
S.S F2, 4(R2)      # Store single (4 bytes)
SW R1, 100(R2)     # Store word (4 bytes)
SD R1, 200(R2)     # Store double (8 bytes)
```

### Branch Operations
```
BEQ R1, R2, LOOP   # Branch if R1 == R2
BNE R1, R2, LOOP   # Branch if R1 != R2
LOOP: L.D F0, 8(R1)  # Label definition
```

## Troubleshooting

### Application Won't Start
- Make sure Java 22 is installed: `java -version`
- Check that Gradle wrapper exists: `.\gradlew.bat --version`

### Instructions Not Parsing
- Check instruction format matches examples above
- Make sure register names are correct (R0-R31, F0-F31)
- Labels must end with colon (e.g., `LOOP:`)

### GUI Not Displaying Properly
- Ensure JavaFX is properly installed
- Try resizing the window
- Check that all tables are visible in the split panes

## Test Cases

Three test case files are included:

**test_case_1.txt:**
```
L.D F6, 0(R2)
L.D F2, 8(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
ADD.D F6, F8, F2
S.D F6, 8(R2)
```

**test_case_2.txt:**
```
L.D F6, 0(R2)
ADD.D F7, F1, F3
L.D F2, 20(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
S.D F10, 0(R2)
```

**test_case_3.txt:**
```
DADDI R1, R1, 24
DADDI R2, R2, 0
LOOP: L.D F0, 8(R1)
MUL.D F4, F0, F2
S.D F4, 8(R1)
DSUBI R1, R1, 8
BNE R1, R2, LOOP
```

## Tips for Best Results

1. **Start with test_case_1.txt** to see basic sequential execution
2. **Use Step mode** initially to understand cycle-by-cycle behavior
3. **Watch the Reservation Stations** to see how instructions wait for operands
4. **Check Register Tags** to see which reservation station will write to each register
5. **Observe Cache Hits/Misses** in the cache statistics
6. **Use test_case_3.txt** to see branch handling and loops

## Need Help?

- Check the README.md for detailed architecture information
- Review the code comments in the source files
- Test with the provided test cases first before creating custom code














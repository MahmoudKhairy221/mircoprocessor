package com.example.tomasulo.gui;

import com.example.tomasulo.core.*;
import com.example.tomasulo.components.*;
import com.example.tomasulo.utils.Constants;
import com.example.tomasulo.utils.InstructionType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class TomasuloGUI extends Application {
    private TomasuloSimulator simulator;
    private InstructionParser parser;
    
    // Control Panel
    private Button loadFileButton;
    private Button stepButton;
    private Button runButton;
    private Button resetButton;
    private Label cycleLabel;
    private Label statusLabel;
    private TextArea instructionInput;
    
    // Configuration Panel
    private Accordion configAccordion;
    private TextField cacheSizeField;
    private TextField blockSizeField;
    private TextField hitLatencyField;
    private TextField missPenaltyField;
    
    // Station Sizes
    private TextField fpAddSubSizeField;
    private TextField fpMulDivSizeField;
    private TextField intAluSizeField;
    private TextField loadSizeField;
    private TextField storeSizeField;
    
    // Initial Registers
    private TextArea initialRegField;
    
    // Memory Initialization
    private TextArea memoryInitField;
    
    // Latency Fields map
    private java.util.Map<String, TextField> latencyFields = new java.util.HashMap<>();
    
    // Display Tables
    private ReservationStationTable rsTable;
    private RegisterFileTable registerTable;
    private CacheTable cacheTable;
    private LoadStoreBufferTable lsBufferTable;
    private InstructionListTable instructionTable;
    
    // Log
    private TextArea executionLog;
    
    private boolean running = false;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            simulator = new TomasuloSimulator();
            parser = new InstructionParser();
            
            primaryStage.setTitle("Tomasulo Algorithm Simulator");
            primaryStage.setWidth(1400);
            primaryStage.setHeight(900);
            
            // Create main layout
            BorderPane root = new BorderPane();
            
            // Top: Control Panel
            root.setTop(createControlPanel());
            
            // Center: Main display area
            root.setCenter(createMainDisplay());
            
            // Right: Configuration Panel
            root.setRight(createConfigurationPanel());
            
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                Platform.exit();
                System.exit(0);
            });
            primaryStage.show();
            
            // Apply initializations after GUI is shown
            // Always apply (even if empty) to ensure registers are initialized correctly
            Platform.runLater(() -> {
                applyInitialRegisters(); // Always apply, even if field is empty (will set to 0)
                applyMemoryInitializationFromField(); // Always apply, even if field is empty
                updateDisplay();
            });
            
            System.out.println("Tomasulo Simulator GUI started successfully!");
        } catch (Exception e) {
            System.err.println("Error starting GUI: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        
        // Initialize display
        updateDisplay();
    }
    
    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        
        loadFileButton = new Button("Load File");
        loadFileButton.setOnAction(e -> loadInstructionsFromFile());
        
        stepButton = new Button("Step");
        stepButton.setOnAction(e -> stepSimulation());
        
        runButton = new Button("Run");
        runButton.setOnAction(e -> toggleRun());
        
        resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetSimulation());
        
        cycleLabel = new Label("Cycle: 0");
        statusLabel = new Label("Status: Ready");
        
        controlPanel.getChildren().addAll(
            loadFileButton, stepButton, runButton, resetButton,
            new Separator(), cycleLabel, statusLabel
        );
        
        return controlPanel;
    }
    
    private VBox createMainDisplay() {
        VBox mainDisplay = new VBox(10);
        mainDisplay.setPadding(new Insets(10));
        
        // Instruction input/display
        Label instructionLabel = new Label("Instructions:");
        instructionInput = new TextArea();
        instructionInput.setPrefRowCount(5);
        instructionInput.setPromptText("Enter MIPS instructions here or load from file...\nExample:\nL.D F6, 0(R2)\nADD.D F7, F1, F3");
        
        Button loadTextButton = new Button("Load Instructions from Text");
        loadTextButton.setOnAction(e -> loadInstructionsFromText());
        
        VBox instructionPanel = new VBox(5);
        instructionPanel.getChildren().addAll(instructionLabel, instructionInput, loadTextButton);

        // Execution Log
        Label logLabel = new Label("Execution Log:");
        executionLog = new TextArea();
        executionLog.setEditable(false);
        executionLog.setPrefRowCount(5);
        
        VBox logPanel = new VBox(5);
        logPanel.getChildren().addAll(logLabel, executionLog);
        
        TabPane inputLogTabPane = new TabPane();
        Tab inputTab = new Tab("Instructions", instructionPanel);
        inputTab.setClosable(false);
        Tab logTab = new Tab("Execution Log", logPanel);
        logTab.setClosable(false);
        inputLogTabPane.getTabs().addAll(inputTab, logTab);
        
        // Create tables
        instructionTable = new InstructionListTable();
        rsTable = new ReservationStationTable();
        registerTable = new RegisterFileTable();
        cacheTable = new CacheTable();
        lsBufferTable = new LoadStoreBufferTable();
        
        // Split panes for layout
        SplitPane topSplit = new SplitPane();
        topSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        topSplit.getItems().addAll(instructionTable.getView(), rsTable.getView());
        topSplit.setDividerPositions(0.4);
        
        SplitPane bottomSplit = new SplitPane();
        bottomSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        bottomSplit.getItems().addAll(registerTable.getView(), cacheTable.getView(), lsBufferTable.getView());
        bottomSplit.setDividerPositions(0.33, 0.66);
        
        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplit.getItems().addAll(inputLogTabPane, topSplit, bottomSplit);
        mainSplit.setDividerPositions(0.25, 0.6);
        
        mainDisplay.getChildren().add(mainSplit);
        
        return mainDisplay;
    }
    
    private ScrollPane createConfigurationPanel() {
        VBox configContent = new VBox(10);
        configContent.setPadding(new Insets(10));
        
        Label configLabel = new Label("Configuration");
        configLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 1. Cache Configuration
        TitledPane cachePane = new TitledPane();
        cachePane.setText("Cache Settings");
        VBox cacheConfig = new VBox(5);
        cacheConfig.setPadding(new Insets(5));
        
        cacheSizeField = new TextField(String.valueOf(Constants.DEFAULT_CACHE_SIZE));
        blockSizeField = new TextField(String.valueOf(Constants.DEFAULT_BLOCK_SIZE));
        hitLatencyField = new TextField(String.valueOf(Constants.DEFAULT_CACHE_HIT_LATENCY));
        missPenaltyField = new TextField(String.valueOf(Constants.DEFAULT_CACHE_MISS_PENALTY));
        
        cacheConfig.getChildren().addAll(
            new Label("Cache Size (bytes):"), cacheSizeField,
            new Label("Block Size (bytes):"), blockSizeField,
            new Label("Hit Latency (cycles):"), hitLatencyField,
            new Label("Miss Penalty (cycles):"), missPenaltyField
        );
        cachePane.setContent(cacheConfig);
        
        // 2. Station Sizes
        TitledPane stationPane = new TitledPane();
        stationPane.setText("Buffer Sizes");
        VBox stationConfig = new VBox(5);
        stationConfig.setPadding(new Insets(5));
        
        fpAddSubSizeField = new TextField(String.valueOf(Constants.DEFAULT_FP_ADD_SUB_STATIONS));
        fpMulDivSizeField = new TextField(String.valueOf(Constants.DEFAULT_FP_MUL_DIV_STATIONS));
        intAluSizeField = new TextField(String.valueOf(Constants.DEFAULT_INTEGER_ALU_STATIONS));
        loadSizeField = new TextField(String.valueOf(Constants.DEFAULT_LOAD_STATIONS));
        storeSizeField = new TextField(String.valueOf(Constants.DEFAULT_STORE_STATIONS));
        
        stationConfig.getChildren().addAll(
            new Label("FP Add/Sub Stations:"), fpAddSubSizeField,
            new Label("FP Mul/Div Stations:"), fpMulDivSizeField,
            new Label("Integer ALU Stations:"), intAluSizeField,
            new Label("Load Buffers:"), loadSizeField,
            new Label("Store Buffers:"), storeSizeField
        );
        stationPane.setContent(stationConfig);
        
        // 3. Instruction Latencies
        TitledPane latencyPane = new TitledPane();
        latencyPane.setText("Instruction Latencies");
        VBox latencyConfig = new VBox(5);
        latencyConfig.setPadding(new Insets(5));
        
        // Group by category for display, but allow individual editing
        for (InstructionType type : InstructionType.values()) {
            HBox row = new HBox(5);
            row.setAlignment(Pos.CENTER_LEFT);
            Label label = new Label(type.getMnemonic() + ":");
            label.setPrefWidth(60);
            TextField field = new TextField(String.valueOf(type.getDefaultLatency()));
            field.setPrefWidth(60);
            latencyFields.put(type.getMnemonic(), field);
            row.getChildren().addAll(label, field);
            latencyConfig.getChildren().add(row);
        }
        latencyPane.setContent(latencyConfig);
        
        // 4. Initial Registers
        TitledPane regPane = new TitledPane();
        regPane.setText("Initial Registers");
        VBox regConfig = new VBox(5);
        regConfig.setPadding(new Insets(5));
        
        initialRegField = new TextArea();
        initialRegField.setPromptText("R1=10\nF2=20.5\nF6=100");
        initialRegField.setPrefRowCount(5);
        // Start with empty - all registers initialize to 0 by default
        initialRegField.setText("");
        
        regConfig.getChildren().addAll(new Label("Enter values (e.g. R1=10):"), initialRegField);
        regPane.setContent(regConfig);
        
        // 5. Memory Initialization
        TitledPane memoryPane = new TitledPane();
        memoryPane.setText("Memory Initialization");
        VBox memoryConfig = new VBox(5);
        memoryConfig.setPadding(new Insets(5));
        
        memoryInitField = new TextArea();
        memoryInitField.setPromptText("0=10.5\n8=20.0\n16=100.0\n32=3.14159");
        memoryInitField.setPrefRowCount(5);
        // Start with empty - all memory initializes to 0 by default
        memoryInitField.setText("");
        
        memoryConfig.getChildren().addAll(
            new Label("Enter address=value (e.g. 0=10.5):"),
            memoryInitField
        );
        memoryPane.setContent(memoryConfig);
        
        // Accordion
        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(cachePane, stationPane, latencyPane, regPane, memoryPane);
        accordion.setExpandedPane(cachePane);
        
        // Display current initialized values
        VBox currentValuesBox = new VBox(10);
        currentValuesBox.setPadding(new Insets(10));
        currentValuesBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1;");
        
        Label currentValuesLabel = new Label("Current Initialized Values:");
        currentValuesLabel.setStyle("-fx-font-weight: bold;");
        
        Label currentRegLabel = new Label("Registers: (see Initial Registers section)");
        currentRegLabel.setWrapText(true);
        
        Label currentMemLabel = new Label("Memory: (see Memory Initialization section)");
        currentMemLabel.setWrapText(true);
        
        // Update labels when fields change
        initialRegField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                currentRegLabel.setText("Registers: " + newVal.replace("\n", ", "));
            } else {
                currentRegLabel.setText("Registers: None");
            }
        });
        
        memoryInitField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                currentMemLabel.setText("Memory: " + newVal.replace("\n", ", "));
            } else {
                currentMemLabel.setText("Memory: None");
            }
        });
        
        // Initialize labels with current values
        if (initialRegField.getText() != null && !initialRegField.getText().trim().isEmpty()) {
            currentRegLabel.setText("Registers: " + initialRegField.getText().replace("\n", ", "));
        }
        if (memoryInitField.getText() != null && !memoryInitField.getText().trim().isEmpty()) {
            currentMemLabel.setText("Memory: " + memoryInitField.getText().replace("\n", ", "));
        }
        
        currentValuesBox.getChildren().addAll(currentValuesLabel, currentRegLabel, currentMemLabel);
        
        Button applyButton = new Button("Apply Configuration");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setStyle("-fx-base: #4CAF50;"); // Green color
        applyButton.setOnAction(e -> applyConfiguration());
        
        configContent.getChildren().addAll(configLabel, currentValuesBox, accordion, applyButton);
        
        ScrollPane scrollPane = new ScrollPane(configContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(320);
        
        return scrollPane;
    }
    
    private void loadInstructionsFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load MIPS Instructions");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                List<Instruction> instructions = parser.parseFile(file.getAbsolutePath());
                simulator.loadInstructions(instructions);
                // Re-apply registers after loadInstructions (which calls reset)
                applyInitialRegisters();
                applyMemoryInitializationFromField();
                // Use original lines with labels preserved instead of Instruction.toString()
                List<String> originalLines = parser.getOriginalLines();
                instructionInput.setText(String.join("\n", originalLines));
                updateDisplay();
                statusLabel.setText("Status: Instructions loaded from file");
            } catch (Exception e) {
                showError("Error loading file", e.getMessage());
            }
        }
    }
    
    private void loadInstructionsFromText() {
        String text = instructionInput.getText();
        if (text.isEmpty()) {
            showError("Error", "Please enter instructions");
            return;
        }
        
        try {
            List<Instruction> instructions = parser.parseText(text);
            simulator.loadInstructions(instructions);
            // Re-apply registers after loadInstructions (which calls reset)
            applyInitialRegisters();
            applyMemoryInitializationFromField();
            updateDisplay();
            statusLabel.setText("Status: Instructions loaded from text");
        } catch (Exception e) {
            showError("Error parsing instructions", e.getMessage());
        }
    }
    
    private void stepSimulation() {
        if (simulator.getState().isSimulationComplete()) {
            statusLabel.setText("Status: Simulation complete");
            return;
        }
        
        simulator.step();
        updateDisplay();
        
        ExecutionState state = simulator.getState();
        cycleLabel.setText("Cycle: " + state.getCurrentCycle());
        statusLabel.setText("Status: " + state.getStatusMessage());
    }
    
    private void toggleRun() {
        if (running) {
            running = false;
            runButton.setText("Run");
        } else {
            running = true;
            runButton.setText("Stop");
            runSimulation();
        }
    }
    
    private void runSimulation() {
        new Thread(() -> {
            while (running && !simulator.getState().isSimulationComplete()) {
                Platform.runLater(() -> stepSimulation());
                try {
                    Thread.sleep(500); // 500ms delay between steps
                } catch (InterruptedException e) {
                    break;
                }
            }
            Platform.runLater(() -> {
                running = false;
                runButton.setText("Run");
            });
        }).start();
    }
    
    private void resetSimulation() {
        simulator.reset();
        // Re-apply registers after reset as simulator.reset() clears them
        applyInitialRegisters();
        // Re-apply memory initialization after reset as cache.reset() clears memory
        applyMemoryInitializationFromField();
        updateDisplay();
        cycleLabel.setText("Cycle: 0");
        statusLabel.setText("Status: Reset");
    }
    
    private void applyMemoryInitialization() {
        // Use the field from configuration panel
        applyMemoryInitializationFromField();
    }
    
    private void applyMemoryInitializationFromField() {
        if (memoryInitField == null) {
            return;
        }
        
        String text = memoryInitField.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        
        try {
            String[] lines = text.split("\n");
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    int address = Integer.parseInt(parts[0].trim());
                    double value = Double.parseDouble(parts[1].trim());
                    
                    // Determine size based on address alignment (default to 8 bytes for double)
                    // User can specify size by using format: address:size=value
                    int size = 8; // Default to double precision
                    String addrStr = parts[0].trim();
                    if (addrStr.contains(":")) {
                        String[] addrParts = addrStr.split(":");
                        address = Integer.parseInt(addrParts[0].trim());
                        size = Integer.parseInt(addrParts[1].trim());
                    }
                    
                    simulator.getCache().initializeMemory(address, value, size);
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error applying memory initialization: " + e.getMessage());
        }
    }
    
    private void applyInitialRegisters() {
        if (simulator == null) return;
        
        // First, reset all registers to 0
        simulator.getRegisterFile().reset();
        
        // Then apply user-specified values if any
        String regText = initialRegField.getText();
        if (regText != null && !regText.trim().isEmpty()) {
            String[] lines = regText.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    try {
                        String reg = parts[0].trim();
                        double val = Double.parseDouble(parts[1].trim());
                        simulator.getRegisterFile().setValue(reg, val);
                    } catch (Exception ex) {
                        System.err.println("Invalid register line: " + line);
                    }
                }
            }
        }
        // Note: Don't call updateDisplay() here - let caller handle it to avoid double updates
    }

    private void applyConfiguration() {
        try {
            // 1. Cache Settings
            int cacheSize = Integer.parseInt(cacheSizeField.getText());
            int blockSize = Integer.parseInt(blockSizeField.getText());
            int hitLatency = Integer.parseInt(hitLatencyField.getText());
            int missPenalty = Integer.parseInt(missPenaltyField.getText());
            
            // 2. Station Sizes
            int fpAddSub = Integer.parseInt(fpAddSubSizeField.getText());
            int fpMulDiv = Integer.parseInt(fpMulDivSizeField.getText());
            int intAlu = Integer.parseInt(intAluSizeField.getText());
            int load = Integer.parseInt(loadSizeField.getText());
            int store = Integer.parseInt(storeSizeField.getText());
            
            // Recreate simulator with new settings
            simulator = new TomasuloSimulator(cacheSize, blockSize);
            simulator.getCache().setHitLatency(hitLatency);
            simulator.getCache().setMissPenalty(missPenalty);
            
            simulator.initializeStations(fpAddSub, fpMulDiv, intAlu, load, store);
            
            // 3. Latencies
            for (java.util.Map.Entry<String, TextField> entry : latencyFields.entrySet()) {
                try {
                    int latency = Integer.parseInt(entry.getValue().getText());
                    simulator.setInstructionLatency(entry.getKey(), latency);
                } catch (NumberFormatException e) {
                    // Ignore invalid latency inputs or use defaults
                    System.err.println("Invalid latency for " + entry.getKey());
                }
            }
            
            // 4. Initial Registers - apply immediately after simulator creation
            applyInitialRegisters();
            
            // 5. Memory Initialization
            applyMemoryInitializationFromField();
            
            // Reload instructions if present
            String text = instructionInput.getText();
            if (!text.isEmpty()) {
                try {
                    List<Instruction> instructions = parser.parseText(text);
                    simulator.loadInstructions(instructions);
                    // After loading instructions, re-apply registers (loadInstructions calls reset)
                    applyInitialRegisters();
                    applyMemoryInitializationFromField();
                } catch (Exception ex) {
                    // Ignore parse error here, will be caught when loading explicitly
                }
            }
            
            // Force display update on JavaFX thread
            Platform.runLater(() -> {
                updateDisplay();
                statusLabel.setText("Status: Configuration applied");
            });
            
        } catch (NumberFormatException e) {
            showError("Configuration Error", "Please enter valid integer values for sizes and latencies.");
        } catch (Exception e) {
            showError("Configuration Error", "Error applying settings: " + e.getMessage());
        }
    }
    
    private void showPreloadDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Pre-load Registers");
        dialog.setHeaderText("Enter register values (e.g., R1=10, F2=20.5)");
        
        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);
        
        TextArea textArea = new TextArea();
        textArea.setPromptText("R1=10\nF2=20.5\nF6=100");
        dialog.getDialogPane().setContent(textArea);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return textArea.getText();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(text -> {
            try {
                String[] lines = text.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String reg = parts[0].trim();
                        double val = Double.parseDouble(parts[1].trim());
                        simulator.getRegisterFile().setValue(reg, val);
                    }
                }
                updateDisplay();
                statusLabel.setText("Status: Registers pre-loaded");
            } catch (Exception e) {
                showError("Error", "Invalid register format: " + e.getMessage());
        }
        });
    }
    
    private void showInitMemoryDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Initialize Memory");
        dialog.setHeaderText("Enter memory address and value (e.g., 0=10.5, 8=20.0, 16=100.0)\nAddress is in bytes. Use 4 bytes for float, 8 bytes for double.");
        
        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);
        
        // This method is deprecated - use Memory Initialization in Configuration Panel instead
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Memory Initialization");
        alert.setHeaderText("Use Configuration Panel");
        alert.setContentText("Please use the 'Memory Initialization' section in the Configuration Panel (right side) to initialize memory addresses.");
        alert.showAndWait();
    }
    
    private void updateDisplay() {
        if (simulator == null) return;
        
        List<Instruction> displayInstructions = simulator.getState().getTrace();
        // If trace is empty (initial state), show the static program
        if (displayInstructions.isEmpty() && simulator.getState().getCurrentCycle() == 0) {
            displayInstructions = simulator.getInstructions();
        }
        
        instructionTable.update(displayInstructions, simulator.getState());
        rsTable.update(simulator.getRsManager());
        registerTable.update(simulator.getRegisterFile());
        cacheTable.update(simulator.getCache());
        lsBufferTable.update(simulator.getLoadStoreBuffer());
        
        // Update Log
        List<String> log = simulator.getState().getExecutionLog();
        executionLog.setText(String.join("\n", log));
        executionLog.setScrollTop(Double.MAX_VALUE); // Auto-scroll to bottom
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

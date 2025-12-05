package com.example.tomasulo.gui;

import com.example.tomasulo.core.ExecutionState;
import com.example.tomasulo.core.Instruction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

public class InstructionListTable {
    private TableView<InstructionData> tableView;
    private ObservableList<InstructionData> data;
    
    public InstructionListTable() {
        tableView = new TableView<>();
        data = FXCollections.observableArrayList();
        
        TableColumn<InstructionData, String> pcCol = new TableColumn<>("PC");
        pcCol.setCellValueFactory(new PropertyValueFactory<>("pc"));
        
        TableColumn<InstructionData, String> instructionCol = new TableColumn<>("Instruction");
        instructionCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        
        TableColumn<InstructionData, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(new PropertyValueFactory<>("issueCycle"));
        
        TableColumn<InstructionData, String> execStartCol = new TableColumn<>("Exec Start");
        execStartCol.setCellValueFactory(new PropertyValueFactory<>("executeStartCycle"));
        
        TableColumn<InstructionData, String> execEndCol = new TableColumn<>("Exec End");
        execEndCol.setCellValueFactory(new PropertyValueFactory<>("executeEndCycle"));
        
        TableColumn<InstructionData, String> wbCol = new TableColumn<>("Write Back");
        wbCol.setCellValueFactory(new PropertyValueFactory<>("writeBackCycle"));
        
        TableColumn<InstructionData, String> completeCol = new TableColumn<>("Complete");
        completeCol.setCellValueFactory(new PropertyValueFactory<>("completed"));
        
        tableView.getColumns().addAll(pcCol, instructionCol, issueCol, execStartCol, execEndCol, wbCol, completeCol);
        tableView.setItems(data);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    public void update(List<Instruction> instructions, ExecutionState state) {
        data.clear();
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            InstructionData instData = new InstructionData();
            instData.setPc(String.format("0x%04X", inst.getInstructionAddress()));
            instData.setInstruction(inst.toString());
            instData.setIssueCycle(inst.getIssueCycle() >= 0 ? String.valueOf(inst.getIssueCycle()) : "");
            instData.setExecuteStartCycle(inst.getExecuteStartCycle() >= 0 ? String.valueOf(inst.getExecuteStartCycle()) : "");
            instData.setExecuteEndCycle(inst.getExecuteEndCycle() >= 0 ? String.valueOf(inst.getExecuteEndCycle()) : "");
            instData.setWriteBackCycle(inst.getWriteBackCycle() >= 0 ? String.valueOf(inst.getWriteBackCycle()) : "");
            instData.setCompleted(inst.isCompleted() ? "Yes" : "No");
            
            // Highlight current instruction
            if (i == state.getInstructionPointer() && !state.isSimulationComplete()) {
                instData.setCurrent(true);
            }
            
            data.add(instData);
        }
    }
    
    public VBox getView() {
        Label title = new Label("Instruction Queue");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox vbox = new VBox(5);
        vbox.getChildren().addAll(title, tableView);
        return vbox;
    }
    
    public static class InstructionData {
        private String pc;
        private String instruction;
        private String issueCycle;
        private String executeStartCycle;
        private String executeEndCycle;
        private String writeBackCycle;
        private String completed;
        private boolean current;
        
        public String getPc() { return pc; }
        public void setPc(String pc) { this.pc = pc; }
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        public String getIssueCycle() { return issueCycle; }
        public void setIssueCycle(String issueCycle) { this.issueCycle = issueCycle; }
        public String getExecuteStartCycle() { return executeStartCycle; }
        public void setExecuteStartCycle(String executeStartCycle) { this.executeStartCycle = executeStartCycle; }
        public String getExecuteEndCycle() { return executeEndCycle; }
        public void setExecuteEndCycle(String executeEndCycle) { this.executeEndCycle = executeEndCycle; }
        public String getWriteBackCycle() { return writeBackCycle; }
        public void setWriteBackCycle(String writeBackCycle) { this.writeBackCycle = writeBackCycle; }
        public String getCompleted() { return completed; }
        public void setCompleted(String completed) { this.completed = completed; }
        public boolean isCurrent() { return current; }
        public void setCurrent(boolean current) { this.current = current; }
    }
}














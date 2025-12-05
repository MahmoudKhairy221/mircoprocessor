package com.example.tomasulo.gui;

import com.example.tomasulo.components.RegisterFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.Map;

public class RegisterFileTable {
    private TableView<RegisterData> tableView;
    private ObservableList<RegisterData> data;
    
    public RegisterFileTable() {
        tableView = new TableView<>();
        data = FXCollections.observableArrayList();
        
        TableColumn<RegisterData, String> nameCol = new TableColumn<>("Register");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<RegisterData, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        TableColumn<RegisterData, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));
        
        tableView.getColumns().addAll(nameCol, valueCol, tagCol);
        tableView.setItems(data);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    public void update(RegisterFile registerFile) {
        data.clear();
        
        // Add integer registers in order R0-R31
        for (int i = 0; i < 32; i++) {
            String regName = "R" + i;
            RegisterData regData = new RegisterData();
            regData.setName(regName);
            regData.setValue(String.format("%.2f", registerFile.getValue(regName)));
            regData.setTag(registerFile.getTag(regName));
            data.add(regData);
        }
        
        // Add FP registers in order F0-F31
        for (int i = 0; i < 32; i++) {
            String regName = "F" + i;
            RegisterData regData = new RegisterData();
            regData.setName(regName);
            regData.setValue(String.format("%.2f", registerFile.getValue(regName)));
            regData.setTag(registerFile.getTag(regName));
            data.add(regData);
        }
    }
    
    public VBox getView() {
        Label title = new Label("Register File");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox vbox = new VBox(5);
        vbox.getChildren().addAll(title, tableView);
        return vbox;
    }
    
    public static class RegisterData {
        private String name;
        private String value;
        private String tag;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getTag() { return tag != null ? tag : ""; }
        public void setTag(String tag) { this.tag = tag; }
    }
}











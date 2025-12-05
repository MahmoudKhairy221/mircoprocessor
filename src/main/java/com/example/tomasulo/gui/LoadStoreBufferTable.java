package com.example.tomasulo.gui;

import com.example.tomasulo.components.LoadStoreBuffer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class LoadStoreBufferTable {
    private TableView<LSBufferData> tableView;
    private ObservableList<LSBufferData> data;
    
    public LoadStoreBufferTable() {
        tableView = new TableView<>();
        data = FXCollections.observableArrayList();
        
        TableColumn<LSBufferData, String> stationCol = new TableColumn<>("Station");
        stationCol.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        
        TableColumn<LSBufferData, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<LSBufferData, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        
        TableColumn<LSBufferData, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        TableColumn<LSBufferData, String> cyclesCol = new TableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cyclesRemaining"));
        
        tableView.getColumns().addAll(stationCol, typeCol, addressCol, valueCol, cyclesCol);
        tableView.setItems(data);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    public void update(LoadStoreBuffer buffer) {
        data.clear();
        
        for (LoadStoreBuffer.LoadStoreEntry entry : buffer.getEntries()) {
            LSBufferData entryData = new LSBufferData();
            entryData.setStationName(entry.getStationName());
            entryData.setType(entry.getType().toString());
            entryData.setAddress(String.valueOf(entry.getAddress()));
            entryData.setValue(entry.getType() == LoadStoreBuffer.LoadStoreType.STORE ?
                    String.format("%.2f", entry.getValue()) : "");
            entryData.setCyclesRemaining(String.valueOf(entry.getCyclesRemaining()));
            
            data.add(entryData);
        }
    }
    
    public VBox getView() {
        Label title = new Label("Load/Store Buffer");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox vbox = new VBox(5);
        vbox.getChildren().addAll(title, tableView);
        return vbox;
    }
    
    public static class LSBufferData {
        private String stationName;
        private String type;
        private String address;
        private String value;
        private String cyclesRemaining;
        
        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getCyclesRemaining() { return cyclesRemaining; }
        public void setCyclesRemaining(String cyclesRemaining) { this.cyclesRemaining = cyclesRemaining; }
    }
}














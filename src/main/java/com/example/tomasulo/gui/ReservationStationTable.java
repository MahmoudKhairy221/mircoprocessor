package com.example.tomasulo.gui;

import com.example.tomasulo.components.ReservationStation;
import com.example.tomasulo.components.ReservationStationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ReservationStationTable {
    private TableView<ReservationStationData> tableView;
    private ObservableList<ReservationStationData> data;
    
    public ReservationStationTable() {
        tableView = new TableView<>();
        data = FXCollections.observableArrayList();
        
        TableColumn<ReservationStationData, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<ReservationStationData, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));
        
        TableColumn<ReservationStationData, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(new PropertyValueFactory<>("operation"));
        
        TableColumn<ReservationStationData, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(new PropertyValueFactory<>("vj"));
        
        TableColumn<ReservationStationData, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(new PropertyValueFactory<>("vk"));
        
        TableColumn<ReservationStationData, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(new PropertyValueFactory<>("qj"));
        
        TableColumn<ReservationStationData, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(new PropertyValueFactory<>("qk"));
        
        TableColumn<ReservationStationData, String> destCol = new TableColumn<>("Dest");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        
        TableColumn<ReservationStationData, String> cyclesCol = new TableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cyclesRemaining"));
        
        tableView.getColumns().addAll(nameCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol, destCol, cyclesCol);
        tableView.setItems(data);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    public void update(ReservationStationManager rsManager) {
        data.clear();
        
        for (ReservationStation rs : rsManager.getAllStations()) {
            ReservationStationData rsData = new ReservationStationData();
            rsData.setName(rs.getName());
            boolean busy = rs.isBusy();
            rsData.setBusy(busy ? "Yes" : "No");
            if (busy) {
                rsData.setOperation(rs.getOperation() != null ? rs.getOperation() : "");
                rsData.setVj(rs.getVj() != null ? rs.getVj() : "");
                rsData.setVk(rs.getVk() != null ? rs.getVk() : "");
                rsData.setQj(rs.getQj() != null ? rs.getQj() : "");
                rsData.setQk(rs.getQk() != null ? rs.getQk() : "");
                rsData.setDestination(rs.getDestination() != null ? rs.getDestination() : "");
                rsData.setCyclesRemaining(String.valueOf(rs.getCyclesRemaining()));
            } else {
                rsData.setOperation("");
                rsData.setVj("");
                rsData.setVk("");
                rsData.setQj("");
                rsData.setQk("");
                rsData.setDestination("");
                rsData.setCyclesRemaining("0");
            }
            
            data.add(rsData);
        }
    }
    
    public VBox getView() {
        Label title = new Label("Reservation Stations");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox vbox = new VBox(5);
        vbox.getChildren().addAll(title, tableView);
        return vbox;
    }
    
    public static class ReservationStationData {
        private String name;
        private String busy;
        private String operation;
        private String vj;
        private String vk;
        private String qj;
        private String qk;
        private String destination;
        private String cyclesRemaining;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBusy() { return busy; }
        public void setBusy(String busy) { this.busy = busy; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getVj() { return vj; }
        public void setVj(String vj) { this.vj = vj; }
        public String getVk() { return vk; }
        public void setVk(String vk) { this.vk = vk; }
        public String getQj() { return qj; }
        public void setQj(String qj) { this.qj = qj; }
        public String getQk() { return qk; }
        public void setQk(String qk) { this.qk = qk; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getCyclesRemaining() { return cyclesRemaining; }
        public void setCyclesRemaining(String cyclesRemaining) { this.cyclesRemaining = cyclesRemaining; }
    }
}











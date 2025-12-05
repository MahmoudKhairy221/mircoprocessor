package com.example.tomasulo.gui;

import com.example.tomasulo.components.Cache;
import com.example.tomasulo.components.CacheBlock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class CacheTable {
    private TableView<CacheBlockData> tableView;
    private ObservableList<CacheBlockData> data;
    private Label statsLabel;
    
    public CacheTable() {
        tableView = new TableView<>();
        data = FXCollections.observableArrayList();
        
        TableColumn<CacheBlockData, String> indexCol = new TableColumn<>("Index");
        indexCol.setCellValueFactory(new PropertyValueFactory<>("index"));
        
        TableColumn<CacheBlockData, String> validCol = new TableColumn<>("Valid");
        validCol.setCellValueFactory(new PropertyValueFactory<>("valid"));
        
        TableColumn<CacheBlockData, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));

        TableColumn<CacheBlockData, String> baseAddrCol = new TableColumn<>("Base Addr");
        baseAddrCol.setCellValueFactory(new PropertyValueFactory<>("baseAddress"));

        TableColumn<CacheBlockData, String> dirtyCol = new TableColumn<>("Dirty");
        dirtyCol.setCellValueFactory(new PropertyValueFactory<>("dirty"));
        
        TableColumn<CacheBlockData, String> hexCol = new TableColumn<>("Hex");
        hexCol.setCellValueFactory(new PropertyValueFactory<>("hexValue"));
        
        tableView.getColumns().addAll(indexCol, validCol, tagCol, baseAddrCol, dirtyCol, hexCol);
        tableView.setItems(data);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        statsLabel = new Label();
    }
    
    public void update(Cache cache) {
        data.clear();
        
        for (int i = 0; i < cache.getNumBlocks(); i++) {
            CacheBlock block = cache.getBlock(i);
            CacheBlockData blockData = new CacheBlockData();
            blockData.setIndex(String.valueOf(i));
            blockData.setValid(block.isValid() ? "Yes" : "No");
            blockData.setTag(block.isValid() ? String.valueOf(block.getTag()) : "");
            blockData.setBaseAddress(block.isValid() ? String.valueOf(block.getBaseAddress()) : "");
            blockData.setDirty(block.isValid() && block.isDirty() ? "Yes" : "No");
            
            if (block.isValid()) {
                byte[] bytes = block.getData();

                // Interpret the block as a little-endian unsigned integer.
                long intValue = 0L;
                int len = Math.min(8, bytes.length);
                for (int j = 0; j < len; j++) {
                    intValue |= (long) (bytes[j] & 0xFF) << (8 * j);
                }

                // Hex representation of the same 64-bit integer.
                blockData.setHexValue(String.format("0x%016X", intValue));
            } else {
                blockData.setHexValue("");
            }
            
            data.add(blockData);
        }
        
        // Update statistics
        statsLabel.setText(String.format("Hits: %d, Misses: %d, Hit Rate: %.2f%%",
                cache.getHits(), cache.getMisses(), cache.getHitRate() * 100));
    }
    
    public VBox getView() {
        Label title = new Label("Cache");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox vbox = new VBox(5);
        vbox.getChildren().addAll(title, tableView, statsLabel);
        return vbox;
    }
    
    public static class CacheBlockData {
        private String index;
        private String valid;
        private String tag;
            private String baseAddress;
            private String dirty;
        private String data;       // Binary representation
        private String hexValue;   // Hex representation
        private String decimalValue;
        
        public String getIndex() { return index; }
        public void setIndex(String index) { this.index = index; }
        public String getValid() { return valid; }
        public void setValid(String valid) { this.valid = valid; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
            public String getBaseAddress() { return baseAddress; }
            public void setBaseAddress(String baseAddress) { this.baseAddress = baseAddress; }
            public String getDirty() { return dirty; }
            public void setDirty(String dirty) { this.dirty = dirty; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getHexValue() { return hexValue; }
        public void setHexValue(String hexValue) { this.hexValue = hexValue; }
        public String getDecimalValue() { return decimalValue; }
        public void setDecimalValue(String decimalValue) { this.decimalValue = decimalValue; }
    }
}





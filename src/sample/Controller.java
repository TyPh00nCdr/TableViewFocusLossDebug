package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Arrays;

public class Controller {
    @FXML private TableView<String> table;
    @FXML private TableColumn<String, String> column1;
    @FXML private TableColumn<String, String> column2;
    @FXML private TableColumn<String, String> column3;

    @FXML
    private void initialize() {
        final ObservableList<String> items = FXCollections.observableList(
                Arrays.asList("String1", "String2", "String3"));

        table.setItems(items);
        column1.setCellFactory(ComboBoxTableCellFocusLoss.forTableColumn(items));
        column2.setCellFactory(ComboBoxTableCellFocusLoss.forTableColumn(items));
        column3.setCellFactory(ComboBoxTableCellFocusLoss.forTableColumn(items));
    }

}

package sample;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ComboBoxTableCellFocusLoss<S, T> extends TableCell<S, T> {

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final ObservableList<T> items) {
        return list -> new ComboBoxTableCellFocusLoss<S, T>(items);
    }

    //---------------------------------------------------------------------------------------------------------------

    private final static StringConverter<?> defaultStringConverter = new StringConverter<Object>() {
        @Override
        public String toString(Object t) {
            return t == null ? null : t.toString();
        }

        @Override
        public Object fromString(String string) {
            return (Object) string;
        }
    };

    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");

    private BooleanProperty comboBoxEditable = new SimpleBooleanProperty(this, "comboBoxEditable");

    private final ObservableList<T> items;

    private ComboBox<T> comboBox;


    public ComboBoxTableCellFocusLoss(ObservableList<T> items) {
        this.getStyleClass().add("combo-box-table-cell");
        this.items = items;
        setConverter(defaultStringConverter());
    }


    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    public final BooleanProperty comboBoxEditableProperty() {
        return comboBoxEditable;
    }

    public final void setComboBoxEditable(boolean value) {
        comboBoxEditableProperty().set(value);
    }

    public final boolean isComboBoxEditable() {
        return comboBoxEditableProperty().get();
    }

    public ObservableList<T> getItems() {
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        if (comboBox == null) {
            comboBox = createComboBox(this, items, converterProperty());
            comboBox.editableProperty().bind(comboBoxEditableProperty());
        }

        comboBox.getSelectionModel().select(getItem());

        super.startEdit();
        setText(null);
        setGraphic(comboBox);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText(getConverter().toString(getItem()));
        setGraphic(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        updateItem(this, getConverter(), null, null, comboBox);
    }

    /****************************
     * CellUtils Static Methods *
     ****************************/

    static <T> void updateItem(Cell<T> cell, StringConverter<T> converter, ComboBox<T> comboBox) {
        updateItem(cell, converter, null, null, comboBox);
    }

    static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final HBox hbox,
                               final Node graphic,
                               final ComboBox<T> comboBox) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (comboBox != null) {
                    comboBox.getSelectionModel().select(cell.getItem());
                }
                cell.setText(null);

                if (graphic != null) {
                    hbox.getChildren().setAll(graphic, comboBox);
                    cell.setGraphic(hbox);
                } else {
                    cell.setGraphic(comboBox);
                }
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    }

    static <T> ComboBox<T> createComboBox(final Cell<T> cell,
                                          final ObservableList<T> items,
                                          final ObjectProperty<StringConverter<T>> converter) {
        ComboBox<T> comboBox = new ComboBox<T>(items);
        comboBox.converterProperty().bind(converter);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        // setup listeners to properly commit any changes back into the data model.
        // First listener attempts to commit or cancel when the ENTER or ESC keys are released.
        // This is applicable in cases where the ComboBox is editable, and the user has
        // typed some input, and also when the ComboBox popup is showing.
        comboBox.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                tryComboBoxCommit(comboBox, cell);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
            }
        });

        // Second listener attempts to commit when the user is in the editor of
        // the ComboBox, and moves focus away.
        comboBox.getEditor().focusedProperty().addListener(o -> {
            if (!comboBox.isFocused()) {
                tryComboBoxCommit(comboBox, cell);
            }
        });

        // Third listener makes an assumption about the skin being used, and attempts to add
        // a listener to the ListView within it, such that when the user mouse clicks on a
        // on an item, that is immediately committed and the cell exits the editing mode.
        boolean success = listenToComboBoxSkin(comboBox, cell);
        if (!success) {
            comboBox.skinProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    boolean successInListener = listenToComboBoxSkin(comboBox, cell);
                    if (successInListener) {
                        comboBox.skinProperty().removeListener(this);
                    }
                }
            });
        }

        return comboBox;
    }

    @SuppressWarnings("unchecked")
    static <T> StringConverter<T> defaultStringConverter() {
        return (StringConverter<T>) defaultStringConverter;
    }

    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
                cell.getItem() == null ? "" : cell.getItem().toString() :
                converter.toString(cell.getItem());
    }

    private static <T> void tryComboBoxCommit(ComboBox<T> comboBox, Cell<T> cell) {
        StringConverter<T> sc = comboBox.getConverter();
        if (comboBox.isEditable() && sc != null) {
            T value = sc.fromString(comboBox.getEditor().getText());
            cell.commitEdit(value);
        } else {
            cell.commitEdit(comboBox.getValue());
        }
    }

    private static <T> boolean listenToComboBoxSkin(final ComboBox<T> comboBox, final Cell<T> cell) {
        Skin<?> skin = comboBox.getSkin();
        if (skin != null && skin instanceof ComboBoxListViewSkin) {
            ComboBoxListViewSkin cbSkin = (ComboBoxListViewSkin) skin;
            Node popupContent = cbSkin.getPopupContent();
            if (popupContent != null && popupContent instanceof ListView) {
                popupContent.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> cell.commitEdit(comboBox.getValue()));
                return true;
            }
        }
        return false;
    }
}

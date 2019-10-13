package io.proximax.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.StringUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;

/**
 *
 * @author thcao
 */
public class CategoryDialog extends AbstractController {

    @FXML
    private JFXTextField folderField;

    @FXML
    private ListView<String> folderLv;

    @FXML
    private JFXButton addFolderBtn;

    private LocalAccount localAccount = null;

    private boolean modified = false;

    public CategoryDialog(LocalAccount localAccount) {
        super(true);
        this.localAccount = localAccount;
    }

    @Override
    protected void initialize() {
        ObservableList<String> obList = FXCollections.observableList(LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network));
        obList.remove(CONST.HOME);
        folderLv.setItems(obList);
    }

    @Override
    protected void dispose() {

    }

    @Override
    public String getTitle() {
        return CONST.CATEGORYDLG_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.CATEGORYDLG_FXML;
    }

    @FXML
    void saveBtn(ActionEvent event) {
        if (modified) {
            setButtonType(ButtonType.OK);
        } else {
            setButtonType(ButtonType.CLOSE);
        }
        close();
    }

    @FXML
    void addFolderBtn(ActionEvent event) {
        try {
            String str = folderField.getText().trim();
            if (StringUtils.isValidFileName(str)) {
                if (!folderLv.getItems().contains(str)) {
                    folderLv.getItems().add(str);
                    LocalFileHelpers.addFolder(localAccount.fullName, localAccount.network, str);
                    modified = true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void removeFolderBtn(ActionEvent event) {
        int idx = folderLv.getSelectionModel().getSelectedIndex();
        if (idx != -1) {
            String str = folderLv.getSelectionModel().getSelectedItem();            
            if (LocalFileHelpers.delFolder(localAccount, str)) {
                LocalFileHelpers.moveFilesNewFolder(localAccount, str, CONST.HOME);
            }
            folderLv.getItems().remove(idx);
            modified = true;
        }
    }

    @FXML
    void upFolderBtn(ActionEvent event) {
        int idx = folderLv.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            String item1 = folderLv.getSelectionModel().getSelectedItem();
            String item2 = folderLv.getItems().get(idx - 1);
            folderLv.getItems().set(idx - 1, item1);
            folderLv.getItems().set(idx, item2);
            folderLv.getSelectionModel().select(idx - 1);

            modified = true;
        }
    }

}

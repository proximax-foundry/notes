package io.proximax.app.controller;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.StringUtils;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/**
 *
 * @author thcao
 */
public class FolderDialog extends AbstractController {

    @FXML
    private JFXTextField folderField;
    @FXML
    private Label titleLbl;
    @FXML
    private Label folderLbl;
    @FXML
    private Button folderBtn;
    @FXML
    private JFXComboBox<String> folderCbx;

    private String folder;
    private LocalAccount localAccount;
    private LocalFile localFile = null;
    private boolean bRemove = false;

    public FolderDialog(LocalAccount localAccount, String folder) {
        super(true);
        this.folder = folder;
        this.localFile = null;
        this.bRemove = false;
        this.localAccount = localAccount;
    }

    public FolderDialog(LocalAccount localAccount, LocalFile localFile) {
        super(true);
        this.localFile = localFile;
        this.bRemove = false;
        this.localAccount = localAccount;
    }

    public FolderDialog(LocalAccount localAccount, boolean bRemove) {
        super(true);
        this.localAccount = localAccount;
        this.bRemove = bRemove;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (bRemove) {
            titleLbl.setText("DELETE A FOLDER");
            folderBtn.setText("DELETE");
            folderField.setVisible(false);
            folderCbx.setVisible(true);
            ObservableList<String> obList = FXCollections.observableList(LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network));
            obList.remove(CONST.HOME);
            folderCbx.setItems(obList);
            if (!obList.isEmpty()) {
                folderCbx.setValue(obList.get(0));
            }
        } else if (localFile == null) {
            if (!StringUtils.isEmpty(folder)) {
                folderField.setText(folder);
            }
            titleLbl.setText("NEW FOLDER");
            folderBtn.setText("CREATE");
            folderField.setVisible(true);
            folderCbx.setVisible(false);
        } else {
            titleLbl.setText("MOVE TO FOLDER");
            folderBtn.setText("MOVE");
            folderField.setVisible(false);
            folderCbx.setVisible(true);
            ObservableList<String> obList = FXCollections.observableList(LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network));
            folderCbx.setItems(obList);
            folderCbx.setValue(obList.get(0));
        }
    }

    public static boolean isValidName(String text) {
        Pattern pattern = Pattern.compile(
                "# Match a valid Windows filename (unspecified file system).          \n"
                + "^                                # Anchor to start of string.        \n"
                + "(?!                              # Assert filename is not: CON, PRN, \n"
                + "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n"
                + "    CON|PRN|AUX|NUL|             # COM5, COM6, COM7, COM8, COM9,     \n"
                + "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n"
                + "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n"
                + "  (?:\\.[^.]*)?                  # followed by optional extension    \n"
                + "  $                              # and end of string                 \n"
                + ")                                # End negative lookahead assertion. \n"
                + "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n"
                + "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]  # Last char is not a space or dot.  \n"
                + "$                                # Anchor to end of string.            ",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(text);
        boolean isMatch = matcher.matches();
        return isMatch;
    }

    @FXML
    private void folderBtn(ActionEvent event) {
        try {
            if (bRemove) {
                String sFolder = folderCbx.getValue();
                if (LocalFileHelpers.delFolder(localAccount, sFolder)) {
                    LocalFileHelpers.moveFilesNewFolder(localAccount, sFolder, CONST.HOME);
                }
            } else if (localFile == null) {
                String nFolder = folderField.getText();
                if (StringUtils.isEmpty(nFolder)) {
                    throw new Exception("Folder name cannot be empty.");
                }
                if (!isValidName(nFolder)) {
                    throw new Exception("Folder cannot have special character");
                }
                List<String> folders = LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network);
                if (folders.contains(nFolder)) {
                    throw new Exception("Folder existed");
                }
                LocalFileHelpers.addFolder(localAccount.fullName, localAccount.network, nFolder);
            } else {
                String sFolder = folderCbx.getValue();
                if (!sFolder.equals(localFile.category)) {
                    LocalFileHelpers.moveFileFolder(localAccount, localFile.id, sFolder);
                    localFile.category = sFolder;
                }
            }
            setButtonType(ButtonType.OK);
            IApp.runSafe(() -> {
                close();
                showParent();
            });
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    @Override
    protected void dispose() {

    }

    @Override
    public String getTitle() {
        return CONST.FOLDERDLG_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.FOLDERDLG_FXML;
    }

}

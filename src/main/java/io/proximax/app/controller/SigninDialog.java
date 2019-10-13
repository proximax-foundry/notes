package io.proximax.app.controller;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.fx.control.text.ProxinoteController;
import io.proximax.app.recovery.AccountInfo;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.NetworkUtils;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class SigninDialog extends AbstractController {

    @FXML
    private JFXComboBox<String> networkCbx;
    @FXML
    private CheckBox prvkeyChk;
    @FXML
    private JFXTextField prvkeyField;
    @FXML
    private CheckBox termsChk;
    @FXML
    private CheckBox networkChk;
    @FXML
    private Button submitBtn;
    @FXML
    private Label errorLbl;
    @FXML
    private Button btnRecovery;
    private AccountInfo accountInfo = null;
    private String userName = System.getProperty("user.name");
    private boolean isLogin = false;
    private String network = NetworkUtils.NETWORK_DEFAULT;

    public SigninDialog() {
        super(false);
        if (network.equals(NetworkUtils.NONE_NET)) {
            network = NetworkUtils.TEST_NET;
        }
        isLogin = AccountHelpers.isExistAccount(userName, network);
    }

    public SigninDialog(String network) {
        super(false);
        this.network = network;
        isLogin = AccountHelpers.isExistAccount(userName, network);
    }

    @Override
    public void initialize() {
        ObservableList<String> obList = FXCollections.observableList(NetworkUtils.NETWORK_SUPPORT);
        networkCbx.setItems(obList);
        if (obList.contains(network)) {
            networkCbx.setValue(network);

            if (network.equals(NetworkUtils.NETWORK_DEFAULT)) {
                networkChk.selectedProperty().setValue(true);
            }
        } else {
            network = obList.get(0);
            networkCbx.setValue(network);
        }

        if (NetworkUtils.NETWORKS.size() <= 1) {
            networkCbx.setDisable(true);
        }
        isLogin = AccountHelpers.isExistAccount(userName, network);
        if (!isLogin) {
            prvkeyChk.setVisible(!isLogin);
            prvkeyField.setVisible(!isLogin);
            btnRecovery.setVisible(!isLogin);
            termsChk.setVisible(!isLogin);
            btnRecovery.setVisible(!isLogin);
            prvkeyField.disableProperty().bind(prvkeyChk.selectedProperty().not());
            btnRecovery.disableProperty().bind(prvkeyChk.selectedProperty().not());
            submitBtn.disableProperty().bind(termsChk.selectedProperty().not());
        }
        networkCbx.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!network.equals(newValue)) {
                boolean needLogin = AccountHelpers.isExistAccount(userName, newValue);
                if ((needLogin && !isLogin) || (!needLogin && isLogin)) {
                    try {
                        SigninDialog dlg = new SigninDialog(newValue);
                        dlg.openWindow();
                        hide();
                    } catch (Exception ex) {
                    }
                } else {
                    if (newValue.equals(NetworkUtils.NETWORK_DEFAULT)) {
                        networkChk.selectedProperty().setValue(true);
                    } else {
                        networkChk.selectedProperty().setValue(false);
                    }
                    network = newValue;
                }
            }
        });
    }

    @FXML
    void networkBtn(ActionEvent event) {
        try {
            NetworkDialog dlg = new NetworkDialog(NetworkUtils.TEST_NET);
            dlg.setParent(this);
            dlg.openWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void signinBtn(ActionEvent event) {
        try {
            String network = networkCbx.getValue();
            LocalAccount account = AccountHelpers.login(userName, network, userName);
            if (networkChk.isSelected()) {
                NetworkUtils.NETWORK_DEFAULT = network;
            } else {
                NetworkUtils.NETWORK_DEFAULT = NetworkUtils.NONE_NET;
            }
            NetworkUtils.updateNetworkDefault();
            hide();
            ProxinoteController.showDialog(account);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError(this, e.getMessage());
        }

    }

    @FXML
    void signupBtn(ActionEvent event) {
        try {
            String network = networkCbx.getValue();
            String privateKey = null;
            if (prvkeyChk.isSelected()) {
                privateKey = prvkeyField.getText();
                //need search db in network
            }
            LocalAccount account = AccountHelpers.createAccount(userName, network, userName, privateKey);
            if (account == null) {
                ErrorDialog.showError(this, "Cannot create account");
                return;
            }
            if (accountInfo != null) {
                AccountHelpers.updateAccountInfo(account, accountInfo);
            }
            if (networkChk.isSelected()) {
                NetworkUtils.NETWORK_DEFAULT = network;
            } else {
                NetworkUtils.NETWORK_DEFAULT = NetworkUtils.NONE_NET;
            }
            NetworkUtils.updateNetworkDefault();
            hide();
            ProxinoteController.showDialog(account);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError(this, e.getMessage());
        }

    }

    @Override
    protected void dispose() {
        IApp.exit(0);
    }

    public static void showDialog() {
        try {
            SigninDialog dialog = new SigninDialog();
            dialog.setResizable(false);
            dialog.openWindow();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void recoveryBtn(ActionEvent event) {
        try {
            errorLbl.setText("");
            RecoveryDialog dlg = new RecoveryDialog();
            dlg.setParent(this);
            dlg.openWindow();
            accountInfo = null;
            if (dlg.getResultType() == ButtonType.OK) {
                accountInfo = dlg.getAccountInfo();
                prvkeyField.setText(dlg.getAccountInfo().getPrivateKey());
            }
        } catch (Exception ex) {

        }

    }

    @Override
    public void show() {
        reloadTheme();
        super.show();
    }

    @Override
    public String getTitle() {
        return CONST.SIGNIN_TITLE;
    }

    @Override
    public String getFXML() {
        if (isLogin) {
            return CONST.SIGNIN_FXML;
        } else {
            return CONST.SIGNUP_FXML;
        }

    }

}

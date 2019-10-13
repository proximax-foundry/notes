package io.proximax.app.main;

import io.proximax.app.controller.SigninDialog;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.fx.control.text.ProxinoteController;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.NetworkUtils;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Notes extends Application implements IApp {

    private Stage primaryStage = null;
    private int theme = 0;
    private Map<String, Object> caches = new HashMap<>();
    private Image iconApp = null;

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public Image getIcon() {
        if (iconApp == null) {
            iconApp = new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + CONST.PROXINOTE_ICON));
        }
        return iconApp;
    }

    public Image getImageFromResource(String resUrl) {
        return new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + resUrl));
    }

    public Image getImageFromResource(String resUrl, double w, double h) {
        return new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + resUrl), w, h, true, true);
    }

    @Override
    public String getCurrentTheme() {
        return CONST.THEMES[theme];
    }

    @Override
    public String getCurrentThemeUrl() {
        return getClass().getResource(String.format(CONST.CSS_THEME, getCurrentTheme())).toExternalForm();
    }

    @Override
    public String getThemeUrl(int i) {
        return getClass().getResource(String.format(CONST.CSS_THEME, CONST.THEMES[i])).toExternalForm();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void initializeAccount() {
    }

    private LocalAccount localAccount = null;

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        CONST.IAPP = this;
        this.primaryStage = primaryStage;
        primaryStage.getIcons().add(getIcon());
        AccountHelpers.initUserHome();
        //startX(primaryStage);
        if (NetworkUtils.NETWORK_SUPPORT.contains(NetworkUtils.NETWORK_DEFAULT)) {
            String user = System.getProperty("user.name");
            if (AccountHelpers.isExistAccount(user, NetworkUtils.NETWORK_DEFAULT)) {
                startX(primaryStage);
                return;
            }
        }
        SigninDialog.showDialog();
    }

    public void startX(Stage primaryStage) throws InterruptedException {
        try {
            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Init application",
                    ButtonType.CANCEL
            );
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getIcon());
            alert.setTitle("Init application");
            alert.setHeaderText("Launching... ");
            ProgressIndicator progressIndicator = new ProgressIndicator();
            alert.setGraphic(progressIndicator);
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            alert.setX((bounds.getWidth() - 400.0) / 2);
            alert.setY((bounds.getHeight() - 200.0) / 2);
            Task<Void> task = new Task<Void>() {
                final int N_ITERATIONS = 6;

                {
                    setOnFailed(a -> {
                        alert.close();
                        updateMessage("Failed");
                    });
                    setOnSucceeded(a -> {
                        alert.close();
                        updateMessage("Succeeded");
                        ProxinoteController.showDialog(localAccount);
                    });
                    setOnCancelled(a -> {
                        alert.close();
                        updateMessage("Cancelled");
                    });
                }

                @Override
                protected Void call() throws Exception {
                    updateProgress(2, N_ITERATIONS);
                    String user = System.getProperty("user.name");
                    try {
                        if (!StringUtils.isEmpty(user)) {
                            updateProgress(4, N_ITERATIONS);
                            localAccount = AccountHelpers.login(user, NetworkUtils.NETWORK_DEFAULT, user);
                        }
                        updateProgress(N_ITERATIONS, N_ITERATIONS);
                    } catch (Exception ex) {
                    }
                    return null;
                }
            };
            progressIndicator.progressProperty()
                    .bind(task.progressProperty());
            Thread taskThread = new Thread(
                    task,
                    "task-thread-1"
            );
            taskThread.start();
            //alert.initOwner(primaryStage);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()
                    && result.get() == ButtonType.CANCEL && task.isRunning()) {
                task.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Run monitor tool
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void dispose() {
    }

    public String getString(String key) {
        return (String) caches.get(key);
    }

    public void putString(String key, String val) {
        caches.put(key, val);
    }

    public String getCurrentDir() {
        String dir = (String) caches.get("latest.dir");
        if (StringUtils.isEmpty(dir)) {
            dir = System.getProperty("user.home");
        }
        return dir;
    }

    public File getCurrentFolder() {
        return new File(getCurrentDir());
    }

    public void saveCurrentDir(String sDir) {
        caches.put("latest.dir", sDir);
    }

}

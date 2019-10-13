package io.proximax.app.fx.control.text;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import io.proximax.sdk.infrastructure.Listener;
import io.proximax.sdk.model.account.Address;
import io.proximax.sdk.model.transaction.Transaction;
import io.proximax.sdk.model.transaction.TransferTransaction;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.controller.AbstractController;
import io.proximax.app.controller.CategoryDialog;
import io.proximax.app.controller.ErrorDialog;
import io.proximax.app.controller.FolderDialog;
import io.proximax.app.controller.NetworkDialog;
import io.proximax.app.controller.UserProfileDialog;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.fx.control.ProxiStatusBar;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.NetworkUtils;
import io.proximax.app.utils.StringUtils;
import io.proximax.async.AsyncCallbacks;
import io.proximax.async.AsyncTask;
import io.proximax.download.DownloadParameter;
import io.proximax.download.DownloadResult;
import io.proximax.download.Downloader;
import io.proximax.exceptions.UploadFailureException;
import io.proximax.upload.UploadParameter;
import io.proximax.upload.UploadResult;
import io.proximax.upload.Uploader;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.reactfx.SuspendableNo;

public class ProxinoteController extends AbstractController {

    @FXML
    private ImageView logoIv;
    @FXML
    private ProxiStatusBar statusBar;
    @FXML
    private TableView<LocalFile> fileTable;
    @FXML
    private JFXTextField searchField;
    @FXML
    private TableColumn<LocalFile, String> nameCol;
    @FXML
    private TableColumn<LocalFile, String> dateCol;
    @FXML
    JFXComboBox<String> folderCombo;
    //Local account
    private LocalAccount localAccount = null;

    private int newNoteIdx = 0;

    //bind status text property
    private final StringProperty statusProperty = new SimpleStringProperty();

    // The table's masterData
    ObservableList<LocalFile> masterData = null;

    // the saved/loaded files and their format are arbitrary and may change across versions
    private static final String RTFX_FILE_EXTENSION = ".rtfx";
    private static final String TXT_FILE_EXTENSION = ".txt";

    private TabPane tabPane = new TabPane();

    protected final SuspendableNo updatingToolbar = new SuspendableNo();

//    private LocalFile localFile = null;
    private boolean isRichText = true;

    Button boldBtn, italicBtn, underlineBtn, strikeBtn, undoBtn, redoBtn, cutBtn, copyBtn, exportPdfBtn, exportWordBtn;
    ToggleGroup alignmentGrp;
    ToggleButton alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, wrapToggle;
    JFXColorPicker paragraphBackgroundPicker;
    JFXComboBox<Integer> sizeCombo;
    JFXComboBox<String> familyCombo;
    JFXColorPicker textColorPicker, backgroundColorPicker;
    private BooleanProperty bConnected = new SimpleBooleanProperty(false);
//    final ContextMenu tabMenu = new ContextMenu();

    /**
     *
     * @param account
     */
    public ProxinoteController(LocalAccount localAccount) {
        super(false);
        this.localAccount = localAccount;
    }

    public ProxinoteController() {
        super(false);
    }

    @Override
    protected Scene createScene(String fxml) throws IOException {
        Button newBtn = createButton("new", this::newDocument,
                "Add new note");
        Button saveBtn = createButton("save", this::saveDocument,
                "Save note");
        wrapToggle = createToggleButton(null, "wrap", null, "Wrap");
        wrapToggle.setSelected(true);
        undoBtn = createButton("undo", this::undo, "Undo");
        redoBtn = createButton("redo", this::redo, "Redo");
        cutBtn = createButton("cut", this::cut, "Cut");
        copyBtn = createButton("copy", this::copy, "Copy");
        Button pasteBtn = createButton("paste", this::paste, "Paste");

        Button bgcolorBtn = createButton("bgcolor", null, "Background Color Picker");
        Button textcolorBtn = createButton("textcolor", null, "Text Color Picker");

        boldBtn = createButton("bold", this::toggleBold, "Bold");
        exportPdfBtn = createButton("exportpdf", this::exportPDF, "Export PDF");
        exportWordBtn = createButton("exportword", this::exportWord, "Export Word");
        italicBtn = createButton("italic", this::toggleItalic, "Italic");
        underlineBtn = createButton("underline", this::toggleUnderline, "Underline");
        strikeBtn = createButton("strikethrough", this::toggleStrikethrough, "Strike Trough");
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        alignmentGrp = new ToggleGroup();
        alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft, "Align left");
        alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter, "Align center");
        alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight, "Align right");
        alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify, "Justify");
        paragraphBackgroundPicker = new JFXColorPicker();
        sizeCombo = new JFXComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getStyleClass().add("font-combox");
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setTooltip(new Tooltip("Font size"));
        familyCombo = new JFXComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getStyleClass().add("font-combox");
        familyCombo.getSelectionModel().select("Tahoma");
        familyCombo.setTooltip(new Tooltip("Font family"));
        familyCombo.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {

            @Override
            public ListCell<String> call(ListView<String> param) {
                final Label label = new Label();
                final ListCell<String> cell = new ListCell<String>() {

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            label.setText(item);
                            label.setFont(new Font(item, 14));
                            setGraphic(label);
                        }
                    }
                };
                return cell;
            }
        });
        textColorPicker = new JFXColorPicker(Color.BLACK);
        backgroundColorPicker = new JFXColorPicker();

        paragraphBackgroundPicker.setTooltip(new Tooltip("Paragraph background"));
        textColorPicker.setTooltip(new Tooltip("Text color"));
        backgroundColorPicker.setTooltip(new Tooltip("Text background"));

        paragraphBackgroundPicker.valueProperty().addListener((o, old, color) -> updateParagraphBackground(color));
        sizeCombo.setOnAction(evt -> updateFontSize(sizeCombo.getValue()));
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        backgroundColorPicker.valueProperty().addListener((o, old, color) -> updateBackgroundColor(color));
        VBox vbox = new VBox();
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        if (isSupportRichText()) {
            ToolBar toolBar1 = new ToolBar(newBtn, saveBtn, exportPdfBtn, exportWordBtn,
                    new Separator(Orientation.VERTICAL),
                    wrapToggle,
                    new Separator(Orientation.VERTICAL),
                    undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn,
                    new Separator(Orientation.VERTICAL),
                    //boldBtn, italicBtn, underlineBtn, strikeBtn, new Separator(Orientation.VERTICAL),
                    alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn,
                    new Separator(Orientation.VERTICAL),
                    insertImageBtn
            );
            ToolBar toolBar2 = new ToolBar(familyCombo, sizeCombo,
                    boldBtn, italicBtn, underlineBtn, strikeBtn,
                    //new Separator(Orientation.VERTICAL),
                    textcolorBtn, textColorPicker, bgcolorBtn, backgroundColorPicker
            //                    , new Separator(Orientation.VERTICAL), paragraphBackgroundPicker
            );
            vbox.getChildren().addAll(toolBar1, toolBar2, tabPane);
        } else {
            ToolBar toolBar1 = new ToolBar(newBtn, saveBtn, exportPdfBtn, wrapToggle, undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn);
            toolBar1.setPrefHeight(64.0);
            vbox.getChildren().addAll(toolBar1, tabPane);
        }
        FXMLLoader loader = loadXML(fxml);
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        ((BorderPane) mainPane).setCenter(vbox);

        //keyboard
        scene.getAccelerators().put(new KeyCodeCombination(
                KeyCode.N, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            newNote();
        });
        scene.getAccelerators().put(new KeyCodeCombination(
                KeyCode.S, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            saveDocument();
        });
        scene.getAccelerators().put(new KeyCodeCombination(
                KeyCode.D, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            deleteNote();
        });
        scene.getAccelerators().put(new KeyCodeCombination(
                KeyCode.M, KeyCombination.SHORTCUT_DOWN), (Runnable) () -> {
            moveNote();
        });

        mainPane.getChildren().add(winGroup);
        return scene;
    }

    Group winGroup = new Group();

    public boolean isSupportRichText() {
        return isRichText;
    }

    private void reloadContent(LocalFile localFile) {
        if (localFile != null) {
            // don't need download
            Tab tab = getTabAreaEx(localFile);
            if (tab != null) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
            WindowArea w = findWindowArea(localFile);
            if (w != null) {
                w.toFront();
                return;
            }
            File fileCache = LocalFileHelpers.getSourceFile(localAccount, localFile);
            if (fileCache != null) {
                openUniqueFile(localFile, fileCache);
            } else {
                Alert alert = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Operation in progress",
                        ButtonType.CANCEL
                );
                alert.setTitle("Load File");
                alert.setHeaderText("Please wait... ");
                ProgressIndicator progressIndicator = new ProgressIndicator();
                alert.setGraphic(progressIndicator);
                final ProxinoteController proxinoteController = this;

                Task<Void> task = new Task<Void>() {
                    final int N_ITERATIONS = 999;

                    {
                        setOnFailed(a -> {
                            alert.close();
                            updateMessage("Failed");
                        });
                        setOnSucceeded(a -> {
                            alert.close();
                            updateMessage("Succeeded");
                        });
                        setOnCancelled(a -> {
                            alert.close();
                            updateMessage("Cancelled");
                        });
                    }

                    @Override
                    protected Void call() throws Exception {
                        updateMessage("Downloading " + localFile.fileName + " ...");
                        updateProgress(100, N_ITERATIONS);
                        try {
                            File fileCache = LocalFileHelpers.createFileCache(localAccount, localFile);
                            DownloadParameter parameter = LocalFileHelpers.createDownloadParameter(localFile);
                            Downloader download = new Downloader(localAccount.connectionConfig);
                            DownloadResult downloadResult = download.download(parameter);

                            InputStream byteStream = downloadResult.getData().getByteStream();
                            FileOutputStream fouts = new FileOutputStream(fileCache);
                            byte[] buffer = new byte[1024];
                            int read = 0;
                            long sum = 0;
                            while ((read = byteStream.read(buffer)) >= 0) {
                                fouts.write(buffer, 0, read);
                                sum += read;
                                updateProgress(100 + 800 * sum / localFile.fileSize, N_ITERATIONS);
                            }
                            updateProgress(900, N_ITERATIONS);
                            final File file = fileCache;
                            IApp.runSafe(() -> {
                                openUniqueFile(localFile, file);
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            failed();
                            ErrorDialog.showErrorFX(proxinoteController, ex.getMessage());
                        }
                        if (!isCancelled()) {
                            updateProgress(N_ITERATIONS, N_ITERATIONS);
                        }
                        return null;
                    }
                };
                progressIndicator.progressProperty().bind(task.progressProperty());
                Thread taskThread = new Thread(
                        task,
                        "proxipad-thread-" + taskExecution.getAndIncrement()
                );
                taskThread.start();
                alert.initOwner(primaryStage);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.CANCEL && task.isRunning()) {
                    task.cancel();
                }
            }
        }
    }

    AtomicInteger taskExecution = new AtomicInteger(0);

    protected void saveRichText(IRichText tab) {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                "Operation in progress",
                ButtonType.CANCEL
        );
        alert.setTitle("Save File");
        alert.setHeaderText("Please wait... ");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        alert.setGraphic(progressIndicator);

        final LocalFile localFile = tab.getLocalFile();
        localFile.fileName = tab.getFileName();
        tab.setTitle(localFile.getFilenamePretty());

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
                    //updateFileTable(0);
                });
                setOnCancelled(a -> {
                    alert.close();
                    updateMessage("Cancelled");
                });
            }

            @Override
            protected Void call() throws Exception {
                String fileName = tab.getFileName();
                String filePath = LocalFileHelpers.getTempFilePath(localAccount);
                updateMessage("Saving " + fileName + " ...");
                File file = tab.saveFile(filePath);
                updateProgress(2, N_ITERATIONS);
                if (localFile.id == 0) {
                    localFile.filePath = fileName;
                    localFile.filePath = filePath;
                    localFile.desc = tab.getDesc(128);
                    localFile.fileSize = file.length();
                    localFile.modified = file.lastModified();
                    localFile.status = CONST.FILE_STATUS_FAILED;
                    localFile.nemHash = "";
                    localFile.metadata = "";
                    localFile.hash = "";
                    LocalFileHelpers.addFile(localAccount, localFile);
                    addFileTable(localFile);
                    tab.setLocalFile(localFile);
                    updateProgress(N_ITERATIONS, N_ITERATIONS);
                } else {
                    LocalFile saveFile = new LocalFile(localFile);
                    saveFile.id = 0;
                    saveFile.filePath = filePath;
                    saveFile.fileName = fileName;
                    saveFile.desc = tab.getDesc(128);
                    saveFile.metadata = "";
                    saveFile.fileSize = file.length();
                    saveFile.modified = file.lastModified();
                    saveFile.nemHash = "";
                    saveFile.hash = "";
                    saveFile.status = CONST.FILE_STATUS_FAILED;
                    LocalFileHelpers.updateFile(localAccount, localFile, saveFile);
                    repFileTable(localFile, saveFile);
                    tab.setLocalFile(saveFile);
                    updateProgress(N_ITERATIONS, N_ITERATIONS);
                }
                if (!isCancelled()) {
                    updateProgress(0, N_ITERATIONS);
                }
                return null;
            }
        };
        progressIndicator.progressProperty().bind(task.progressProperty());
        Thread taskThread = new Thread(
                task,
                "task-thread-" + taskExecution.getAndIncrement()
        );
        taskThread.start();
        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.CANCEL && task.isRunning()) {
            task.cancel();
        }
    }

    private MenuItem createMenuItem(String title, String styleClass, Runnable action, boolean disable) {
        ImageView iv = new ImageView();
        iv.setId(styleClass);
        MenuItem menuItem = new MenuItem(title, iv);
        //menuItem.getStyleClass().add(styleClass);
        if (action != null) {
            menuItem.setOnAction(evt -> {
                action.run();
            });
        }
        menuItem.setDisable(disable);
        return menuItem;
    }

    private JFXButton createButton(String styleClass, Runnable action, String toolTip) {
        JFXButton button = new JFXButton();
        button.getStyleClass().add(styleClass);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
                if (activeContainer != null) {
                    activeContainer.areaRequestFocus();
                }
            });
        }
        button.setPrefWidth(48);
        button.setPrefHeight(32);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action, String toolTip) {
        ToggleButton button = new ToggleButton();
        if (grp != null) {
            button.setToggleGroup(grp);
        }
        button.getStyleClass().add(styleClass);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
                if (activeContainer != null) {
                    activeContainer.areaRequestFocus();
                }
            });
        }
        button.setPrefWidth(48);
        button.setPrefHeight(32);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private void toggleBold() {
        if (activeContainer != null) {
            activeContainer.toggleBold();
        }
    }

    private void exportPDF() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage.getOwner());
        if (activeContainer != null) {
            activeContainer.exportPDF(selectedDirectory.getAbsolutePath());
            setStatus("Export document to pdf done.");
        }
    }

    private void exportWord() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage.getOwner());
        if (activeContainer != null) {
            activeContainer.exportWord(selectedDirectory.getAbsolutePath());
            setStatus("Export document to word done.");
        }
    }

    private void toggleItalic() {
        if (activeContainer != null) {
            activeContainer.toggleItalic();
        }
    }

    private void toggleUnderline() {
        if (activeContainer != null) {
            activeContainer.toggleUnderline();
        }
    }

    private void toggleStrikethrough() {
        if (activeContainer != null) {
            activeContainer.toggleStrikethrough();
        }
    }

    private void alignLeft() {
        if (activeContainer != null) {
            activeContainer.alignLeft();
        }
    }

    private void alignCenter() {
        if (activeContainer != null) {
            activeContainer.alignCenter();
        }
    }

    private void alignRight() {
        if (activeContainer != null) {
            activeContainer.alignRight();
        }
    }

    private void alignJustify() {
        if (activeContainer != null) {
            activeContainer.alignJustify();
        }
    }

    /*TabPane implement*/
    private void newDocument() {
        LocalFile localFile = new LocalFile();
        localFile.uType = CONST.UTYPE_SECURE_NEMKEYS;
        localFile.address = localAccount.address;
        localFile.privateKey = localAccount.privateKey;
        localFile.publicKey = localAccount.publicKey;
        localFile.category = getCategory();
        localFile.fileName = "New Note " + getTabIndex();
        localFile.filePath = LocalFileHelpers.getTempFilePath(localAccount);
        addNewTab(localFile, "");
    }

    private String getCategory() {
        String folder = folderCombo.getValue();
        if (StringUtils.isEmpty(folder) || folder.equals(CONST.ALLNOTES)) {
            folder = CONST.HOME;
        }
        return folder;
    }

    private int getTabIndex() {
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        newNoteIdx++;
        for (Iterator<Tab> i = iter; iter.hasNext();) {
            TabArea tabArea = (TabArea) i.next();
            String txt = tabArea.getText();
            if (txt.equals("New Note " + newNoteIdx)) {
                newNoteIdx++;
            }
        }
        return newNoteIdx;
    }

    /**
     * Adds a new empty file tab and selects it.
     *
     */
    private void addNewTab(LocalFile localFile, String content) {
        TabArea tab = new TabArea(this, localAccount, localFile);
        tab.setOnCloseRequest(tab);
        tab.setTextContent(content);
        // Updates the view and the model.
        tabPane.getTabs().add(tab);
        // Selects the new tab.
        tabPane.getSelectionModel().select(tab);
        tabPane.requestFocus();
        tab.areaRequestFocus();
    }

    protected int getTabCount() {
        return tabPane.getTabs().size();
    }

    protected void newEmptyTab() {
        if (tabPane.getTabs().size() <= 1) {
            newDocument();
        }
    }

    /**
     * Adds a new empty file tab and selects it.
     *
     */
    private void addNewTab(LocalFile localFile, File file) {
        TabArea tab = new TabArea(this, localAccount, localFile);
        tab.setOnCloseRequest(tab);
        tab.load(file);
        // Updates the view and the model.
        tabPane.getTabs().add(tab);
        // Selects the new tab.
        tabPane.getSelectionModel().select(tab);

//        WindowArea w1 = findWindowArea(localFile);
//        if (w1 != null) {
//            w1.toFront();
//        } else {
//            final WindowArea w = new WindowArea(localAccount, localFile);
//            w.setController(this);
//            w.setLayoutX(300.0);
//            w.setLayoutY(140.0);
//            // define the initial window size
//            //w.setPrefSize(960.0, 605.0);
//            w.setPrefSize(560.0, 405.0);
//            w.load(file);
//            w.activedProperty().addListener((obs, oldVal, newVal) -> {
//                System.out.println(newVal ? "textArea-Focused" : "Unfocused");
//                if (newVal) {
//                    //w.bindToolbars();       
//                    activeContainer = w.getRichTextControl();
//                    activeContainer.bindToolbars();
//                }
//            });
//            winGroup.getChildren().add(w);
//        }
    }

    public void addNewTab(WindowArea w) {
        winGroup.getChildren().remove(w);
        TabArea tab = new TabArea(w);
        tab.setOnCloseRequest(tab);
        // Updates the view and the model.
        tabPane.getTabs().add(tab);
        // Selects the new tab.
        tabPane.getSelectionModel().select(tab);
    }

    public void openNoteWindow(TabArea tab) {
        tabPane.getTabs().remove(tab);
        WindowArea w = new WindowArea(tab);
        w.activedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println(newVal ? "textArea-Focused" : "Unfocused");
            if (newVal) {
                //w.bindToolbars();       
                activeContainer = w;
                bindToolbars(w);
            }
        });
        winGroup.getChildren().add(w);

    }

    private void bindToolbars(IRichText container) {
        if (container != binToolbarContainer) {
            binToolbarContainer = container;
            binToolbarContainer.bindToolbars();
        }

    }

    private IRichText activeContainer = null;
    private IRichText binToolbarContainer = null;

    private WindowArea findWindowArea(LocalFile localFile) {
        Iterator<Node> iter = winGroup.getChildren().iterator();
        for (Iterator<Node> i = iter; iter.hasNext();) {
            WindowArea w = (WindowArea) i.next();
            if (w.getLocalFile().equals(localFile)) {
                return w;
            }
        }
        return null;
    }

    private void undo() {
        if (activeContainer != null) {
            activeContainer.undo();
        }
    }

    private void redo() {
        if (activeContainer != null) {
            activeContainer.redo();
        }
    }

    private void cut() {
        if (activeContainer != null) {
            activeContainer.cut();
        }
    }

    private void copy() {
        if (activeContainer != null) {
            activeContainer.copy();
        }
    }

    private void paste() {
        if (activeContainer != null) {
            activeContainer.paste();
        }
    }

    private void requestFocus() {
        if (activeContainer != null) {
            activeContainer.areaRequestFocus();
        }
    }

    private TabArea getCurrentTab1() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            return (TabArea) tab;
        }
        return null;

    }

    /**
     *
     * @param filename/hash associated with local file name/hash on tab.
     * @return the TabArea associated with file.
     */
    private TabArea getTabAreaEx(LocalFile localFile) {
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        for (Iterator<Tab> i = iter; iter.hasNext();) {
            TabArea tabArea = (TabArea) i.next();
            if (tabArea.isLocalFile(localFile)) {
                return tabArea;
            }
        }
        return null;
    }

    private void moveFileFolderInTab(String oFolder) {
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        for (Iterator<Tab> i = iter; iter.hasNext();) {
            TabArea tabArea = (TabArea) i.next();
            LocalFile localFile = tabArea.getLocalFile();
            if (localFile.category.equals(oFolder)) {
                localFile.category = CONST.HOME;
            }
        }
    }

    /**
     * Opens a file if it is not already open.
     */
    public void openUniqueFile(LocalFile localFile, File file) {
        TabArea tabArea = getTabAreaEx(localFile);
        if (tabArea == null) {
            addNewTab(localFile, file);
        } else {
            tabPane.getSelectionModel().select(tabArea);
        }
    }

    /**
     * Opens a file if it is not already open.
     */
    public void openUniqueFile(LocalFile localFile, String contents) {
        TabArea tabArea = getTabAreaEx(localFile);
        if (tabArea == null) {
            addNewTab(localFile, contents);
        } else {
            tabPane.getSelectionModel().select(tabArea);
        }
    }

    public void saveDocument() {
        if (activeContainer != null && activeContainer.isModified()) {
            saveRichText(activeContainer);
        }
    }

    /**
     * Action listener which inserts a new image at the current caret position.
     */
    private void insertImage() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert image");
        fileChooser.setInitialDirectory(new File(initialDir));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (activeContainer != null) {
            activeContainer.insertImage(selectedFile);
        }
    }

    private void updateFontSize(Integer size) {
        if (!updatingToolbar.get() && activeContainer != null) {
            activeContainer.updateFontSize(size);
        }
    }

    private void updateFontFamily(String family) {
        if (!updatingToolbar.get() && activeContainer != null) {
            activeContainer.updateFontFamily(family);
        }
    }

    private void updateTextColor(Color color) {
        if (!updatingToolbar.get() && activeContainer != null) {
            activeContainer.updateTextColor(color);
        }
    }

    private void updateBackgroundColor(Color color) {
        if (!updatingToolbar.get() && activeContainer != null) {
            activeContainer.updateBackgroundColor(color);
        }
    }

    private void updateParagraphBackground(Color color) {
        if (!updatingToolbar.get() && activeContainer != null) {
            activeContainer.updateParagraphBackground(color);
        }
    }

    private void reloadFolderData() {
        String currentFolder = folderCombo.getValue();
        ObservableList<String> obList = FXCollections.observableList(LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network));
        if (obList.isEmpty()) {
            obList.add(CONST.ALLNOTES);
            obList.add(CONST.HOME);
        } else {
            obList.add(0, CONST.ALLNOTES);
        }
        folderCombo.getItems().clear();
        folderCombo.setItems(obList);
        if (StringUtils.isNotNull(currentFolder) && obList.contains(currentFolder)) {
            folderCombo.setValue(currentFolder);
        } else {
            folderCombo.setValue(CONST.HOME);
        }
    }

    @Override
    public void initialize() {
        try {
            if (localAccount == null) {
                initializeAccount();
            }

//            MenuItem menu0 = createMenuItem("Open to New Window", "", this::openNoteWindow, false);
//            tabMenu.getItems().add(menu0);
            nameCol.setCellValueFactory(new PropertyValueFactory<LocalFile, String>("filenamePretty"));
            dateCol.setCellValueFactory(new PropertyValueFactory<LocalFile, String>("modified"));
            dateCol.setSortable(false);

            ObservableList<String> obList = FXCollections.observableList(LocalFileHelpers.getListFolder(localAccount.fullName, localAccount.network));
            if (obList.isEmpty()) {
                obList.add(CONST.ALLNOTES);
                obList.add(CONST.HOME);
            } else {
                obList.add(0, CONST.ALLNOTES);
            }
            folderCombo.setItems(obList);
            folderCombo.setValue(CONST.HOME);
            folderCombo.getStyleClass().add("folder-combox");
            initializeFileTable();
            folderCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (StringUtils.isNotNull(newValue)) {
                    System.out.println("Change folder: " + newValue);
                    initializeFileTable();
                }
            });
            tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
                @Override
                public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                    if (newValue != null) {
                        activeContainer = (IRichText) newValue;
                        bindToolbars(activeContainer);
                    }
                }
            });
            //tabPane.setContextMenu(tabMenu);
            newNote();
            initializeStatusBar();
            fileTable.setRowFactory(tv -> {
                TableRow<LocalFile> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                            && event.getClickCount() == 1) {
                        LocalFile rowFile = row.getItem();
                        reloadContent(rowFile);
                    }
                });
                // only display context menu for non-null items:
//                row.contextMenuProperty().bind(
//                        Bindings.when(Bindings.isNotNull(row.itemProperty()))
//                                .then(fileMenu)
//                                .otherwise((ContextMenu) null));
//
                return row;
            });
            nameCol.setCellFactory(tc -> {
                TableCell<LocalFile, String> cell = new TableCell<>();
                Text text = new Text();
                cell.setGraphic(text);
                text.getStyleClass().add("text-wrap-desc");
                cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                text.wrappingWidthProperty().bind(nameCol.widthProperty());
                text.textProperty().bind(cell.itemProperty());
                return cell;
            });
            dateCol.setCellFactory(tc -> {
                TableCell<LocalFile, String> cell = new TableCell<>();
                Text text = new Text();
                cell.setGraphic(text);
                cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                text.getStyleClass().add("text-wrap-date");
                text.wrappingWidthProperty().bind(dateCol.widthProperty());
                text.textProperty().bind(cell.itemProperty());
                return cell;
            });
            //check server is alive
            initializeCheckingConnection();
            //monitor transaction
            initializeMonitorTransaction();
            logoIv.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                showProfile();
            });
            // monitor job
            initializeJob();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initializeJob() {
        jobStatus = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                Task<Boolean> aliveTask = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        if (localAccount.testNode()) {
                            //need active account
                            if (uploadTask != null) {
                                if (!uploadTask.isDone()) {
                                    return true;
                                }
                            }
                            LocalFile localFile = LocalFileHelpers.getJob(localAccount);
                            if (localFile != null) {
                                try {
                                    LocalFile lFile = findLocalFileInTable(localFile.id);
                                    if (lFile != null) {
                                        lFile.clone(localFile);
                                        localFile = lFile;
                                    }
                                    LocalFileHelpers.addFileQueue(localAccount, localFile);
                                    File fileSave = new File(localFile.filePath);
                                    uploadTask = createUploadTaskAsync(localFile, fileSave);
                                    // run task in single-thread executor (will queue if another task is running):                                    
                                    execJob.submit(uploadTask);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        return true;
                    }

                };
                return aliveTask;
            }
        };
        jobStatus.setPeriod(Duration.seconds(30));
        jobStatus.start();
    }

    private AsyncTask asyncTask = null;
    private Task<Void> uploadTask = null;

    private LocalFile findLocalFileInTable(int id) {
        for (LocalFile localFile : masterData) {
            if (localFile.id == id) {
                return localFile;
            }
        }
        return null;
    }

    private LocalFile findLocalFileOpen(int id) {
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        for (Iterator<Tab> i = iter; iter.hasNext();) {
            TabArea tabArea = (TabArea) i.next();
            if (tabArea.getLocalFile().id == id) {
                return tabArea.getLocalFile();
            }
        }
        return null;
    }

    private Task<Void> createUploadTask(LocalFile localFile, File file) {
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected void setException(Throwable t) {
            }

            {
                setOnFailed(a -> {
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    uploadTask = null;
                });
                setOnSucceeded(a -> {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    uploadTask = null;
                });
                setOnCancelled(a -> {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                    uploadTask = null;
                });
            }

            @Override
            protected Void call() throws Exception {
                if (localAccount.testNode()) {
                    try {
                        IApp.runSafe(() -> {
                            statusBar.progressProperty().bind(progressProperty());
                            statusBar.textProperty().bind(messageProperty());
                        });
                        updateMessage("Status: uploading " + localFile.fileName + " ...");
                        updateProgress(100, 999);
                        LocalFileHelpers.uploadingFile(localAccount, localFile);
                        UploadParameter uploadParameter = LocalFileHelpers.createUploadFileParameter(localAccount, localFile, file);
                        Uploader upload = new Uploader(localAccount.connectionConfig);
                        updateProgress(400, 999);
                        UploadResult uploadResult = upload.upload(uploadParameter);
                        updateProgress(800, 999);
                        localFile.uploadDate = System.currentTimeMillis();
                        localFile.hash = uploadResult.getData().getDataHash();
                        localFile.nemHash = uploadResult.getTransactionHash();
                        localFile.status = CONST.FILE_STATUS_TXN;
                        LocalFileHelpers.updateLocalFile(localAccount, localFile.id, localFile.hash, localFile.nemHash, localFile.metadata, localFile.uploadDate, localFile.status);
                        LocalFile updateFile = findLocalFileInTable(localFile.id);
                        if (updateFile != null) {
                            updateFile.uploadDate = localFile.uploadDate;
                            updateFile.hash = localFile.hash;
                            updateFile.nemHash = localFile.nemHash;
                            updateFile.status = localFile.status;
                        }
                        updateMessage("Status: upload " + localFile.fileName + " completed.");
                        updateProgress(999, 999);
                    } catch (Exception ex) {
                        updateMessage("Status: upload " + localFile.fileName + " failed.");
                        ex.printStackTrace();
                        LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                        throw new ExecutionException(ex);

                    }
                } else {
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                }
                return null;
            }
        };
        return task;
    }

    private Task<Void> createUploadTaskAsync(LocalFile localFile, File file) {
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected void setException(Throwable t) {
                t.printStackTrace();
            }

            {
                setOnFailed(a -> {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                    uploadTask = null;
                });
                setOnSucceeded(a -> {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    uploadTask = null;
                });
                setOnCancelled(a -> {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().unbind();
                        statusBar.textProperty().unbind();
                    });
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                });
            }

            @Override
            protected Void call() throws Exception {
                if (localAccount.testNode()) {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().bind(progressProperty());
                        statusBar.textProperty().bind(messageProperty());
                        updateMessage("Status: uploading " + localFile.fileName + " ...");
                        updateProgress(100, 999);
                    });
                    LocalFileHelpers.uploadingFile(localAccount, localFile);
                    UploadParameter uploadParameter = LocalFileHelpers.createUploadFileParameter(localAccount, localFile, file);
                    Uploader upload = new Uploader(localAccount.connectionConfig);
                    final CompletableFuture<Throwable> toPopulateOnFailure = new CompletableFuture<>();
                    asyncTask = upload.uploadAsync(uploadParameter,
                            AsyncCallbacks.create(
                                    (UploadResult uploadResult) -> {
                                        localFile.uploadDate = System.currentTimeMillis();
                                        localFile.hash = uploadResult.getData().getDataHash();
                                        localFile.nemHash = uploadResult.getTransactionHash();
                                        localFile.status = CONST.FILE_STATUS_TXN;
                                        LocalFileHelpers.updateLocalFile(localAccount, localFile.id, localFile.hash, localFile.nemHash, localFile.metadata, localFile.uploadDate, localFile.status);
                                        LocalFile updateFile = findLocalFileInTable(localFile.id);
                                        if (updateFile != null) {
                                            updateFile.uploadDate = localFile.uploadDate;
                                            updateFile.hash = localFile.hash;
                                            updateFile.nemHash = localFile.nemHash;
                                            updateFile.status = localFile.status;
                                        }
                                        IApp.runSafe(() -> {
                                            updateMessage("Status: upload " + localFile.fileName + " completed.");
                                            updateProgress(999, 999);
                                        });
                                    }, toPopulateOnFailure::complete));
//                                    (Throwable ex) -> {
//                                        ex.printStackTrace();
//                                        LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_FAILED);
//                                        if (isCancelled()) {
//                                            return;
//                                        }
//                                        IApp.runSafe(() -> {
//                                            updateMessage("Status: upload issue " + localFile.fileName);
//                                            updateProgress(0, 999);
//                                        });
//                                        failed();
//                                    }));
                    int progress = 100;
                    while (!asyncTask.isDone()) {
                        if (progress < 900) {
                            progress += 10;
                            updateProgress(progress, 999);
                        }
                        if (asyncTask.isCancelled()) {
                            IApp.runSafe(() -> {
                                updateMessage("Status: cancel upload " + localFile.filePath);
                                updateProgress(0, 999);
                            });
                            LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                            break;
                        }
                        Thread.sleep(100);
                    }
                    try {
                        final Throwable throwable = toPopulateOnFailure.get(5, TimeUnit.SECONDS);
                        if (throwable instanceof UploadFailureException) {
                            updateMessage("Status: upload issue " + localFile.fileName);
                            throw new ExecutionException(throwable);
                        }
                    } catch (TimeoutException ex) {
                    }
                } else {
                    LocalFileHelpers.uploadFileFailed(localAccount, localFile);
                }
                return null;
            }
        };
        return task;
    }

    private void showProfile() {
        try {
            UserProfileDialog dlg = new UserProfileDialog(localAccount);
            dlg.openWindow(this);
        } catch (IOException ex) {
        }
    }

    @Override
    protected void onClosing(Event event) {
        super.onClosing(event);
        IApp.exit(0);
    }

    @Override
    protected void show() {
        primaryStage.show();
//        updateFileTable(0);
    }

    @Override
    public String getTitle() {
        return CONST.HOME_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.HOME_FXML;
    }

    public static void showDialog(LocalAccount localAccount) {
        try {
            ProxinoteController dialog = new ProxinoteController(localAccount);
            dialog.setResizable(false);
            dialog.openWindow();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void initializeAccount() {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                "Init your account",
                ButtonType.CANCEL
        );
        alert.setTitle("Init your account");
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
                    updateProgress(4, N_ITERATIONS);
                    if (!StringUtils.isEmpty(user)) {
                        if (AccountHelpers.isExistAccount(user, NetworkUtils.TEST_NET)) {
                            localAccount = AccountHelpers.login(user, NetworkUtils.TEST_NET, user);
                        } else {
                            localAccount = AccountHelpers.createAccount(user, NetworkUtils.TEST_NET, user, null);
                        }
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
                "task-thread-" + taskExecution.getAndIncrement()
        );
        taskThread.start();
        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.CANCEL && task.isRunning()) {
            task.cancel();
        }

    }

    @FXML
    private void addNoteBtn(ActionEvent event) {
        newNote();
    }

    @FXML
    private void addFolderBtn(ActionEvent event) {
        try {
            FolderDialog dialog = new FolderDialog(localAccount, "");
            dialog.setParent(this);
            dialog.openWindow();
            if (dialog.getResultType() == ButtonType.OK) {
                reloadFolderData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void cfgFolderBtn(ActionEvent event) {
        try {
            activeContainer.areaRequestFocus();
            CategoryDialog dialog = new CategoryDialog(localAccount);
            dialog.setParent(this);
            dialog.openWindow();
            if (dialog.getResultType() == ButtonType.OK) {
                reloadFolderData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void delFolderBtn(ActionEvent event) {
        try {
            FolderDialog dialog = new FolderDialog(localAccount, true);
            dialog.setParent(this);
            dialog.openWindow();
            if (dialog.getResultType() == ButtonType.OK) {
                reloadFolderData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void newNote() {
        try {
            newDocument();
        } catch (Exception ex) {
        }
    }

    private void loadFileTableData() {
        List<LocalFile> listFiles = LocalFileHelpers.getFiles(localAccount, folderCombo.getValue());
        if (listFiles != null) {
            try {
                IApp.runSafe(() -> {
                    fileTable.getItems().clear();
                });
                masterData.removeAll();
                masterData.addAll(listFiles);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void initializeFileTable() {
        masterData = FXCollections.observableArrayList();
        List<LocalFile> listFiles = LocalFileHelpers.getFiles(localAccount, folderCombo.getValue());
        if (listFiles != null) {
            masterData.addAll(listFiles);
        }
        // 1. Wrap the ObservableList in a FilteredList (initially display all masterData).
        FilteredList<LocalFile> filteredData = new FilteredList<>(masterData, p -> true);
        // 2. Set the filter Predicate whenever the filter changes.
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(localFile -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = newValue.toLowerCase();
                if (localFile.fileName.toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches first name.
                }
                return false; // Does not match.
            });
        });
        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<LocalFile> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(fileTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.        
        fileTable.setItems(sortedData);
    }

    private void deleteNote() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete this note?", ButtonType.YES, ButtonType.NO);
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
        alert.showAndWait();
        if (alert.getResult() == ButtonType.NO) {
            return;
        }
        LocalFile localFile = fileTable.getSelectionModel().getSelectedItem();
        if (localFile != null) {
            masterData.remove(localFile);
            fileTable.refresh();
            //call delete
            TabArea tab = getTabAreaEx(localFile);
            if (tab != null) {
                tabPane.getTabs().remove(tab);
            } else {
                WindowArea w = findWindowArea(localFile);
                if (w != null) {
                    w.close();
                    winGroup.getChildren().remove(w);
                }
            }

            LocalFileHelpers.deleteFile(localAccount, localFile);
        }
    }

    private void moveNote() {
        try {
            LocalFile localFile = fileTable.getSelectionModel().getSelectedItem();
            if (localFile != null) {
                FolderDialog dlg = new FolderDialog(localAccount, localFile);
                dlg.openWindow(this);
                if (dlg.getResultType() == ButtonType.OK) {
                    masterData.remove(localFile);
                    fileTable.refresh();
                    LocalFile openFile = findLocalFileOpen(localFile.id);
                    if (openFile != null) {
                        openFile.category = localFile.category;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void addFileTable(LocalFile localFile) {
        if (localFile.category.equals(getCategory())) {
            masterData.add(0, localFile);
            this.fileTable.refresh();
            this.fileTable.getSelectionModel().select(localFile);
//            TabArea tab = getTabAreaEx(localFile);
//            if (tab != null) {
//                tab.setLocalFile(localFile);
//            }
        }
        //getCurrentTab().setLocalFile(localFile);
    }

    private void repFileTable(LocalFile curFile, LocalFile newFile) {
        if (newFile.category.equals(getCategory())) {
            masterData.remove(curFile);
            masterData.add(0, newFile);
            this.fileTable.refresh();
            this.fileTable.getSelectionModel().select(newFile);
        }
//        TabArea tab = getTabAreaEx(curFile);
//        if (tab != null) {
//            tab.setLocalFile(newFile);
//        }
        //getCurrentTab().setLocalFile(newFile);
    }

    /**
     * Initialize status bar
     */
    private void initializeStatusBar() {
        statusBar = new ProxiStatusBar();
        statusBar.setImageSuccess(getMainApp().getImageFromResource(CONST.IMAGE_GREEN, 14.0, 14.0));
        statusBar.setImageFailed(getMainApp().getImageFromResource(CONST.IMAGE_RED, 14.0, 14.0));
        statusBar.setEventHandler(this);
        ((BorderPane) mainPane).setBottom(statusBar);
        BorderPane.setAlignment(statusBar, Pos.BOTTOM_CENTER);
        // text in status bar
        statusBar.textProperty().bind(statusProperty);
        if (localAccount != null) {
            List<String> list = localAccount.getNodes();
            ObservableList<String> obList = FXCollections.observableList(list);
            if (localAccount.getCurrentNodeIndex() == -1) {
                localAccount.setConnectionIndex(0);
            }
            statusBar.setNodeItems(obList, localAccount.getCurrentNodeIndex());
            // connection status
            setConnection(localAccount.isConnected());
        }
        //add theme button in statusbar
        final ImageView imageView = new ImageView();
        imageView.setId("nightmode-img");
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        JFXButton nightMode = new JFXButton("Night mode", imageView);
        nightMode.setId("nav-togbtn1");
        statusBar.getLeftItems().add(nightMode);
        nightMode.setOnAction(evt -> {
            if (mainApp.getCurrentTheme().contains("Light")) {
                mainApp.setTheme(1);
                nightMode.setText("Light mode");
            } else {
                mainApp.setTheme(0);
                nightMode.setText("Night mode");
            }
            reloadTheme();
            //area.requestFocus();
        });
        //add net config button in statusbar
        final ImageView imv = new ImageView();
        imv.setId("netcfg-img");
        imv.setFitHeight(16);
        imv.setFitWidth(16);
        JFXButton netcfgBtn = new JFXButton("Network Config", imv);
        netcfgBtn.setId("nav-togbtn1");
        statusBar.getLeftItems().add(netcfgBtn);
        netcfgBtn.setOnAction(evt -> {
            showNetCfg();
        });
    }

    private void showNetCfg() {
        try {
            NetworkDialog dlg = new NetworkDialog(localAccount.network);
            dlg.setParent(this);
            dlg.openWindow();
            if (dlg.getResultType() == ButtonType.OK) {
                List<String> list = localAccount.getNodes();
                ObservableList<String> obList = FXCollections.observableList(list);
                statusBar.setNodeItems(obList, localAccount.getCurrentNodeIndex());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JFXButton createImageButtonCSS(String imgUrl, Runnable action, String toolTip) {
        ImageView iv = new ImageView();
        iv.setFitWidth(16);
        iv.setFitHeight(16);
        iv.setId(imgUrl);
        JFXButton button = new JFXButton("", iv);
        button.setGraphicTextGap(0.0);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
            });
        }
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    /**
     * Set connection status in status bar
     *
     * @param connected
     */
    public void setConnection(Boolean connected) {
        this.statusBar.setImageStatus(connected);
        bConnected.set(connected);
        if (connected) {
            if (this.listener == null) {
                initializeMonitorTransaction();
            }
        } else {
            closeListener();
        }
    }

    private void closeListener() {
        try {
            if (listener != null) {
                listener.close();
                listener = null;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set connection status: image, text in status bar
     *
     * @param connected
     */
    public void setConnectionStatus(Boolean connected) {
        if (connected) {
            setStatus(CONST.STR_CONNECTED);
        } else {
            setStatus(CONST.STR_DISCONNECTED);
        }
        this.statusBar.setImageStatus(connected);
    }

    /**
     * Set status text in status bar
     *
     * @param status
     */
    public void setStatus(String status) {
        this.statusProperty.set(CONST.STR_STATUS + status);
    }

    private ScheduledService<Boolean> serverStatus = null;
    private ScheduledService<Boolean> jobStatus = null;

    /**
     * Initialize service checking connection
     */
    private void initializeCheckingConnection() {
        serverStatus = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                Task<Boolean> aliveTask = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        if (localAccount != null && localAccount.testNode()) {
                            if (localAccount.status == 0) {
                                NetworkUtils.activeAccount(localAccount);
                            }
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected void succeeded() {
                        serverStatus.setPeriod(Duration.minutes(1));
                        if (getValue()) { // alive  
                            setConnection(Boolean.TRUE);
                        } else {
                            setConnection(Boolean.FALSE);
                        }
                    }

                    @Override
                    protected void failed() {
                        serverStatus.setPeriod(Duration.minutes(1));
                    }

                };
                return aliveTask;
            }
        };
        serverStatus.setPeriod(Duration.seconds(5));
        serverStatus.start();
    }

    /**
     * Dispose when close application
     */
    @Override

    protected void dispose() {
        //area.dispose();
        if (localAccount == null) {
            localAccount.disconnect();
            localAccount = null;
        }
        if (serverStatus != null) {
            serverStatus.cancel();
        }
        execJob.shutdownNow();
        try {
            if (!execJob.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                execJob.shutdownNow();
            }
        } catch (InterruptedException e) {
            execJob.shutdownNow();
        }
    }

    private AtomicInteger taskCount = new AtomicInteger(0);
    private ExecutorService execJob = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t;
    });

    private Listener listener = null;

    private void initializeMonitorTransaction() {
        try {
            if (localAccount != null && localAccount.testNode() && listener == null) {
                System.out.println("Monitor address: " + localAccount.address);
                listener = (Listener) localAccount.connectionConfig.getBlockchainNetworkConnection().getBlockchainApi().createListener();
                Address address = Address.createFromRawAddress(localAccount.address);
                listener.open().get();
                // wait for transaction to be confirmed
                Disposable subscribe = listener.confirmed(address).subscribe((Transaction txn) -> {
                    try {
                        if (txn instanceof TransferTransaction) {
                            TransferTransaction transferTransaction = (TransferTransaction) txn;
                            String sender = transferTransaction.getSigner().get().getAddress().plain();
                            String recipient = transferTransaction.getRecipient().getAddress().get().plain();
                            String nemHash = transferTransaction.getTransactionInfo().get().getHash().get();
                            if (localAccount.address.equals(recipient)) {
                                if (localAccount.address.equals(sender)) {
                                    LocalFileHelpers.updateFileFromTXN(localAccount.fullName, localAccount.network, nemHash, CONST.FILE_STATUS_NOR);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handle(Event event) {
        if (event.getSource() instanceof ImageView) {
            if (event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) {
                ImageView imageView = (ImageView) event.getSource();
                if (imageView.getId().equals("status-image")) {
                    if (localAccount.isConnected()) {
                        localAccount.disconnect();
                        setConnection(false);
                    } else {

                    }
                }
            }
        } else if (event.getSource() instanceof ComboBox) {
            ComboBox comboBox = (ComboBox) event.getSource();
            if (comboBox.getId().equals("status-nodes")) {
                localAccount.disconnect();
                setConnection(false);
                localAccount.setConnectionIndex(comboBox.getSelectionModel().getSelectedIndex());
                if (serverStatus != null) {
                    serverStatus.cancel();
                    serverStatus.setPeriod(Duration.seconds(1));
                    serverStatus.restart();
                }
            }
        }

        super.handle(event);
    }

    private boolean isModified() {
        if (isWinModified()) {
            return true;
        }
        return isTabModified();
    }

    private boolean isTabModified() {
        Iterator<Tab> iter = tabPane.getTabs().iterator();
        for (Iterator<Tab> i = iter; iter.hasNext();) {
            TabArea tabArea = (TabArea) i.next();
            if (tabArea.isModified()) {
                return true;
            }
        }
        return false;
    }

    private boolean isWinModified() {
        Iterator<Node> iter = winGroup.getChildren().iterator();
        for (Iterator<Node> i = iter; iter.hasNext();) {
            WindowArea w = (WindowArea) i.next();
            if (w.isModified()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canExit() {
        if (isModified()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you quit application and cancel all notes modifed?", ButtonType.YES, ButtonType.NO);
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
            alert.showAndWait();
            if (alert.getResult() == ButtonType.NO) {
                return false;
            }
        }
        if (uploadTask != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you quit application and cancel upload notes ?", ButtonType.YES, ButtonType.NO);
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                jobStatus.cancel();
                if (asyncTask != null) {
                    while (!asyncTask.isDone()) {
                        asyncTask.cancel();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        if (asyncTask.isCancelled()) {
                            break;
                        }
                    }
                }
                while (!uploadTask.isDone()) {
                    uploadTask.cancel(true);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                    if (uploadTask.isCancelled()) {
                        break;
                    }
                }
                uploadTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}

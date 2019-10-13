package io.proximax.app.fx.control.text;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.scene.control.Tab;

import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.fxmisc.richtext.GenericStyledArea;
import org.reactfx.util.Either;

/**
 *
 * @author vantran
 */
public class TabArea extends Tab implements EventHandler, IRichText {

    private RichTextControl textArea = null;
    final ContextMenu tabMenu = new ContextMenu();
    private Label title = null;

    public TabArea(ProxinoteController controller, LocalAccount localAccount, LocalFile localFile) {
        super();
        textArea = new RichTextControl(controller, localAccount, localFile);
        setContent(textArea.getContainer());
        if (StringUtils.isEmpty(localFile.fileName)) {
            localFile.fileName = "New Note";
        }
        MenuItem menu0 = createMenuItem("Open to New Window", "", this::openNoteWindow, false);
        tabMenu.getItems().add(menu0);
        setContextMenu(tabMenu);
        textArea.setParentWindow(this);
        title = new Label(localFile.getFilenamePretty());
        setGraphic(title);
        title.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (t.getButton() == MouseButton.PRIMARY && t.getClickCount() == 2) {
                    openNoteWindow();
                }
            }
        });
        textArea.areaRequestFocus();
    }

    public TabArea(WindowArea w) {
        super();
        textArea = w.getRichTextControl();
        setContent(textArea.getContainer());
        MenuItem menu0 = createMenuItem("Open to New Window", "", this::openNoteWindow, false);
        tabMenu.getItems().add(menu0);
        setContextMenu(tabMenu);
        textArea.setParentWindow(this);
        title = new Label(w.getTitle());
        setGraphic(title);
        title.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (t.getButton() == MouseButton.PRIMARY && t.getClickCount() == 2) {
                    openNoteWindow();
                }
            }
        });
        textArea.areaRequestFocus();
    }

    public String getTitle() {
        return title.getText();
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

    /**
     * Undo the last change to the text area.
     */
    public void undo() {
        textArea.undo();
    }

    /**
     * Redo the last change to the text area.
     */
    public void redo() {
        textArea.redo();
    }

    /**
     * Cut the current selection in the text area.
     */
    public void cut() {
        textArea.cut();
    }

    /**
     * Copy the current selection in the text area.
     */
    public void copy() {
        textArea.copy();
    }

    /**
     * Paste the currently cut/copied text to the text area.
     */
    public void paste() {
        textArea.paste();
    }

    /**
     * Select all the text in the text area.
     */
    public void selectAll() {
        textArea.selectAll();
    }

    /**
     *
     * @return the TextArea associated with this controller.
     */
    public GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> getTextArea() {
        return textArea.getTextArea();
    }

    public boolean isSupportRichText() {
        return textArea.isSupportRichText();
    }

    public void load(File file) {
        textArea.load(file);
    }

    public void save(File file) {
        textArea.save(file);
    }

    public File saveCache() {
        return textArea.saveCache();
    }

    public File saveFile(String fileName) {
        return textArea.saveFile(fileName);
    }

    public String getDesc(int size) {
        String desc = null;
        String text = textArea.getFirstParagraph();
        if (!StringUtils.isEmpty(text)) {
            if (text.length() > size) {
                desc = text.substring(0, size);
            } else {
                desc = text;
            }
            int idx = desc.indexOf("\\n");
            if (idx != -1) {
                desc = desc.substring(0, idx);
            }
            desc = desc.replaceAll("\\r|\\n", "");
        }
        return desc;
    }

    public String getFileName() {
        return textArea.getFileName();
    }

    /**/
    /**
     *
     * @return the saved state of this file tab. A saved state returns false if
     * any changes were made in the editor, and where not written to the file.
     */
    public boolean isModified() {
        return textArea.isModified();
    }

    /**
     * Set the saved state of this file tab.
     *
     * @param value
     */
    public void markModified(boolean value) {
        textArea.markModified(value);
    }

    /**
     * Set the file to be associated with this file tab.
     *
     * @param value to be set as file.
     */
    public void setLocalFile(LocalFile localFile) {
        textArea.setLocalFile(localFile);
    }

    /**
     *
     * @return the current file.
     */
    public LocalFile getLocalFile() {
        return textArea.getLocalFile();
    }

    /**/
    /**
     * Updates the text area to match the file text and the tab text to match
     * the file name.
     *
     * @param content
     */
    public void setTextContent(String content) {
        textArea.setTextContent(content);
    }

    /**
     *
     * @param file to be returned as a String.
     * @return the File output as a String.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String getFileOutputAsString(File file) throws FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);) {
            StringBuilder sb = new StringBuilder();
            while (bis.available() > 0) {
                sb.append((char) bis.read());
            }
            return sb.toString();
        }
    }

    public boolean isLocalFile(LocalFile localFile) {
        return (localFile.equals(textArea.getLocalFile()));
    }

    @Override
    public ChangeListener bindToolbars() {
        return textArea.bindToolbars();
    }

    public void setController(ProxinoteController parent) {
        textArea.setController(parent);
    }

    public ProxinoteController getController() {
        return textArea.getController();
    }

    @Override
    public void toggleBold() {
        textArea.toggleBold();
    }

    @Override
    public void toggleItalic() {
        textArea.toggleItalic();
    }

    @Override
    public void toggleUnderline() {
        textArea.toggleUnderline();
    }

    @Override
    public void toggleStrikethrough() {
        textArea.toggleStrikethrough();
    }

    @Override
    public void alignLeft() {
        textArea.alignLeft();
    }

    @Override
    public void alignCenter() {
        textArea.alignCenter();
    }

    @Override
    public void alignRight() {
        textArea.alignRight();
    }

    @Override
    public void alignJustify() {
        textArea.alignJustify();
    }

    @Override
    public void handle(Event event) {
        if (event.getEventType() == TAB_CLOSE_REQUEST_EVENT) {
            if (isModified()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save note ?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(textArea.getController().getMainApp().getIcon());
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    textArea.getController().saveRichText(this);
                }
                if (alert.getResult() == ButtonType.CANCEL) {
                    event.consume();
                } else {
                    textArea.getController().newEmptyTab();
                }
            } else {
                if (isEmpty() && textArea.getController().getTabCount() <= 1) {
                    event.consume();
                } else {
                    textArea.getController().newEmptyTab();
                }
            }
        }
    }

    public boolean isEmpty() {
        return textArea.getTextArea().getText().isEmpty() && getText().contains("New Note");
    }

    @Override
    public void areaRequestFocus() {
        textArea.areaRequestFocus();
    }

    @Override
    public void insertImage(File selectedFile) {
        textArea.insertImage(selectedFile);
    }

    @Override
    public void updateFontSize(Integer size) {
        textArea.updateFontSize(size);
    }

    @Override
    public void updateFontFamily(String family) {
        textArea.updateFontFamily(family);
    }

    @Override
    public void updateTextColor(Color color) {
        textArea.updateTextColor(color);
    }

    @Override
    public void updateBackgroundColor(Color color) {
        textArea.updateBackgroundColor(color);
    }

    public void updateParagraphBackground(Color color) {
        textArea.updateParagraphBackground(color);
    }

    public RichTextControl getRichTextControl() {
        return textArea;
    }

    public void openNoteWindow() {
        getController().openNoteWindow(this);
    }

    @Override
    public void setTitle(String text) {
        title.setText(text);
    }

    @Override
    public void exportPDF(String path) {
        textArea.exportPDF(path);
    }
    
    @Override
    public void exportWord(String path) {
        textArea.exportWord(path);
    }

}

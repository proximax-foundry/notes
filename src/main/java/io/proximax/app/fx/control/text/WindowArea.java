package io.proximax.app.fx.control.text;

import io.proximax.app.core.ui.IApp;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jfxtras.labs.scene.control.window.CloseIcon;
import jfxtras.labs.scene.control.window.Window;

import org.fxmisc.richtext.GenericStyledArea;
import org.reactfx.util.Either;

public class WindowArea extends Window implements IRichText {

    private RichTextControl textArea = null;

    public WindowArea(ProxinoteController controller, LocalAccount localAccount, LocalFile localFile) {
        super();
        textArea = new RichTextControl(controller, localAccount, localFile);
        setTitle(localFile.getFilenamePretty());
        // either to the left
        addButtons();
        getContentPane().getChildren().add(textArea.getContainer());
        if (StringUtils.isEmpty(localFile.fileName)) {
            localFile.fileName = "New Note";
        }
//        setLayoutX(300.0);
//        setLayoutY(140.0);
//        // define the initial window size
//        setPrefSize(960.0, 605.0);
        textArea.setParentWindow(this);
        addHandleEvents();
        textArea.areaRequestFocus();
    }

    public WindowArea(TabArea tab) {
        super();
        textArea = tab.getRichTextControl();
        setTitle(tab.getTitle());
        addButtons();
        //setTitleBarStyleClass("top-pane");
        getContentPane().getChildren().add(textArea.getContainer());
//        setLayoutX(300.0);
//        setLayoutY(140.0);
//        // define the initial window size
//        setPrefSize(960.0, 605.0);        
        setLayoutX(300.0);
        setLayoutY(140.0);
        // define the initial window size        
        setPrefSize(560.0, 405.0);
        textArea.setParentWindow(this);
        addHandleEvents();
        textArea.areaRequestFocus();
    }

    private void addButtons() {
        // either to the left
        CloseIcon close = new CloseIcon(this);
        final IRichText richText = this;
        close.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (isModified()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save note ?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(textArea.getController().getMainApp().getIcon());
                    alert.showAndWait();
                    if (alert.getResult() == ButtonType.YES) {
                        textArea.getController().saveRichText(richText);
                    }
                    if (alert.getResult() == ButtonType.CANCEL) {
                        return;
                    }
                }
                close();
            }
        });
        if (IApp.isMac()) {
            getLeftIcons().add(close);
        } else {
//            getRightIcons().add(new MinimizeIcon(this));            
            getRightIcons().add(close);
        }
    }

    private void addHandleEvents() {

        addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (t.getButton() == MouseButton.PRIMARY && t.getClickCount() == 2) {
                    if (t.getTarget().toString().startsWith("TitleBar@")) {
                        addNewTab();
                    }
                }
            }
        });

    }

    public RichTextControl getRichTextControl() {
        return textArea;
    }

    public ReadOnlyBooleanProperty activedProperty() {
        return textArea.focusedProperty();
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

    public ChangeListener bindToolbars() {
        return textArea.bindToolbars();
    }

    public void setController(ProxinoteController parent) {
        textArea.setController(parent);
    }

    public ProxinoteController getController() {
        return textArea.getController();
    }

    public void toggleBold() {
        textArea.toggleBold();
    }

    public void toggleItalic() {
        textArea.toggleItalic();
    }

    public void toggleUnderline() {
        textArea.toggleUnderline();
    }

    public void toggleStrikethrough() {
        textArea.toggleStrikethrough();
    }

    public void alignLeft() {
        textArea.alignLeft();
    }

    public void alignCenter() {
        textArea.alignCenter();
    }

    public void alignRight() {
        textArea.alignRight();
    }

    public void alignJustify() {
        textArea.alignJustify();
    }

    public boolean isEmpty() {
        if (textArea.getTextArea().getText().isEmpty() && getTitle().contains("New Note")) {
            return true;
        }
        return false;
    }

    public void areaRequestFocus() {
        textArea.areaRequestFocus();
    }

    public void insertImage(File selectedFile) {
        textArea.insertImage(selectedFile);
    }

    public void updateFontSize(Integer size) {
        textArea.updateFontSize(size);
    }

    public void updateFontFamily(String family) {
        textArea.updateFontFamily(family);
    }

    public void updateTextColor(Color color) {
        textArea.updateTextColor(color);
    }

    public void updateBackgroundColor(Color color) {
        textArea.updateBackgroundColor(color);
    }

    public void updateParagraphBackground(Color color) {
        textArea.updateParagraphBackground(color);
    }

    public void setText(String text) {
        setTitle(text);
    }

    public void addNewTab() {
        getController().addNewTab(this);
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

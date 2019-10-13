package io.proximax.app.fx.control.text;

import io.proximax.app.db.LocalFile;
import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;

/**
 *
 * @author thcao
 */
public interface IRichText {

    public ChangeListener bindToolbars();

    public void setTitle(String text);

    public LocalFile getLocalFile();

    public String getFileName();

    public void setLocalFile(LocalFile localFile);

    public File saveFile(String fileName);
    
    public void exportPDF(String path);
    
    public void exportWord(String path);

    public String getDesc(int size);

    public void areaRequestFocus();

    public void insertImage(File selectedFile);

    public void updateFontSize(Integer size);

    public void updateFontFamily(String family);

    public void updateTextColor(Color color);

    public void updateBackgroundColor(Color color);

    public void updateParagraphBackground(Color color);

    public void toggleBold();

    public void toggleItalic();

    public void toggleUnderline();

    public void toggleStrikethrough();

    public void alignLeft();

    public void alignCenter();

    public void alignRight();

    public void alignJustify();

    public void undo();

    /**
     * Redo the last change to the text area.
     */
    public void redo();

    /**
     * Cut the current selection in the text area.
     */
    public void cut();

    /**
     * Copy the current selection in the text area.
     */
    public void copy();

    /**
     * Paste the currently cut/copied text to the text area.
     */
    public void paste();

    /**
     * Select all the text in the text area.
     */
    public void selectAll();

    public boolean isModified();
}

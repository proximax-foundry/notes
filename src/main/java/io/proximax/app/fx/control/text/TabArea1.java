package io.proximax.app.fx.control.text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Tuple2;

import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.util.Either;

/**
 *
 * @author vantran
 */
public class TabArea1 extends Tab implements ChangeListener<Boolean>, EventHandler {

    private final TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();
    private static final String RTFX_FILE_EXTENSION = ".rtfx";
    private final boolean isRichText = true;
    private final SimpleBooleanProperty modified = new SimpleBooleanProperty(false);

    private LocalAccount localAccount = null;
    private LocalFile localFile = null;
    private final GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> textArea = new GenericStyledArea<>(
            ParStyle.EMPTY, // default paragraph style
            (paragraph, style) -> paragraph.setStyle(style.toCss()), // paragraph style setter

            TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Tahoma").updateTextColor(Color.BLACK), // default segment style
            styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()), // segment operations
            seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter

    {
        textArea.setWrapText(true);
        textArea.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));

    }

    public TabArea1(LocalAccount localAccount, LocalFile localFile) {
        super();
        this.localAccount = localAccount;
        this.localFile = localFile;
        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(textArea);
        setContent(vsPane);

        if (StringUtils.isEmpty(localFile.fileName)) {
            localFile.fileName = "New Note";
        }
        setText(localFile.getFilenamePretty());

        textArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                markModified(true);
            }
        });
        textArea.requestFocus();
    }

    private Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
            BiConsumer<? super TextExt, TextStyle> applyStyle) {
        return seg.getSegment().unify(
                text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                LinkedImage::createNode
        );
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
        return textArea;
    }

    public boolean isSupportRichText() {
        return isRichText;
    }

    public void load(File file) {
        if (textArea.getStyleCodecs().isPresent()) {
            if (!isSupportRichText() || !file.getName().endsWith(RTFX_FILE_EXTENSION)) {
                // Write the content to the file
                try {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    // Read the file, and set its contents within the editor            
                    StringBuffer sb = new StringBuffer();
                    while (bis.available() > 0) {
                        sb.append((char) bis.read());
                    }
                    textArea.replaceSelection(sb.toString());
                } catch (Exception e) {
                }
            } else {
                Tuple2<Codec<ParStyle>, Codec<StyledSegment<Either<String, LinkedImage>, TextStyle>>> codecs = textArea.getStyleCodecs().get();
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, textArea.getSegOps());
                try {

                    FileInputStream fis = new FileInputStream(file);
                    DataInputStream dis = new DataInputStream(fis);
                    StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = codec.decode(dis);
                    fis.close();

                    if (doc != null) {
                        textArea.replaceSelection(doc);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        markModified(false);
    }

    public void save(File file) {
        // Write the content to the file
        if (!isSupportRichText() || !file.getName().endsWith(RTFX_FILE_EXTENSION)) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                String text = textArea.getText();
                bos.write(text.getBytes());
                bos.flush();
                bos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = textArea.getDocument();

            // Use the Codec to save the document in a binary format
            textArea.getStyleCodecs().ifPresent(codecs -> {
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec
                        = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, textArea.getSegOps());
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    DataOutputStream dos = new DataOutputStream(fos);
                    codec.encode(dos, doc);
                    fos.close();
                } catch (IOException fnfe) {
                    fnfe.printStackTrace();
                }
            });
        }
    }
    
    public File saveCache() {
        File file = new File(localFile.filePath);
        save(file);        
        return file;
    }

    public File saveFile(String fileName) {
        File file = new File(fileName);
        save(file);
        markModified(false);
        return file;
    }

    public void insertImage(File selectedFile) {
        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            imagePath = imagePath.replace('\\', '/');
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros
                    = ReadOnlyStyledDocument.fromSegment(Either.right(new RealLinkedImage(imagePath)),
                            ParStyle.EMPTY, TextStyle.EMPTY, textArea.getSegOps());
            textArea.replaceSelection(ros);
        }
    }

    public void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = textArea.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = textArea.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            textArea.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    public void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = textArea.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = textArea.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            textArea.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = textArea.getSelection();
        int startPar = textArea.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
        int endPar = textArea.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
        for (int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = textArea.getParagraph(i);
            textArea.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
    }

    private String getFirstParagraph() {
        for (Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> p : textArea.getParagraphs()) {
            if (!StringUtils.isEmpty(p.getText())) {
                return p.getText();
            }
        }
        return null;
    }

    public String getDesc(int size) {
        String desc = null;
        String text = getFirstParagraph();
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
        String fileName = getDesc(CONST.MAX_LENGTH);
        if (StringUtils.isEmpty(fileName)) {
            fileName = getText();
        }
        //fileName = fileName.replaceAll("[\\\\/:*?\"<>|,]", "");
        fileName = fileName.replaceAll("[^A-Za-z0-9 ]","");
        if (isSupportRichText() && textArea.getStyleCodecs().isPresent()) {
            fileName += RTFX_FILE_EXTENSION;
        }
        return fileName;
    }

    /**/
    /**
     *
     * @return the saved state of this file tab. A saved state returns false if
     * any changes were made in the editor, and where not written to the file.
     */
    public boolean isModified() {
        return modified.get();
    }

    /**
     * Set the saved state of this file tab.
     *
     * @param value
     */
    public void markModified(boolean value) {
        modified.set(value);
    }

    /**
     *
     * @return the saved state property.
     */
    public SimpleBooleanProperty modifiedProperty() {
        return modified;
    }

    /**
     * Set the file to be associated with this file tab.
     *
     * @param value to be set as file.
     */
    public void setLocalFile(LocalFile localFile) {
        this.localFile = localFile;
    }

    /**
     *
     * @return the current file.
     */
    public LocalFile getLocalFile() {
        return localFile;
    }

    /**/
    /**
     * Updates the text area to match the file text and the tab text to match
     * the file name.
     *
     * @param content
     */
    public void setTextContent(String content) {
        textArea.replaceSelection(content);
        markModified(false);
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
        return (localFile.equals(this.localFile));
    }

    public ChangeListener bindToolbars() {
        textArea.wrapTextProperty().unbind();
        textArea.wrapTextProperty().bind(getParent().wrapToggle.selectedProperty());
        getParent().undoBtn.disableProperty().unbind();
        getParent().undoBtn.disableProperty().bind(textArea.undoAvailableProperty().map(x -> !x));
        getParent().redoBtn.disableProperty().unbind();
        getParent().redoBtn.disableProperty().bind(textArea.redoAvailableProperty().map(x -> !x));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            {
                bind(textArea.selectionProperty());
            }

            @Override
            protected boolean computeValue() {
                return textArea.getSelection().getLength() == 0;
            }
        };
        getParent().cutBtn.disableProperty().unbind();
        getParent().cutBtn.disableProperty().bind(selectionEmpty);
        getParent().copyBtn.disableProperty().unbind();
        getParent().copyBtn.disableProperty().bind(selectionEmpty);
        textArea.beingUpdatedProperty().removeListener(this);
        textArea.beingUpdatedProperty().addListener(this);
        return this;
    }

    @Override
    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean beingUpdated) {
        //Boolean beingUpdated = (Boolean) newValue;
        if (!beingUpdated) {
            boolean bold, italic, underline, strike;
            Integer fontSize;
            String fontFamily;
            Color textColor;
            Color backgroundColor;

            IndexRange selection = textArea.getSelection();
            if (selection.getLength() != 0) {
                StyleSpans<TextStyle> styles = textArea.getStyleSpans(selection);
                bold = styles.styleStream().anyMatch(s -> s.bold.orElse(false));
                italic = styles.styleStream().anyMatch(s -> s.italic.orElse(false));
                underline = styles.styleStream().anyMatch(s -> s.underline.orElse(false));
                strike = styles.styleStream().anyMatch(s -> s.strikethrough.orElse(false));
                int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                fontSize = sizes.length == 1 ? sizes[0] : -1;
                String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                fontFamily = families.length == 1 ? families[0] : null;
                Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                textColor = colors.length == 1 ? colors[0] : null;
                Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
            } else {
                int p = textArea.getCurrentParagraph();
                int col = textArea.getCaretColumn();
                TextStyle style = textArea.getStyleAtPosition(p, col);
                bold = style.bold.orElse(false);
                italic = style.italic.orElse(false);
                underline = style.underline.orElse(false);
                strike = style.strikethrough.orElse(false);
                fontSize = style.fontSize.orElse(-1);
                fontFamily = style.fontFamily.orElse(null);
                textColor = style.textColor.orElse(null);
                backgroundColor = style.backgroundColor.orElse(null);
            }

            int startPar = textArea.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
            int endPar = textArea.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
            List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = textArea.getParagraphs().subList(startPar, endPar + 1);

            @SuppressWarnings("unchecked")
            Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct().toArray(Optional[]::new);
            Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

            @SuppressWarnings("unchecked")
            Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor).distinct().toArray(Optional[]::new);
            Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0] : Optional.empty();

            getParent().updatingToolbar.suspendWhile(() -> {
                if (bold) {
                    if (!getParent().boldBtn.getStyleClass().contains("pressed")) {
                        getParent().boldBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getParent().boldBtn.getStyleClass().remove("pressed");
                }

                if (italic) {
                    if (!getParent().italicBtn.getStyleClass().contains("pressed")) {
                        getParent().italicBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getParent().italicBtn.getStyleClass().remove("pressed");
                }

                if (underline) {
                    if (!getParent().underlineBtn.getStyleClass().contains("pressed")) {
                        getParent().underlineBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getParent().underlineBtn.getStyleClass().remove("pressed");
                }

                if (strike) {
                    if (!getParent().strikeBtn.getStyleClass().contains("pressed")) {
                        getParent().strikeBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getParent().strikeBtn.getStyleClass().remove("pressed");
                }

                if (alignment.isPresent()) {
                    TextAlignment al = alignment.get();
                    switch (al) {
                        case LEFT:
                            getParent().alignmentGrp.selectToggle(getParent().alignLeftBtn);
                            break;
                        case CENTER:
                            getParent().alignmentGrp.selectToggle(getParent().alignCenterBtn);
                            break;
                        case RIGHT:
                            getParent().alignmentGrp.selectToggle(getParent().alignRightBtn);
                            break;
                        case JUSTIFY:
                            getParent().alignmentGrp.selectToggle(getParent().alignJustifyBtn);
                            break;
                    }
                } else {
                    getParent().alignmentGrp.selectToggle(null);
                }

                getParent().paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

                if (fontSize != -1) {
                    getParent().sizeCombo.getSelectionModel().select(fontSize);
                } else {
                    getParent().sizeCombo.getSelectionModel().clearSelection();
                }

                if (fontFamily != null) {
                    getParent().familyCombo.getSelectionModel().select(fontFamily);
                } else {
                    getParent().familyCombo.getSelectionModel().clearSelection();
                }

                if (textColor != null) {
                    getParent().textColorPicker.setValue(textColor);
                }

                getParent().backgroundColorPicker.setValue(backgroundColor);
            });
        }
    }

    public void setParent(ProxinoteController parent) {
        this.parent = parent;
    }

    public ProxinoteController getParent() {
        return parent;
    }

    private ProxinoteController parent = null;

    public void toggleBold() {
        updateStyleInSelection(spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    public void toggleItalic() {
        updateStyleInSelection(spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    public void toggleUnderline() {
        updateStyleInSelection(spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    public void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle.strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
    }

    public void alignLeft() {
        updateParagraphStyleInSelection(ParStyle.alignLeft());
    }

    public void alignCenter() {
        updateParagraphStyleInSelection(ParStyle.alignCenter());
    }

    public void alignRight() {
        updateParagraphStyleInSelection(ParStyle.alignRight());
    }

    public void alignJustify() {
        updateParagraphStyleInSelection(ParStyle.alignJustify());
    }

    private void updateParagraphStyleInSelection(ParStyle mixin) {
        updateParagraphStyleInSelection(style -> style.updateWith(mixin));
    }

    public void updateParagraphBackground(Color color) {
        updateParagraphStyleInSelection(ParStyle.backgroundColor(color));
    }

    public void updateBackgroundColor(Color color) {
        updateStyleInSelection(TextStyle.backgroundColor(color));
    }

    public void updateFontSize(Integer size) {
        updateStyleInSelection(TextStyle.fontSize(size));
    }

    public void updateFontFamily(String family) {
        updateStyleInSelection(TextStyle.fontFamily(family));
    }

    public void updateTextColor(Color color) {
        updateStyleInSelection(TextStyle.textColor(color));
    }

    @Override
    public void handle(Event event) {
        if (event.getEventType() == TAB_CLOSE_REQUEST_EVENT) {
            if (isModified()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save note ?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(parent.getMainApp().getIcon());
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    //parent.saveTabContent(this);
                }
                if (alert.getResult() == ButtonType.CANCEL) {
                    event.consume();
                } else {
                    parent.newEmptyTab();
                }
            } else {
                if (isEmpty() && parent.getTabCount() <= 1) {
                    event.consume();
                } else {
                    parent.newEmptyTab();
                }
            }
        }
    }

    public boolean isEmpty() {
        if (textArea.getText().isEmpty() && getText().contains("New Note")) {
            return true;
        }
        return false;
    }

    void requestFocus() {
        textArea.requestFocus();
    }
    
    
}

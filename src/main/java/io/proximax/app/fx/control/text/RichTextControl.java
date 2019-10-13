package io.proximax.app.fx.control.text;

import com.itextpdf.text.BaseColor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.StringUtils;
import java.awt.image.BufferedImage;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

/**
 *
 * @author vantran
 */
public class RichTextControl implements ChangeListener<Boolean> {

    private final TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
    private final LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();
    private static final String RTFX_FILE_EXTENSION = ".rtfx";
    private final boolean isRichText = true;
    private final SimpleBooleanProperty modified = new SimpleBooleanProperty(false);

    private IRichText parentWindow = null;

    private LocalAccount localAccount = null;
    private LocalFile localFile = null;
    protected final GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle> textArea = new GenericStyledArea<>(
            ParStyle.EMPTY, // default paragraph style
            (paragraph, style) -> paragraph.setStyle(style.toCss()), // paragraph style setter

            TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Tahoma").updateTextColor(Color.BLACK), // default
            // segment style
            styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()), // segment operations
            seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss()))); // Node creator and segment style
    // setter

    {
        textArea.setWrapText(true);
        textArea.setStyleCodecs(ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));

    }

    VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(
            textArea);

    public RichTextControl(ProxinoteController controller, LocalAccount localAccount, LocalFile localFile) {
        super();

        this.localAccount = localAccount;
        this.localFile = localFile;
        this.controller = controller;

        // setCenter(vsPane);
        if (StringUtils.isEmpty(localFile.fileName)) {
            localFile.fileName = "New Note";
        }

        textArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                markModified(true);
            }
        });
        textArea.requestFocus();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return textArea.focusedProperty();
    }

    public VirtualizedScrollPane getContainer() {
        return vsPane;
    }

    private Node createNode(StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
            BiConsumer<? super TextExt, TextStyle> applyStyle) {
        return seg.getSegment().unify(text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                LinkedImage::createNode);
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
                Tuple2<Codec<ParStyle>, Codec<StyledSegment<Either<String, LinkedImage>, TextStyle>>> codecs = textArea
                        .getStyleCodecs().get();
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument
                        .codec(codecs._1, codecs._2, textArea.getSegOps());
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
                Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec = ReadOnlyStyledDocument
                        .codec(codecs._1, codecs._2, textArea.getSegOps());
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

    public boolean exportPDF(String path) {
        com.itextpdf.text.Document pdfDocument = new com.itextpdf.text.Document();
        FontFactory.registerDirectories();
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(pdfDocument, new FileOutputStream(path + File.separator + getFileNameNoExt() + ".pdf"));
            if (writer != null) {
                pdfDocument.open();
                List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> paragraphs = textArea.getDocument().getParagraphs();
                int i = 0;
                for (Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> e : paragraphs) {
                    com.itextpdf.text.Paragraph pdfPara = new com.itextpdf.text.Paragraph();
                    int pA = Element.ALIGN_LEFT;
                    if (e.getParagraphStyle().alignment.isPresent()) {
                        switch (e.getParagraphStyle().alignment.get()) {
                            case CENTER:
                                pA = Element.ALIGN_CENTER;
                                break;
                            case RIGHT:
                                pA = Element.ALIGN_RIGHT;
                                break;
                            case JUSTIFY:
                                pA = Element.ALIGN_JUSTIFIED;
                                break;
                        }
                    }
                    pdfPara.setAlignment(pA);
                    for (StyledSegment<Either<String, LinkedImage>, TextStyle> styleSeg : e.getStyledSegments()) {
                        TextStyle style = styleSeg.getStyle();
                        Either<String, LinkedImage> segment = styleSeg.getSegment();
                        try {
                            if (segment.isLeft()) {
                                String txt = segment.getLeft();
                                Chunk el;
                                if (StringUtils.isEmpty(txt)) {
                                    el = new Chunk(Chunk.NEWLINE);
                                } else {
                                    el = new Chunk(txt);
                                }
                                if (style != null) {
                                    String fontFamily = "Tahoma";
                                    int fontSize = 12;
                                    if (style.fontFamily.isPresent()) {
                                        fontFamily = style.fontFamily.get();
                                    }
                                    if (style.fontSize.isPresent()) {
                                        fontSize = style.fontSize.get();
                                    }
                                    int fontStyle = com.itextpdf.text.Font.NORMAL;
                                    if (style.bold.isPresent() && style.bold.get()) {
                                        fontStyle = fontStyle | com.itextpdf.text.Font.BOLD;
                                    }
                                    if (style.italic.isPresent() && style.italic.get()) {
                                        fontStyle = fontStyle | com.itextpdf.text.Font.ITALIC;
                                    }
                                    if (style.underline.isPresent() && style.underline.get()) {
                                        fontStyle = fontStyle | com.itextpdf.text.Font.UNDERLINE;
                                    }
                                    if (style.strikethrough.isPresent() && style.strikethrough.get()) {
                                        fontStyle = fontStyle | com.itextpdf.text.Font.STRIKETHRU;
                                    }
                                    BaseColor fontColor = BaseColor.BLACK;
                                    if (style.textColor.isPresent()) {
                                        Color c = style.textColor.get();
                                        fontColor = new BaseColor(
                                                (int) (c.getRed() * 255),
                                                (int) (c.getGreen() * 255),
                                                (int) (c.getBlue() * 255));
                                    }
                                    //com.itextpdf.text.Font font = FontFactory.getFont(fontFamily, fontSize, fontStyle, fontColor);
                                    com.itextpdf.text.Font font = FontFactory.getFont(fontFamily, BaseFont.IDENTITY_H, true);
                                    font.setColor(fontColor);
                                    font.setSize(fontSize);
                                    font.setStyle(fontStyle);
                                    el.setFont(font);
                                    style.backgroundColor.ifPresent(b -> el.setBackground(new BaseColor(
                                            (int) (b.getRed() * 255),
                                            (int) (b.getGreen() * 255),
                                            (int) (b.getBlue() * 255))));
                                }
                                pdfPara.add(el);
                            } else if (segment.isRight()) {
                                LinkedImage image = segment.getRight();
                                String pathTemp = image.getImagePath();
                                Image img = Image.getInstance(pathTemp);
                                float w = img.getWidth();
                                if (w > 72 * 6) {
                                    //scale width not to be greater than 6 inches
                                    img.scalePercent((72 * 6) / w * 100);
                                }
                                img.setAlignment(pA);
                                Chunk el = new Chunk(img, 0, 0, true);
                                pdfPara.add(el);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    pdfDocument.add(pdfPara);
                }
                pdfDocument.close();
                writer.close();
                return true;
            }
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        return false;

    }

    public void exportWord(String path) {
        XWPFDocument wordDoc = new XWPFDocument();
        List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> paragraphs = textArea.getDocument().getParagraphs();
        for (Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> e : paragraphs) {
            XWPFParagraph wordPara = wordDoc.createParagraph();
            e.getParagraphStyle().alignment.ifPresent(a -> {
                ParagraphAlignment pA = ParagraphAlignment.LEFT;
                switch (a) {
                    case CENTER:
                        pA = ParagraphAlignment.CENTER;
                        break;
                    case RIGHT:
                        pA = ParagraphAlignment.RIGHT;
                        break;
                    case JUSTIFY:
                        pA = ParagraphAlignment.BOTH;
                        break;
                }
                wordPara.setAlignment(pA);
            });

            //wordPara.setFirstLineIndent(e.getParagraphStyle());
            for (StyledSegment<Either<String, LinkedImage>, TextStyle> styleSeg : e.getStyledSegments()) {
                //XWPFParagraph wordPara = wordDoc.createParagraph();
                XWPFRun wordRun = wordPara.createRun();
                Either<String, LinkedImage> segment = styleSeg.getSegment();
                TextStyle style = styleSeg.getStyle();
                if (style != null) {
                    style.bold.ifPresent(b -> wordRun.setBold(b));
                    style.fontFamily.ifPresent(b -> wordRun.setFontFamily(b));
                    style.fontSize.ifPresent(b -> wordRun.setFontSize(b));
                    style.italic.ifPresent(i -> wordRun.setItalic(i));
                    style.underline.ifPresent(u -> wordRun.setUnderline(u ? UnderlinePatterns.SINGLE : null));
                    style.strikethrough.ifPresent(s -> wordRun.setStrikeThrough(s));
                    style.textColor.ifPresent(c -> wordRun.setColor(String.format("%02X%02X%02X",
                            (int) (c.getRed() * 255),
                            (int) (c.getGreen() * 255),
                            (int) (c.getBlue() * 255))));
                    style.backgroundColor.ifPresent(b -> {
                        CTShd cTShd = wordRun.getCTR().addNewRPr().addNewShd();
                        cTShd.setVal(STShd.CLEAR);
                        cTShd.setColor("auto");
                        cTShd.setFill(String.format("%02X%02X%02X",
                                (int) (b.getRed() * 255),
                                (int) (b.getGreen() * 255),
                                (int) (b.getBlue() * 255)));

                    });
                }
                if (segment.isLeft()) {
                    String txt = segment.getLeft();
                    if (txt.startsWith("\t")) {
                        wordRun.addTab();
                    }
                    wordRun.setText(txt);
                } else if (segment.isRight()) {
                    LinkedImage image = segment.getRight();
                    String imgFile = image.getImagePath();
                    int format = 0;
                    if (imgFile.endsWith(".emf")) {
                        format = XWPFDocument.PICTURE_TYPE_EMF;
                    } else if (imgFile.endsWith(".wmf")) {
                        format = XWPFDocument.PICTURE_TYPE_WMF;
                    } else if (imgFile.endsWith(".pict")) {
                        format = XWPFDocument.PICTURE_TYPE_PICT;
                    } else if (imgFile.endsWith(".jpeg") || imgFile.endsWith(".jpg")) {
                        format = XWPFDocument.PICTURE_TYPE_JPEG;
                    } else if (imgFile.endsWith(".png")) {
                        format = XWPFDocument.PICTURE_TYPE_PNG;
                    } else if (imgFile.endsWith(".dib")) {
                        format = XWPFDocument.PICTURE_TYPE_DIB;
                    } else if (imgFile.endsWith(".gif")) {
                        format = XWPFDocument.PICTURE_TYPE_GIF;
                    } else if (imgFile.endsWith(".tiff")) {
                        format = XWPFDocument.PICTURE_TYPE_TIFF;
                    } else if (imgFile.endsWith(".eps")) {
                        format = XWPFDocument.PICTURE_TYPE_EPS;
                    } else if (imgFile.endsWith(".bmp")) {
                        format = XWPFDocument.PICTURE_TYPE_BMP;
                    } else if (imgFile.endsWith(".wpg")) {
                        format = XWPFDocument.PICTURE_TYPE_WPG;
                    }
                    try {
                        FileInputStream imgStream = new FileInputStream(imgFile);
                        BufferedImage img = ImageIO.read(imgStream);
                        double w = img.getWidth();
                        double h = img.getHeight();
                        imgStream.close();
                        double scaling = 1.0;
                        if (w > 72 * 6) {
                            scaling = (72 * 6) / w; //scale width not to be greater than 6 inches
                        }
                        imgStream = new FileInputStream(imgFile);
                        wordRun.addPicture(imgStream, format, imgFile, Units.toEMU(w * scaling), Units.toEMU(h * scaling)); // 200x200 pixels                        
                        imgStream.close();
                    } catch (Exception ex) {
                        // TODO Auto-generated catch block
                        ex.printStackTrace();
                    }
                }
            }
        }
        //});
        try {
            FileOutputStream os = new FileOutputStream(path + File.separator + getFileNameNoExt() + ".docx");
            wordDoc.write(os);
            wordDoc.close();
            os.close();
        } catch (Exception ex) {

            ex.printStackTrace();
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
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros = ReadOnlyStyledDocument
                    .fromSegment(Either.right(new RealLinkedImage(imagePath)), ParStyle.EMPTY, TextStyle.EMPTY,
                            textArea.getSegOps());
            textArea.replaceSelection(ros);
            markModified(true);
        }
    }

    public void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = textArea.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = textArea.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            textArea.setStyleSpans(selection.getStart(), newStyles);
            markModified(true);
        }
    }

    public void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = textArea.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = textArea.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            textArea.setStyleSpans(selection.getStart(), newStyles);
            markModified(true);
        }
    }

    public void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = textArea.getSelection();
        int startPar = textArea.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
        int endPar = textArea.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();
        for (int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = textArea.getParagraph(i);
            textArea.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
        markModified(true);
    }

    public String getFirstParagraph() {
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
            fileName = localFile.fileName;
        }
        // fileName = fileName.replaceAll("[\\\\/:*?\"<>|,]", "");
        fileName = fileName.replaceAll("[^A-Za-z0-9 ]", "");
        if (isSupportRichText() && textArea.getStyleCodecs().isPresent()) {
            fileName += RTFX_FILE_EXTENSION;
        }
        return fileName;
    }

    public String getFileNameNoExt() {
        String fileName = getDesc(CONST.MAX_LENGTH);
        if (StringUtils.isEmpty(fileName)) {
            fileName = localFile.fileName;
        }
        // fileName = fileName.replaceAll("[\\\\/:*?\"<>|,]", "");
        fileName = fileName.replaceAll("[^A-Za-z0-9 ]", "");
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
        try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis);) {
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
        textArea.wrapTextProperty().bind(getController().wrapToggle.selectedProperty());
        getController().undoBtn.disableProperty().unbind();
        getController().undoBtn.disableProperty().bind(textArea.undoAvailableProperty().map(x -> !x));
        getController().redoBtn.disableProperty().unbind();
        getController().redoBtn.disableProperty().bind(textArea.redoAvailableProperty().map(x -> !x));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            {
                bind(textArea.selectionProperty());
            }

            @Override
            protected boolean computeValue() {
                return textArea.getSelection().getLength() == 0;
            }
        };
        getController().cutBtn.disableProperty().unbind();
        getController().cutBtn.disableProperty().bind(selectionEmpty);
        getController().copyBtn.disableProperty().unbind();
        getController().copyBtn.disableProperty().bind(selectionEmpty);
        textArea.beingUpdatedProperty().removeListener(this);
        textArea.beingUpdatedProperty().addListener(this);
        return this;
    }

    @Override
    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean beingUpdated) {
        // Boolean beingUpdated = (Boolean) newValue;
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
                String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct()
                        .toArray(String[]::new);
                fontFamily = families.length == 1 ? families[0] : null;
                Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct()
                        .toArray(Color[]::new);
                textColor = colors.length == 1 ? colors[0] : null;
                Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct()
                        .toArray(i -> new Color[i]);
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
            List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = textArea.getParagraphs()
                    .subList(startPar, endPar + 1);

            @SuppressWarnings("unchecked")
            Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct()
                    .toArray(Optional[]::new);
            Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

            @SuppressWarnings("unchecked")
            Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor)
                    .distinct().toArray(Optional[]::new);
            Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0]
                    : Optional.empty();

            getController().updatingToolbar.suspendWhile(() -> {
                if (bold) {
                    if (!getController().boldBtn.getStyleClass().contains("pressed")) {
                        getController().boldBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getController().boldBtn.getStyleClass().remove("pressed");
                }

                if (italic) {
                    if (!getController().italicBtn.getStyleClass().contains("pressed")) {
                        getController().italicBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getController().italicBtn.getStyleClass().remove("pressed");
                }

                if (underline) {
                    if (!getController().underlineBtn.getStyleClass().contains("pressed")) {
                        getController().underlineBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getController().underlineBtn.getStyleClass().remove("pressed");
                }

                if (strike) {
                    if (!getController().strikeBtn.getStyleClass().contains("pressed")) {
                        getController().strikeBtn.getStyleClass().add("pressed");
                    }
                } else {
                    getController().strikeBtn.getStyleClass().remove("pressed");
                }

                if (alignment.isPresent()) {
                    TextAlignment al = alignment.get();
                    switch (al) {
                        case LEFT:
                            getController().alignmentGrp.selectToggle(getController().alignLeftBtn);
                            break;
                        case CENTER:
                            getController().alignmentGrp.selectToggle(getController().alignCenterBtn);
                            break;
                        case RIGHT:
                            getController().alignmentGrp.selectToggle(getController().alignRightBtn);
                            break;
                        case JUSTIFY:
                            getController().alignmentGrp.selectToggle(getController().alignJustifyBtn);
                            break;
                    }
                } else {
                    getController().alignmentGrp.selectToggle(null);
                }

                getController().paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

                if (fontSize != -1) {
                    getController().sizeCombo.getSelectionModel().select(fontSize);
                } else {
                    getController().sizeCombo.getSelectionModel().clearSelection();
                }

                if (fontFamily != null) {
                    getController().familyCombo.getSelectionModel().select(fontFamily);
                } else {
                    getController().familyCombo.getSelectionModel().clearSelection();
                }

                if (textColor != null) {
                    getController().textColorPicker.setValue(textColor);
                }

                getController().backgroundColorPicker.setValue(backgroundColor);
            });
        }
    }

    public void setController(ProxinoteController controller) {
        this.controller = controller;
    }

    public ProxinoteController getController() {
        return controller;
    }

    private ProxinoteController controller = null;

    public void toggleBold() {
        updateStyleInSelection(
                spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    public void toggleItalic() {
        updateStyleInSelection(
                spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    public void toggleUnderline() {
        updateStyleInSelection(
                spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    public void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle
                .strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
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

    public boolean isEmpty() {
        if (textArea.getText().isEmpty()) {
            return true;
        }
        return false;
    }

    void areaRequestFocus() {
        textArea.requestFocus();
    }

    public IRichText getWindowParent() {
        return parentWindow;
    }

    public void setParentWindow(IRichText parent) {
        this.parentWindow = parent;
    }

}

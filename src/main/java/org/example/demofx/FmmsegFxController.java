package org.example.demofx;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.example.demofx.ZhoConverter.*;

public class FmmsegFxController {
    private static final List<String> FILE_EXTENSIONS = Arrays.asList(".txt", ".xml", ".srt", ".ass", ".vtt", ".json", ".ttml2", ".csv", ".java", ".md", ".html", ".cs", ".py", ".cpp");
    private static final List<String> CONFIG_LIST = Arrays.asList(
            "s2t (简->繁)",
            "s2tw (简->繁台",
            "s2twp (简->繁台/惯)",
            "s2hk (简->繁港)",
            "t2s (繁->简)",
            "t2tw (繁->繁台)",
            "t2twp (繁->繁台/惯)",
            "t2hk (繁->繁港)",
            "tw2s (繁台->简)",
            "tw2sp (繁台->简/惯)",
            "tw2t (繁台->繁)",
            "tw2tp (繁台->繁/惯)",
            "hk2s (繁港->简)",
            "hk2t (繁港->繁)",
            "t2jp (日舊->日新)",
            "jp2t (日新->日舊)");
    @FXML
//    private TextArea textAreaSource;
    private CodeArea textAreaSource;
    @FXML
//    private TextArea textAreaDestination;
    private CodeArea textAreaDestination;
    @FXML
//    private TextArea textAreaPreview;
    private CodeArea textAreaPreview;
    @FXML
    private RadioButton rbS2t;
    @FXML
    private RadioButton rbT2s;
    @FXML
    private RadioButton rbStd;
    @FXML
    private RadioButton rbHK;
    @FXML
    private RadioButton rbManual;
    @FXML
    private CheckBox cbZHTW;
    @FXML
    private CheckBox cbPunctuation;
    @FXML
    private Label lblSourceCode;
    @FXML
    private Label lblDestinationCode;
    @FXML
    private Label lblSourceCharCount;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblFilename;
    @FXML
    private Tab tabMain;
    @FXML
    private Tab tabBatch;
    @FXML
    private Button btnOpenFile;
    @FXML
    private ListView<String> listViewSource;
    @FXML
    private TextField textFieldPath;
    @FXML
    private ComboBox<String> cbManual;

    @FXML
    public void initialize() {
        cbManual.getItems().addAll(CONFIG_LIST);
        cbManual.getSelectionModel().selectFirst();
        textAreaSource.setParagraphGraphicFactory(LineNumberFactory.get(textAreaSource));
        textAreaDestination.setParagraphGraphicFactory(LineNumberFactory.get(textAreaDestination));
    }

    private boolean isOpenFileDisabled = false;
    private String openFileName;

    @FXML
    protected void onBtnPasteClick() {
        retryClipboardPaste(3, 100); // Retry 3 times with 100ms delay
    }

    private void retryClipboardPaste(int retries, int delayMs) {
        if (retries <= 0) {
            lblStatus.setText("Clipboard is empty or could not retrieve contents.");
            return;
        }

        String inputText = getClipboardTextFx();
        if (!inputText.isEmpty()) {
            // If clipboard has content, update the text area and UI
//            textAreaSource.setText(inputText);
            textAreaSource.replaceText(inputText);
            openFileName = "";
            updateSourceInfo(zhoCheck(inputText));
            lblFilename.setText("");
            lblStatus.setText("Clipboard contents pasted to source area.");
        } else {
            // Use PauseTransition to retry after delay without blocking the UI thread
            PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
            pause.setOnFinished(event -> retryClipboardPaste(retries - 1, delayMs)); // Retry
            pause.play();
        }
    }

    private String getClipboardTextFx() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            return clipboard.getString();
        } else {
            return "";
        }
    }

    @FXML
    protected void onBtnCopyClicked() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(textAreaDestination.getText());
        clipboard.setContent(content);
        lblStatus.setText("Destination contents copied to clipboard.");
    }

    // Helper method to get file extension
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return fileName.substring(lastDotIndex);
        }
        return null;
    }

    public void onBtnExitClicked(MouseEvent mouseEvent) {
        final Node source = (Node) mouseEvent.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    public void onBtnStartClicked() {
        // Main Conversion
        if (tabMain.isSelected()) {
            startMainConversion();
        }
        // batch Conversion
        if (tabBatch.isSelected()) {
            startBatchConversion();
        }
    }

    private void startMainConversion() {
        String inputText = textAreaSource.getText();
        if (inputText.isEmpty()) {
            lblStatus.setText("Nothing to convert.");
            return;
        }
        var config = getConfig();
        String convertedText = convert(inputText, config, cbPunctuation.isSelected());

        textAreaDestination.replaceText(convertedText);
        if (rbManual.isSelected()) {
            lblDestinationCode.setText(cbManual.getValue() != null ? cbManual.getValue() : config);
        } else if (!lblSourceCode.getText().contains("non") && !lblSourceCode.getText().isEmpty()) {
            lblDestinationCode.setText(lblSourceCode.getText().contains("Hans") ? "zh-Hant (繁体)" : "zh-Hans (简体)");
        } else {
            lblDestinationCode.setText(lblSourceCode.getText());
        }
        lblStatus.setText(String.format("Conversion process completed. [ %s ]", config));
    }

    private void startBatchConversion() {
        ObservableList<String> fileList = listViewSource.getItems();
        if (fileList.isEmpty()) {
            lblStatus.setText("Empty file list.\n");
            return;
        }

        String outputDirectory = textFieldPath.getText();
        Path outputDirectoryPath = Paths.get(outputDirectory);
        if (outputDirectory.isEmpty() || !Files.isDirectory(outputDirectoryPath)) {
            textAreaPreview.appendText(String.format("Output directory [ %s ] does not exist.\n", outputDirectory));
            return;
        }
        textAreaPreview.clear();
        final String config = getConfig();
        String convertedText;
        int counter = 0;
        for (String file : fileList) {
            counter++;
            if (!FILE_EXTENSIONS.contains(getFileExtension(file))) {
                textAreaPreview.appendText(String.format("%d : [Skipped] %s --> Not a valid file format.\n", counter, file));
            } else {
                File sourceFilePath = new File(file);
                String outputFilename = sourceFilePath.getName();
                Path outputFilePath = outputDirectoryPath.resolve(outputFilename);
                try {
                    String contents = Files.readString(new File(file).toPath());
                    convertedText = convert(contents, config, cbPunctuation.isSelected());

                    // Write string contents to the file with UTF-8 encoding
                    Files.writeString(outputFilePath, convertedText);
                    textAreaPreview.appendText(String.format("%d : %s --> [Done].\n", counter, outputFilePath));

                } catch (Exception e) {
//                    throw new RuntimeException(e);
                    textAreaPreview.appendText(String.format("%d : [Skipped] %s --> Error writing output file.\n", counter, outputFilePath));
                }
            }
        }
        textAreaPreview.appendText("Process completed.\n");
        lblStatus.setText("Batch conversion process completed.");
    }

    private void updateSourceInfo(int textCode) {
        switch (textCode) {
            case 1:
                rbT2s.setSelected(true);
                lblSourceCode.setText("zh-Hant (繁体)");
                break;
            case 2:
                rbS2t.setSelected(true);
                lblSourceCode.setText("zh-Hans (简体)");
                break;
            default:
                lblSourceCode.setText("non-zho (其它)");
        }
        lblSourceCharCount.setText(String.format("[ %,d chars ]", textAreaSource.getText().length()));
        if (!openFileName.isEmpty()) {
            lblFilename.setText(new File(openFileName).getName());
            lblStatus.setText(openFileName);
        } else {
            lblFilename.setText(openFileName);
        }
    }

    private String getConfig() {
        String config = "s2t";
        if (rbS2t.isSelected()) {
            config = rbStd.isSelected() ? "s2t" : (rbHK.isSelected() ? "s2hk" : (cbZHTW.isSelected() ? "s2twp" : "s2tw"));
        }
        if (rbT2s.isSelected()) {
            config = rbStd.isSelected() ? "t2s" : (rbHK.isSelected() ? "hk2s" : (cbZHTW.isSelected() ? "tw2sp" : "tw2s"));
        }
        if (rbManual.isSelected()) {
            if (cbManual.getValue() != null) {
                config = cbManual.getValue().split(" ")[0];
            }
        }
        return config;
    }

    public void onBtnOpenFileClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Text File");
        fileChooser.setInitialDirectory(new File("."));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Subtitle Files", "*.srt;*.vtt;*.ass;*.xml;*.ttml2"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            if (selectedFile.isFile() && selectedFile.exists()) {
                displayFileContents(selectedFile);
            } else {
                lblStatus.setText("Selected file is not valid.");
            }
        }
    }

    private void displayFileContents(File file) {
        try {
            String content = readUtf8TextFile(file);
            textAreaSource.replaceText(content);
            openFileName = file.toString();
            updateSourceInfo(zhoCheck(content));
        } catch (IOException e) {
            lblStatus.setText("Error reading file: " + e.getMessage());
        }
    }

    public static String readUtf8TextFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = in.readAllBytes();

            // Remove UTF-8 BOM if present
            int bomLength = 0;
            if (bytes.length >= 3 &&
                    bytes[0] == (byte) 0xEF &&
                    bytes[1] == (byte) 0xBB &&
                    bytes[2] == (byte) 0xBF) {
                bomLength = 3;
            }

            return new String(bytes, bomLength, bytes.length - bomLength, StandardCharsets.UTF_8);
        }
    }

    public void onSourceTextChanged() {
        lblSourceCharCount.setText(String.format("[ %,d chars ]", textAreaSource.getText().length()));
    }

    public void onRbStdClicked() {
        cbZHTW.setSelected(false);
        cbZHTW.setDisable(true);
    }

    public void onRbZhtwClicked() {
        cbZHTW.setSelected(true);
        cbZHTW.setDisable(false);
    }

    public void onTabMainSelectionChanged() {
        if (!tabMain.isSelected()) {
            return;
        }
        if (isOpenFileDisabled) {
            btnOpenFile.setDisable(false);
            isOpenFileDisabled = false;
        }
    }

    public void onTabBatchSelectionChanged() {
        if (!tabBatch.isSelected()) {
            return;
        }
        if (tabBatch.isSelected()) {
            btnOpenFile.setDisable(true);
            isOpenFileDisabled = true;
        }
    }

    public void onBtnAddClicked() {
        ObservableList<String> currentFileList = listViewSource.getItems();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files");
        fileChooser.setInitialDirectory(new File("."));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Subtitle Files", "*.srt;*.vtt;*.ass;*.xml;*.ttml2"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null) {
            // Check for duplicate files and add only new files to the list
            int count = 0;
            for (File file : selectedFiles) {
                if (!currentFileList.contains(file.getAbsolutePath())) {
                    currentFileList.add(file.getAbsolutePath());
                    count++;
                }
            }
            FXCollections.sort(currentFileList, null);
            listViewSource.setItems(currentFileList);
            lblStatus.setText(String.format("No of file added to list: %d", count));
        }
    }

    public void onBtnRemoveClicked() {
        ObservableList<String> currentFileList = listViewSource.getItems();
        String selectedItem = listViewSource.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            currentFileList.remove(selectedItem);
            lblStatus.setText(String.format("[%s] removed.", selectedItem));
        }
        listViewSource.setItems(currentFileList);
    }

    public void onBtnClearListClicked() {
        ObservableList<String> currentFileList = listViewSource.getItems();
        currentFileList.clear();
        listViewSource.setItems(currentFileList);
        lblStatus.setText("File list cleared.");
    }

    public void onBtnPreviewSourceClicked() {
        String selectedItem = listViewSource.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            File file = new File(selectedItem);
            String fileExtension = getFileExtension(file.getName());
            if (file.isFile() && FILE_EXTENSIONS.contains(fileExtension != null ? fileExtension.toLowerCase() : null)) {
                try {
                    String content = Files.readString(file.toPath());
//                    textAreaPreview.setText(content);
                    textAreaPreview.replaceText(content);
                    lblStatus.setText(String.format("File Preview: %s", selectedItem));
                } catch (IOException e) {
                    textAreaPreview.replaceText("Error reading file: " + e.getMessage());
                }
            } else {
//                textAreaPreview.setText("Selected file is not a valid text file.");
                textAreaPreview.replaceText("Selected file is not a valid text file.");
            }
        }
    }

    public void onBtnSelectPathClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        directoryChooser.setInitialDirectory(new File("."));
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            textFieldPath.setText(selectedDirectory.getAbsolutePath());
            lblStatus.setText("Output path set to: " + selectedDirectory);
        }
    }

    public void onBtnClearPreviewClicked() {
        textAreaPreview.clear();
        lblStatus.setText("Preview cleared.");
    }

    public void onBtnSaveAsClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Text File");
        fileChooser.setInitialDirectory(new File("."));
        fileChooser.setInitialFileName("File.txt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Subtitle Files", "*.srt;*.vtt;*.ass;*.xml;*.ttml2"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showSaveDialog(null);

        if (selectedFile != null) {
            try {
                String contents = textAreaDestination.getText();
                // Write string contents to the file with UTF-8 encoding
                Files.writeString(selectedFile.toPath(), contents);
                lblStatus.setText(String.format("Output contents saved to: %s", selectedFile));
            } catch (Exception e) {
                lblStatus.setText(String.format("Error writing output file: %s", selectedFile));
            }
        }
    }

    public void onTaSourceDragOver(DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();

        if (dragEvent.getGestureSource() != textAreaSource &&
                (dragboard.hasFiles() || dragboard.hasString())) {

            dragEvent.acceptTransferModes(TransferMode.COPY);
        }

        dragEvent.consume();
    }


    public void onTaDragDropped(DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();
        boolean success = false;

        if (dragboard.hasFiles()) {
            File file = dragboard.getFiles().getFirst();

            if (isTextFile(file)) {
                try {
                    String content = readUtf8TextFile(file);
                    textAreaSource.replaceText(content);
                    openFileName = file.toString();
                    updateSourceInfo(zhoCheck(content));
                    success = true;
                } catch (IOException e) {
                    lblStatus.setText("Error: " + e.getMessage());
                }
            } else {
                textAreaSource.replaceText("Not a valid text file.");
            }
        } else if (dragboard.hasString()) {
            // User dragged in plain text
            String text = dragboard.getString();

            // Remove BOM if present (just in case)
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }

            textAreaSource.replaceText(text);
            openFileName = "<pasted text>";
            updateSourceInfo(zhoCheck(text));
            success = true;
        }

        dragEvent.setDropCompleted(success);
        dragEvent.consume();
    }

    private boolean isTextFile(File file) {
        String fileExtension = getFileExtension(file.getName());
        return file.isFile() && FILE_EXTENSIONS.contains(fileExtension != null ? fileExtension.toLowerCase() : null);
    }

    public void onLivSourceDragOver(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != listViewSource && dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.COPY);
        }
        dragEvent.consume();
    }

    public void onLvDragDropped(DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();
        boolean success = false;
        if (dragboard.hasFiles()) {
            ObservableList<String> fileList = listViewSource.getItems();
            List<File> files = dragboard.getFiles();
            int count = 0;
            for (File file : files) {
                if (isTextFile(file) && !fileList.contains(file.getAbsolutePath())) {
                    fileList.add(file.getAbsolutePath());
                    count++;
                    success = true;
                }
            }
            FXCollections.sort(fileList, null);
            listViewSource.setItems(fileList);
            lblStatus.setText(String.format("Total file added: %d", count));
        }
        dragEvent.setDropCompleted(success);
        dragEvent.consume();
    }

    public void onBthRefreshClicked() {
        updateSourceInfo(zhoCheck(textAreaSource.getText()));
    }

    public void onCbManualClicked() {
        rbManual.setSelected(true);
    }

    public void onBthClearSourceClicked() {
        textAreaSource.replaceText("");
    }

    public void onBthClearDestinationClicked() {
        textAreaDestination.replaceText("");
    }
} // class DemoFxController
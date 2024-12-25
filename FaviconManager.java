import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class FaviconManager extends JFrame implements DropTargetListener {

    private File addImageFolder;
    private File exportFolder;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private static final Logger LOGGER = Logger.getLogger(FaviconManager.class.getName());

    public FaviconManager() {
        setTitle("Favicon Manager");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Enable drag and drop
        new DropTarget(this, this);

        // Setup folders on desktop
        setupFolders();

        // Status label for user feedback
        statusLabel = new JLabel("Drag and drop images or folders to create favicons.", JLabel.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Progress bar
        initializeProgressBar();

        // Logging setup
        setupLogging();

        // Menu bar with settings
        initializeMenuBar();
    }

    private void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler("favicon_manager.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupFolders() {
        String userDesktop = System.getProperty("user.home") + File.separator + "Desktop";
        addImageFolder = new File(userDesktop + File.separator + "Add Image");
        exportFolder = new File(userDesktop + File.separator + "Exported Favicon Image Set");

        // Create folders if they don't exist
        for (File folder : new File[]{addImageFolder, exportFolder}) {
            if (!folder.exists()) {
                folder.mkdir();
            }
        }
    }

    private void initializeProgressBar() {
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        add(progressBar, BorderLayout.NORTH);
    }

    private void initializeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu settingsMenu = new JMenu("Settings");

        JMenuItem customSizesItem = new JMenuItem("Custom Sizes");
        customSizesItem.addActionListener(e -> getCustomSizes());

        JMenuItem toggleDarkModeItem = new JMenuItem("Toggle Dark Mode");
        toggleDarkModeItem.addActionListener(e -> toggleDarkMode());

        JMenuItem exportAsZipItem = new JMenuItem("Export as ZIP");
        exportAsZipItem.addActionListener(e -> exportAsZip(exportFolder));

        JMenuItem previewFaviconsItem = new JMenuItem("Preview Favicons");
        previewFaviconsItem.addActionListener(e -> previewFavicons());

        JMenuItem generatePDFItem = new JMenuItem("Generate PDF Report");
        generatePDFItem.addActionListener(e -> generatePdfReport(exportFolder));

        JMenuItem extractColorsItem = new JMenuItem("Extract Colors");
        extractColorsItem.addActionListener(e -> extractColors(exportFolder));

        settingsMenu.add(customSizesItem);
        settingsMenu.add(toggleDarkModeItem);
        settingsMenu.add(exportAsZipItem);
        settingsMenu.add(previewFaviconsItem);
        settingsMenu.add(generatePDFItem);
        settingsMenu.add(extractColorsItem);

        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private String[] getCustomSizes() {
        String input = JOptionPane.showInputDialog(
            this,
            "Enter custom sizes separated by commas (e.g., 16,32,64):",
            "Custom Sizes",
            JOptionPane.PLAIN_MESSAGE
        );
        return input != null ? input.split(",") : new String[0];
    }

    private void toggleDarkMode() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void exportAsZip(File folder) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(folder, "favicons.zip")))) {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
            updateStatus("Favicons exported as ZIP successfully.");
            LOGGER.info("Favicons exported as ZIP: favicons.zip");
        } catch (IOException e) {
            LOGGER.severe("Failed to create ZIP file: " + e.getMessage());
            updateStatus("Error creating ZIP file.");
        }
    }

    private void previewFavicons() {
        SwingUtilities.invokeLater(() -> {
            JFrame previewFrame = new JFrame("Favicon Preview");
            previewFrame.setSize(400, 300);
            previewFrame.setLayout(new GridLayout(0, 3, 10, 10));

            File[] files = exportFolder.listFiles((dir, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    try {
                        BufferedImage image = ImageIO.read(file);
                        ImageIcon icon = new ImageIcon(image.getScaledInstance(64, 64, Image.SCALE_SMOOTH));
                        JLabel label = new JLabel(icon);
                        label.setHorizontalAlignment(JLabel.CENTER);
                        previewFrame.add(label);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to load preview for: " + file.getName());
                    }
                }
            }

            previewFrame.setVisible(true);
        });
    }

    private void generatePdfReport(File folder) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.beginText();
            contentStream.setLeading(14.5f);
            contentStream.newLineAtOffset(25, 750);

            contentStream.showText("Favicon Report");
            contentStream.newLine();
            contentStream.newLine();

            File[] files = folder.listFiles((dir, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    contentStream.showText(file.getName());
                    contentStream.newLine();
                }
            }

            contentStream.endText();
            contentStream.close();

            doc.save(new File(folder, "favicon_report.pdf"));
            updateStatus("PDF Report Generated Successfully.");
        } catch (IOException e) {
            LOGGER.severe("Failed to generate PDF report: " + e.getMessage());
        }
    }

    private void extractColors(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    Map<String, Integer> colorMap = new HashMap<>();

                    for (int x = 0; x < image.getWidth(); x++) {
                        for (int y = 0; y < image.getHeight(); y++) {
                            int rgb = image.getRGB(x, y);
                            String hex = String.format("#%06X", (0xFFFFFF & rgb));
                            colorMap.put(hex, colorMap.getOrDefault(hex, 0) + 1);
                        }
                    }

                    colorMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(entry -> LOGGER.info(file.getName() + " - Color: " + entry.getKey() + " Count: " + entry.getValue()));

                } catch (IOException e) {
                    LOGGER.warning("Failed to extract colors for: " + file.getName());
                }
            }
        }
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // Optional: Handle drag exit if needed
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // Optional: Handle drag over event if needed
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // Optional: Handle action change if needed
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            Object transferData = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (transferData instanceof List) {
                List<File> processedFiles = new ArrayList<>();
                for (Object fileObj : (List<?>) transferData) {
                    if (fileObj instanceof File) {
                        File file = (File) fileObj;
                        if (file.isDirectory()) {
                            processFolder(file, processedFiles);
                        } else if (isValidImage(file)) {
                            generateFavicons(file, processedFiles);
                        } else if (file.getName().toLowerCase().endsWith(".zip")) {
                            extractAndProcessZip(file, processedFiles);
                        } else {
                            updateStatus("Invalid file format: " + file.getName());
                        }
                    }
                }
                generateHtmlDocumentation(processedFiles);
            }
            dtde.dropComplete(true);
        } catch (Exception e) {
            LOGGER.severe("Error during drop: " + e.getMessage());
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

    private void processFolder(File folder, List<File> processedFiles) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processFolder(file, processedFiles);
                } else if (isValidImage(file)) {
                    generateFavicons(file, processedFiles);
                }
            }
        }
    }

    private void extractAndProcessZip(File zipFile, List<File> processedFiles) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isValidImage(new File(entry.getName()))) {
                    File tempFile = File.createTempFile("favicon_temp", ".png");
                    Files.copy(zis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    generateFavicons(tempFile, processedFiles);
                    tempFile.deleteOnExit();
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Failed to process ZIP file: " + zipFile.getName());
        }
    }

    private boolean isValidImage(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".gif") || fileName.endsWith(".bmp") || fileName.endsWith(".webp");
    }

    private void generateFavicons(File sourceImage, List<File> processedFiles) {
        String[] sizes = {"16x16", "32x32", "48x48", "64x64", "128x128"};

        try {
            BufferedImage originalImage = ImageIO.read(sourceImage);
            for (String size : sizes) {
                int dimension = Integer.parseInt(size.split("x")[0]);
                BufferedImage resizedImage = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = resizedImage.createGraphics();
                graphics.drawImage(originalImage, 0, 0, dimension, dimension, null);
                graphics.dispose();

                File output = new File(exportFolder, "favicon-" + size + ".png");
                if (output.exists()) {
                    String uniqueName = "favicon-" + size + "-" + System.currentTimeMillis() + ".png";
                    output = new File(exportFolder, uniqueName);
                }
                ImageIO.write(resizedImage, "png", output);
                processedFiles.add(output);
            }
            updateStatus("Favicons generated successfully.");
            LOGGER.info("Favicons generated and saved to " + exportFolder.getAbsolutePath());
            openExportFolder();
        } catch (IOException e) {
            LOGGER.severe("Failed to generate favicons: " + e.getMessage());
            updateStatus("Error generating favicons.");
        }
    }

    private void generateHtmlDocumentation(List<File> generatedFiles) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h1>Generated Favicons</h1><ul>");
        for (File file : generatedFiles) {
            html.append("<li>").append(file.getName()).append("</li>");
        }
        html.append("</ul></body></html>");
        try {
            Files.writeString(Paths.get(exportFolder.getAbsolutePath(), "favicon_report.html"), html.toString());
        } catch (IOException e) {
            LOGGER.severe("Failed to generate HTML documentation: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private void openExportFolder() {
        try {
            Desktop.getDesktop().open(exportFolder);
        } catch (IOException e) {
            LOGGER.severe("Failed to open export folder: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FaviconManager().setVisible(true));
    }
}

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class FaviconManager extends JFrame implements DropTargetListener {

    private File addImageFolder;
    private File exportFolder;

    public FaviconManager() {
        setTitle("Favicon Manager");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Enable drag and drop
        new DropTarget(this, this);

        // Setup folders on desktop
        setupFolders();
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
                for (Object fileObj : (List<?>) transferData) {
                    if (fileObj instanceof File) {
                        File file = (File) fileObj;
                        if (isValidImage(file)) {
                            generateFavicons(file);
                        }
                    }
                }
            }
            dtde.dropComplete(true);
        } catch (Exception e) {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

    private boolean isValidImage(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg");
    }

    private void generateFavicons(File sourceImage) {
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
                ImageIO.write(resizedImage, "png", output);
            }
            System.out.println("Favicons generated and saved to " + exportFolder.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to generate favicons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FaviconManager().setVisible(true));
    }
}

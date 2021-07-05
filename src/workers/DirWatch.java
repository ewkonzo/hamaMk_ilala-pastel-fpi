package workers;


import main.Controller;
import main.Main;
import utils.FileManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static persistence.Settings.*;

public class DirWatch implements Callable {

    private WatchService watchService;
    private FileManager fileManager = new FileManager();
    private Preferences prefs = getInstance().prefs;
    private final static Logger LOGGER = Logger.getLogger(DirWatch.class.getName());

    private static double receiptTotal;
    private static double outputTax;
    private String watchDirPath;
    private DecimalFormat decimalFormat = new DecimalFormat("##.00");

    //check os
    boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public DirWatch(String watchDirPath, Controller controller) {
        Path path = Paths.get(watchDirPath);
        this.watchDirPath = watchDirPath;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, ENTRY_CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.setLevel(Level.INFO);
    }

    @Override
    public Object call()  {
        //process documents already in the output folder if exists

//        Todo reimplement scan scan existing
        try {
            scanExisting(watchDirPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean poll = true;
        while (poll) {

            WatchKey key = null;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assert key != null;
            for (WatchEvent<?> event : key.pollEvents()) {
//                System.out.println("Event kind : " + event.kind() + " - File : " + event.context());

                // put the thread to sleep briefly to allow the OS to release the lock on the file in question
                if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    processDocument(event.context().toString());
                }
            }

            poll = key.reset();
        }
        return null;
    }

    //Look for text files already in the watch dir on service start
    private void scanExisting(String watchDir) {
        File dir = new File(watchDir);
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.lastIndexOf('.')>0) {
                    // get last index for '.' char
                    int lastIndex = name.lastIndexOf('.');
                    // get extension
                    String str = name.substring(lastIndex);
                    // match path name extension
                    return str.equalsIgnoreCase(".txt") || str.equalsIgnoreCase(".ps");
                }
                return false;
            }
        };

        if(dir.exists() && dir.isDirectory()){
            String[] files = dir.list(filenameFilter);
            for(String file: files){
                LOGGER.info("Existing file found: " + file);
                processDocument(file);
            }
        }
    }

    private void processDocument(String fileName) {

        if(fileName.contains(".txt")){
            ProcessBuilder builder = new ProcessBuilder();
            if (isWindows) {


            } else {

            }
            String inputFilePath = prefs.get(INPUT, Main.INPUT_PATH).concat(File.separator).concat(fileName);
            List<String> fileContents = null;
            try {
                fileContents = new FileManager().readTextFile(inputFilePath);
                createReceiptText(fileContents, inputFilePath, fileName);
                LOGGER.info(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createReceiptText(List<String> rawReceiptData, String inputFilePath, String fileName){
        LOGGER.info("Creating receipt text ...");
        int qty = 1;
        BigDecimal price = BigDecimal.ZERO;
        String description = "";
        List<String> processedReceiptData = new ArrayList<>();

            for(String line: rawReceiptData){
                line = line.trim();
                try{

                    if (isSale(line)){
                        String[] lineComponents = line.split("  ");
                        //remove empty positions
                        List<String> lineData = new ArrayList<>();
                        for(String comp: lineComponents){
                            if(!comp.equals(""))
                                lineData.add(comp.trim());
                        }
                        description = lineData.get(0);
                        price = new BigDecimal(lineData.get(1));
                        qty = (int) Math.round(Double.parseDouble(lineData.get(2)));

                        String processedLine = "R_TRP    \"" + description + "\" " + qty + " * " + price + "V3";
                        processedReceiptData.add(processedLine);
                    }
                    else {
                        if(!line.contains(description) | description.equals("")){
                            String processedLine = "R_TXT    \"" + line + "\"";
                            processedReceiptData.add(processedLine);
                        }
                    }
                }catch (Exception e){
                    LOGGER.severe(e.getMessage());
                }
            }

        //write processed file to text document
        try {
            String outputFilePath = prefs.get(EFP_INPUT, Main.EFP_INPUT_PATH).concat(File.separator).concat(fileName);
            String backupFileSavePath = prefs.get(OUTPUT, Main.OUTPUT_PATH).concat(File.separator).concat(fileName);

            Files.write(Paths.get(outputFilePath), processedReceiptData);
            //TODO uncomment line below in production
            Files.move(Paths.get(inputFilePath), Paths.get(backupFileSavePath), REPLACE_EXISTING); //move file to backup location
            LOGGER.info("Receipt processing and saving successful");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isBill(List<String> content){
        return content.stream().anyMatch((line)->line.toLowerCase().contains("sign"));
    }

    public double extractAmount(String line) throws Exception {
        double amount = 0;
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            amount = Double.parseDouble(matcher.group(0));
//            LOGGER.info("extracted amount: >> " + amount);
        } else {
            throw new Exception("Failed to extract amount from line:: " + line);
        }
        return amount;
    }

    public boolean isSale(String line) {
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        int matches = 0;
        String[] lineComponents = line.split("  ");
        for(String comp: lineComponents){
            if(pattern.matcher(comp).find())
                matches += 1;
            if(matches == 3)
                return true;
        }
        return false;
    }
}

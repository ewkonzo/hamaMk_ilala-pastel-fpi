package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileManager {


    public synchronized void writeToTextFile(String textContent, String filePath) throws IOException {
            FileWriter myWriter = new FileWriter(filePath);
            myWriter.write(textContent);
            myWriter.close();
    }

    public synchronized List<String> readTextFile(String filePath) throws IOException {
        // read each line of the text file and save as a string list
        return Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
    }
}

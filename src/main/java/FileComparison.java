import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileComparison {
    public static int compareFileByByte(Path File_One, Path File_Two, Logger logger) {
        int idx = 0;
        try {
            long fileOne_size = Files.size(File_One);
            long fileTwo_size = Files.size(File_Two);

            if (fileOne_size < Constants.blockSize || fileTwo_size < Constants.blockSize) {
                return idx;
            }
            // Compare byte-by-byte
            try (FileInputStream first = new FileInputStream(String.valueOf(File_One));
                 FileInputStream second = new FileInputStream(String.valueOf(File_Two)))
            {
                byte[] chunk1, chunk2;
                do {
                    chunk1 = first.readNBytes(Constants.blockSize);
                    chunk2 = second.readNBytes(Constants.blockSize);
                    logger.info(chunk1.length +" "+ chunk2.length +" "+ idx);
                    if(!Arrays.equals(chunk1, chunk2)){
                        logger.info("Change detected at "+idx);
                        return idx;
                    }
                    idx++;

                }while (chunk1.length>0 && chunk2.length>0);

                System.out.println(idx);
            }catch (Exception e){
                e.printStackTrace();
                return 0;
            }
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }
}

package eu.kairat.tools.logfileMerger;

import com.beust.jcommander.Parameter;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Boris.Kairat on 30.10.2015.
 */
public class Merger {

    private static final Pattern LOG_LINE_DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (.*)$");
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");

    @Parameter(names = "-logfileSourcePath",
            description = "The path where the tool can find the logfiles to merge.")
    String logfileSourcePath;

    @Parameter(names = "-logfileSourceFileExtension",
            description = "The file extension of the logfiles to merge. (default = \"log\")")
    String logfileSourceFileExtension = "log";

    @Parameter(names = "-logfileTargetPath",
            description = "The path where the tool creates the logfile containing the merging result.")
    String logfileTargetPath = ".\\";

    @Parameter(names = "-logfileTargetName",
            description = "The name of the logfile containing the merging result.")
    String logfileTargetName = "mergedLogfile.log";

    private void checkArguments() {
        // check source file extension
        if(null == logfileSourceFileExtension) throw new RuntimeException("logfileSourceFileExtension is null");
        Pattern p = Pattern.compile("^\\w+$");
        Matcher m = p.matcher(logfileSourceFileExtension);
        if(!m.matches()) throw new RuntimeException("logfileSourceFileExtension is invalid");

        // check source path
        if(null == logfileSourcePath) throw new RuntimeException("logfileSourcePath is null");
        if(!Files.isDirectory(Paths.get(logfileSourcePath))) throw new RuntimeException("logfileSourcePath is not a directory");

        // check logfile target name
        if(null == logfileTargetName) throw new RuntimeException("logfileTargetName is null");
        p = Pattern.compile("^[\\w,\\s-]+\\.[A-Za-z]+$");
        m = p.matcher(logfileTargetName);
        if(!m.matches()) throw new RuntimeException("logfileTargetName is invalid");

        // check source path
        if(null == logfileTargetPath) throw new RuntimeException("logfileTargetPath is null");
        if(!Files.isDirectory(Paths.get(logfileTargetPath))) throw new RuntimeException("logfileSourcePath is not a directory");
    }

    void merge() throws Exception {

        System.out.println("CHECKING ARGUMENTS...");
        checkArguments();

        final List<Line> lineObjects = new LinkedList<Line>();

        int maxSourceChars = 0;
        int processedFiles = 0;
        int processedMessages = 0;
        int processedLines = 0;

        final Path dir = Paths.get(logfileSourcePath);

        Line lastItem = null;


        System.out.println("READING FILES...");
        final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{" + logfileSourceFileExtension + "}");
        for (Path entry : stream) {
            final String source = entry.getFileName().toString().replaceFirst("[.][^.]+$", "");
            maxSourceChars = Math.max(maxSourceChars, source.length());

            try (BufferedReader br = new BufferedReader(new FileReader(entry.toString()))) {
                for (String line; (line = br.readLine()) != null; ) {

                    boolean newItemGenerated = false;

                    final Matcher m = LOG_LINE_DATE_PATTERN.matcher(line);
                    while (m.find()) {
                        final String dateString = m.group(1);
                        final String message = m.group(2);
                        //System.out.println(dateString + source + message);
                        final Date date = LOG_DATE_FORMAT.parse(dateString);
                        lastItem = new Line(source, date, message);
                        lineObjects.add(lastItem);
                        newItemGenerated = true;
                        processedMessages++;
                        break;
                    }


                    if (!newItemGenerated) {
                        lastItem.message += "\n\t" + line;
                    }

                    processedLines++;
                }

            }
            processedFiles++;
            System.out.println("FILES:" + processedFiles + "|MESSAGES:" + processedMessages + "|LINES:" + processedLines);
        }


        System.out.println("SORTING...");
        Collections.sort(lineObjects);

        System.out.println("CREATING FILE...");

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS") ;



        final File file = new File(logfileTargetPath + dateFormat.format(date) + "_" + logfileTargetName);

        // if file doesnt exists, then create it
        if (file.exists()) {
            System.out.println("DELETING EXISTENT TARGET FILE...");
            file.delete();
        } else {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        final int finalMaxSourceChars = maxSourceChars;
        System.out.println("WRITING FILE...");
        lineObjects.stream().forEach(
                l -> {
                    try {
                        bw.write(LOG_DATE_FORMAT.format(l.date));
                        bw.write(" ");
                        bw.write(String.format("%" + finalMaxSourceChars + "s", l.source));
                        bw.write(" ");
                        bw.write(l.message);
                        bw.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        bw.close();

        System.out.println("SYSTEM READY_");

    }
}

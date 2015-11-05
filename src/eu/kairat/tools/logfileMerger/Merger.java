package eu.kairat.tools.logfileMerger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

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

    // STATIC FORMATS

    private static final Pattern LOG_LINE_DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (.*)$");
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");

    // PARAMETERS

    @Parameter(names = "-logfileSourcePath",
            description = "The path where the tool can find the logfiles to merge.")
    String logfileSourcePath;

    @Parameter(names = "-logfileSourceFileExtension",
            description = "The file extension of the logfiles to merge. (default = \"log\")")
    String logfileSourceFileExtension = "log";

    @Parameter(names = "-logfileTargetPath",
            description = "The path where the tool creates the logfile containing the merging result. (default = \".\\\")")
    String logfileTargetPath = ".\\";

    @Parameter(names = "-logfileTargetName",
            description = "The name of the logfile containing the merging result. (default = \"mergedLogfile.log\")")
    String logfileTargetName = "mergedLogfile.log";

    // PARAMETER CHECKS

    private enum Checktypes { PATTERN, PATH }

    /**
     * Checks all given checktypes and that the value is not null.
     * If the check fails an exception with the given parameter name is thrown.
     */
    private void checkString(final String        value,
                             final String        optionalPatternString,
                             final String        parameterName,
                             final Checktypes... checktypes) {

        final List checktypesList = Lists.newArrayList(checktypes);

        if(null == value) {
            throw new RuntimeException( "Parameter '" + parameterName + "' is null!");
        }

        if(checktypesList.contains(Checktypes.PATTERN)) {
            final Pattern p = Pattern.compile(optionalPatternString);
            final Matcher m = p.matcher(value);
            if(!m.matches()) {
                throw new RuntimeException("Parameter '" + parameterName + "' is invalid!");
            }
        }

        if(checktypesList.contains(Checktypes.PATH)) {
            if(!Files.isDirectory(Paths.get(value))) {
                throw new RuntimeException("Parameter '" + parameterName + "' is not a directory!");
            }
        }

    }

    /**
     * Checks all arguments.
     */
    private void checkArguments() {
        checkString(logfileSourceFileExtension, "^\\w+$",                    "logfileSourceFileExtension", Checktypes.PATTERN);
        checkString(logfileSourcePath,          null,                        "logfileSourcePath",          Checktypes.PATH   );
        checkString(logfileTargetName,          "^[\\w,\\s-]+\\.[A-Za-z]+$", "logfileTargetName",          Checktypes.PATTERN);
        checkString(logfileTargetPath,          null,                        "logfileTargetPath",          Checktypes.PATH   );
    }

    // MERGE FUNCTIONALITY

    void merge() throws Exception {

        // number of characters the longest filename of the source files has - without file ending
        int maxSourceChars = 0;
        int processedFiles = 0;
        int processedMessages = 0;
        int processedLines = 0;

        System.out.println("CHECKING ARGUMENTS...");
        checkArguments();

        // one line object per logfile entry
        final List<Line> lineObjects = new LinkedList<Line>();

        System.out.println("READING FILES...");

        final Path dir = Paths.get(logfileSourcePath);
        final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{" + logfileSourceFileExtension + "}");
        Line lastItem = null;

        // iterate over each source file
        for(final Path entry : stream) {
            final String source = entry.getFileName().toString().replaceFirst("[.][^.]+$", "");
            maxSourceChars = Math.max(maxSourceChars, source.length());

            // iterate over each line
            try (BufferedReader br = new BufferedReader(new FileReader(entry.toString()))) {
                for (String line; (line = br.readLine()) != null; ) {

                    // if the line starts with the defined date-time-start-pattern create a new line object
                    // and add it to the list
                    final Matcher m = LOG_LINE_DATE_PATTERN.matcher(line);
                    while (m.find()) {
                        // read the matcher groups and create the new line object
                        final String dateString = m.group(1);
                        final String message = m.group(2);
                        final Date date = LOG_DATE_FORMAT.parse(dateString);
                        lastItem = new Line(source, date, message);
                        lineObjects.add(lastItem);

                        // increment the number of processed messages - not the number of logfile lines
                        processedMessages++;

                        // increment the number of processed lines - not the number of logfile entries
                        processedLines++;

                        break;
                    }

                    // if the current line is an additional line of the current logfile entry
                    // add it to the message string and separate it by a new line followed by a tab
                    lastItem.message += "\n\t" + line;

                    // increment the number of processed lines - not the number of logfile entries
                    processedLines++;
                }

            }

            // increment the number of processed source files
            processedFiles++;

            // print status on console
            System.out.println("FILES:" + processedFiles + "|MESSAGES:" + processedMessages + "|LINES:" + processedLines);
        }

        System.out.println("SORTING...");
        Collections.sort(lineObjects); // uses the "compare"-method of the Line class

        System.out.println("CREATING FILE...");

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS") ;
        final File file = new File(logfileTargetPath + dateFormat.format(new Date()) + "_" + logfileTargetName);

        // if file does not exists, create it
        if (file.exists()) {
            System.out.println("DELETING EXISTENT TARGET FILE...");
            file.delete();
        } else {
            file.createNewFile();
        }

        final FileWriter fw = new FileWriter(file.getAbsoluteFile());
        final BufferedWriter bw = new BufferedWriter(fw);

        System.out.println("WRITING FILE...");

        // workaround - used variable must be final
        final int finalMaxSourceChars = maxSourceChars;

        // write objects to file
        try {
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

        } finally {
            // ensure that the stream is closed
            bw.close();
        }

        System.out.println("SYSTEM READY_");

    }
}

# logfileMerger
Merge multiple logfiles, written during the same time period, containing a specific formated date-time information in each log message line so that the output is timeline consistent.

## Dependencies
[com.beust.jcommander.JCommander](https://github.com/cbeust/jcommander)

## How to run
Execute the Main.class (contains a main method) and provide the following parameters:
 -  -logfileSourcePath (The path where the tool can find the logfiles to merge.)
 -  -logfileSourceFileExtension (The file extension of the logfiles to merge. (default = "log"))
 -  -logfileTargetPath (The path where the tool creates the logfile containing the merging result. (default = ".\"))
 -  -logfileTargetName (The name of the logfile containing the merging result. (default = "mergedLogfile.log"))

package eu.kairat.tools.logfileMerger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("\nLogfile Merger 0.1");
        System.out.println(  "==================\n");

        final Merger merger = new Merger();
        JCommander jc = null;

        try {
            jc = new JCommander(merger, args);
            jc.setProgramName("java -jar logfileMerger.jar");
        } catch(final ParameterException e0) {
            System.out.println(e0.getMessage());
            System.out.println("\nTo display all options use parameter \"-h\"!\n");
            return;
        }

        if(merger.help){
            jc.usage();
            return;
        }

        try {
            merger.merge();
        } catch(final Merger.InitCheckException e1) {
            System.out.println("\n");
            jc.usage();
        }
    }
    
}

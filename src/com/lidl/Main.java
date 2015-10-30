package com.lidl;

import com.beust.jcommander.JCommander;

public class Main {

    public static void main(String[] args) throws Exception {

        final Merger merger = new Merger();
        new JCommander(merger, args);

        merger.merge();
    }
    
}

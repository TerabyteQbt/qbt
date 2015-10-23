package org.cmyers.qbt.docs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import org.markdown4j.Markdown4jProcessor;

public class MarkdownGenerator {

    private static void error(String err) {
        System.err.println(err + "  Usage: mdg [output dir] [files...]");
        System.exit(1);
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            error("Invalid arguments.");
        }
        File outputDir = new File(args[0]);
        if(!outputDir.isDirectory()) {
            error(outputDir.toString() + " is not a directory");
        }
        System.out.println("using output dir: " + outputDir.getAbsolutePath());
        List<String> files = Arrays.asList(args).subList(1, args.length);

        for(String file : files) {
            Markdown4jProcessor p = new Markdown4jProcessor();
            try {
                File input = new File(file);
                System.out.println("Processing file: " + input.getAbsolutePath());
                String html = p.process(input);
                String outputPath = file.replaceAll(".md$", "").replaceAll(".markdown$", "") + ".html";
                File output = new File(outputDir.getAbsolutePath(), outputPath);
                System.out.println("Creating file: " + output.getAbsolutePath());
                output.getParentFile().mkdirs();
                output.createNewFile();
                PrintWriter out = new PrintWriter(output);
                out.append(html);
                out.close();
            }
            catch(IOException e) {
                System.out.println("Unable to process file '" + file + "':");
                e.printStackTrace();
            }
        }

    }

}

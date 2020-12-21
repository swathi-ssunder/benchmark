package com.kg.benchmark;

import net.antidot.semantic.rdf.rdb2rdf.main.Db2triples;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(name = "db2triples mapping", mixinStandardHelpOptions = true, version = "1.0",
        description = "db2triples mapping")
public class Db2Triples implements Runnable {
    @Option(names = {"-m", "--mapping"}, description = "Mapping file")
    protected String mappingFile;

    @Option(names = {"-o", "--output"}, description = "Output file")
    protected String outputFile;

    @Override
    public void run() {
        long startTime = System.nanoTime();

        String[] db2triplesArgs = {
                "-r", mappingFile,
                "-o", outputFile,
                "-m", "r2rml",
                "-t", "TURTLE",
                "-u", "",
                "-p", "",
                "-b", "",
                "-d", "com.mysql.cj.jdbc.Driver",
                "-l", "jdbc:mysql://[user]:[password]@[ip]:[port]/",
        };

        Db2triples.main(db2triplesArgs);

        long estimatedTime = System.nanoTime() - startTime;
        try {
            String line = "Time seconds,Number of Triples";
            String content = (TimeUnit.MILLISECONDS.convert(estimatedTime, TimeUnit.NANOSECONDS) / 1000.0) + "," + numberOfTriples(outputFile) + " triples\n";
            writeFile(outputFile + ".csv", Arrays.asList(line, content));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFile(String outputFile, List<String> contents) throws IOException {
        Files.write(Paths.get(outputFile), String.join("\n", contents).getBytes(), StandardOpenOption.CREATE);
    }

    private static int numberOfTriples(String file) throws IOException {
        if (Files.notExists(Paths.get(file))) {
            throw new IOException("Output file " + file + " does not exist");
        }
        RDFFormat format = Rio.getParserFormatForFileName(file).orElse(RDFFormat.TURTLE);
        RDFParser rdfParser = Rio.createParser(format);
        rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
        Model model = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(model));
        rdfParser.parse(new FileReader(file), file);
        return model.size();
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Db2Triples()).execute(args);
        System.exit(exitCode);
    }
}

package com.kg.benchmark;

import gr.seab.r2rml.beans.Database;
import gr.seab.r2rml.beans.Generator;
import gr.seab.r2rml.beans.Parser;
import gr.seab.r2rml.entities.MappingDocument;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Command(name = "r2rml-parser mapping", mixinStandardHelpOptions = true, version = "1.0",
        description = "r2rml-parser mapping")
public class R2RMLParser implements Runnable {
    @Option(names = {"-m", "--mapping"}, description = "Mapping file")
    protected String mappingFile;

    @Option(names = {"-o", "--output"}, description = "Output file")
    protected String outputFile;

    @Override
    public void run() {
        long startTime = System.nanoTime();
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("r2rml.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        properties.setProperty("mapping.file", mappingFile);
        properties.setProperty("jena.destinationFileName", outputFile);

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("app-context.xml");
        Database db = (Database) context.getBean("db");
        db.setProperties(properties);

        Parser parser = (Parser) context.getBean("parser");
        parser.setProperties(properties);

        MappingDocument mappingDocument = parser.parse();

        Generator generator = (Generator) context.getBean("generator");
        generator.setProperties(properties);
        generator.setResultModel(parser.getResultModel());

        generator.createTriples(mappingDocument);

        context.close();
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
        int exitCode = new CommandLine(new R2RMLParser()).execute(args);
        System.exit(exitCode);
    }
}

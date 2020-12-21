package com.kg.benchmark;

import com.taxonic.carml.engine.RmlMapper;
import com.taxonic.carml.logical_source_resolver.CsvResolver;
import com.taxonic.carml.logical_source_resolver.JsonPathResolver;
import com.taxonic.carml.logical_source_resolver.XPathResolver;
import com.taxonic.carml.model.TriplesMap;
import com.taxonic.carml.util.RmlMappingLoader;
import com.taxonic.carml.vocab.Rdf;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Command(name = "carml mapping", mixinStandardHelpOptions = true, version = "1.0",
        description = "carml mapping")
public class Carml implements Runnable {
    @Option(names = {"-m", "--mapping"}, description = "Mapping file")
    protected String mappingFile;

    @Option(names = {"-o", "--output"}, description = "Output file")
    protected String outputFile;

    @Option(names = {"-d", "--data-directory"}, description = "Data directory")
    protected String dataDirectory;

    @Override
    public void run() {
        long startTime = System.nanoTime();

        Set<TriplesMap> mapping = RmlMappingLoader
                .build()
                .load(RDFFormat.TURTLE, Paths.get(mappingFile));

        RmlMapper mapper = RmlMapper
                .newBuilder()
                // Add the resolvers to suit your need
                .setLogicalSourceResolver(Rdf.Ql.JsonPath, new JsonPathResolver())
                .setLogicalSourceResolver(Rdf.Ql.XPath, new XPathResolver())
                .setLogicalSourceResolver(Rdf.Ql.Csv, new CsvResolver())

                //-- optional: --
                // specify IRI unicode normalization form (default = NFC)
                // see http://www.unicode.org/unicode/reports/tr15/tr15-23.html
                .iriUnicodeNormalization(Normalizer.Form.NFKC)
                // set file directory for sources in mapping
                .fileResolver(Paths.get(dataDirectory))
                // set classpath basepath for sources in mapping
//                .classPathResolver("data")
                // specify casing of hex numbers in IRI percent encoding (default = true)
                // added for backwards compatibility with IRI encoding up until v0.2.3
                .iriUpperCasePercentEncoding(false)
                .build();

        Model result = mapper.map(mapping);
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            Rio.write(result, out, RDFFormat.TURTLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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
        int exitCode = new CommandLine(new Carml()).execute(args);
        System.exit(exitCode);
    }
}

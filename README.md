# benchmark

This is an (R2)RML processor evaluation benchmark.

```bash
# r2rml-parser
cd r2rml-parser
mvn clean package dependency:copy-dependencies
./r2rml-parser.sh -m <mapping-file> -o <output-file>

# morph-rdb
cd morph-rdb
mvn clean package dependency:copy-dependencies
./morph-rdb.sh -m <mapping-file> -o <output-file>

# db2triples
cd db2triples
mvn clean package dependency:copy-dependencies
./db2triples.sh -m <mapping-file> -o <output-file>

# rmlmapper
cd rmlmapper
mvn clean package dependency:copy-dependencies
./rmlmapper.sh -m <mapping-file> -o <output-file>

# carml
cd carml
mvn clean package dependency:copy-dependencies
./carml.sh -m <mapping-file> -o <output-file> -d <data-directory>
```
  
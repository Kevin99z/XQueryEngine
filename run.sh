#!/usr/bin/env bash
mkdir -p output
for i in {1..5} ; do
 file=src/test/queries/q${i}
 query=`cat ${file}`
 basex -o output/ref${i}.xml -q "${query}"
 java -jar ./target/XQueryEngine-1.0-SNAPSHOT-jar-with-dependencies.jar ${file} output/result${i}.xml
 output=`sed '1d;$d' output/result${i}.xml`
  ref=`cat output/ref${i}.xml`
  if [ "$output" == "$ref" ]; then
    echo "Test $i passed"
  else
    echo "Test $i failed"
  fi
done

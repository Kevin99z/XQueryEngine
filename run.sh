#!/usr/bin/env bash
mkdir -p output
for i in {1..5} ; do
 query=`cat src/test/queries/q${i}`
 basex -o output/ref${i}.rtf -q "${query}"
 java -jar ./target/XQueryEngine-1.0-SNAPSHOT-jar-with-dependencies.jar src/test/queries/q${i} output/result${i}.rtf
 output=`sed '1d;$d' output/result${i}.rtf`
  ref=`cat output/ref${i}.rtf`
  if [ "$output" == "$ref" ]; then
    echo "Test $i passed"
  else
    echo "Test $i failed"
  fi
done

#!/bin/sh
poole -b --base-url=http://aloiscochard.github.com/sindi/ src
rm *.html *.png *.css
cp src/output/* ./

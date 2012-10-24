#!/bin/sh

echo enter file name with extension
read NAME
echo enter word to replace
read FIND
echo enter word that replaces
read REPLACE                         
for file in $(grep -il "$FIND" $NAME)
do
sed -e "s/$FIND/$REPLACE/ig" $file > /tmp/tempfile.tmp
mv /tmp/tempfile.tmp $file
done

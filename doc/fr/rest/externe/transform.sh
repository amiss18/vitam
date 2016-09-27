#!/bin/sh
WORKDIR=`dirname $0`
OUTPUT_DIR="${WORKDIR}/target/html"
if [ -d ${OUTPUT_DIR} ]
then
	echo "Purge du répertoire cible"
	rm -rf ${OUTPUT_DIR}
fi

mkdir -p ${OUTPUT_DIR}

for I in `cat index`
do
	echo ${I}
	raml2html -i ${I}.raml -t template/template.nunjucks -o ${OUTPUT_DIR}/${I}.html
done

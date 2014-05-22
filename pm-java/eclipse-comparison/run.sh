#! /bin/sh
ECLIPSE_PATH=~/opt/eclipse
WORKSPACE=~/WORKSPACE.test

mkdir log
${ECLIPSE_PATH}/eclipse -nosplash -application Autofrob.autofrob -data ${WORKSPACE}/ autofrob.DoFrob -vmargs -Xmx256M
INDEX=$((`cat index` + 1))
mkdir oldlog
mv log oldlog/${INDEX}
echo $INDEX > index
echo "Saved as ${INDEX}"

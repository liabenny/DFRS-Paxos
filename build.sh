#!/usr/bin/env bash

cur_dir=$(pwd)
echo $cur_dir  
  
function compile(){
#    echo $cur_dir  
    dfrs=$cur_dir
    dfrs_src=$cur_dir/src
    dfrs_lib=$cur_dir/lib  
    dfrs_class=$cur_dir/bin
    
    rm -rf $dfrs_src/sources.list  
    find $dfrs_src -name "*.java" > $dfrs_src/sources.list  
    cat  $dfrs_src/sources.list  
    
    rm -rf $dfrs_class  
    mkdir $dfrs_class 
 
    javac -d $dfrs_class -encoding utf-8 -cp .:$dfrs_lib/fastjson-1.2.62.jar -g -sourcepath $dfrs_src @$dfrs_src/sources.list

    cp ./run.sh ./bin/
    cp ./lib/fastjson-1.2.62.jar ./bin/
    cp ./knownhosts.json ./bin/knownhosts.json
}

compile
exit(0)

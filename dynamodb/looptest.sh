#!/bin/bash

for i in "$@"
do 
    echo "begin - " $i
    
    aws dynamodb put-item \
        --table-name MusicCollection \
        --item '{
            "Artist": {"S": "'$i' - Artist Test"},
            "SongTitle": {"S": "'$i' - SongTitle Test"} ,
            "AlbumTitle": {"S": "'$i' - AlbumTitle Test with different name"}
        }' \
        --return-consumed-capacity TOTAL;
    
    echo "done - " $i
done

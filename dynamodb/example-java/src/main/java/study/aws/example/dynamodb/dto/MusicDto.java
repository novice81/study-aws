package study.aws.example.dynamodb.dto;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class MusicDto {
    public MusicDto(String artist, String songTitle) {
        this.artist = artist;
        this.songTitle = songTitle;
    }

    public static MusicDto fromAttributeMap(Map<String, AttributeValue> record) {
        return new MusicDto(record.get("Artist").s(), record.get("SongTitle").s());
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public String artist;

    public String songTitle;

    public Map<String, AttributeValue> toAttributeValues() {
        return ImmutableMap.of(
                "Artist", AttributeValue.builder().s(getArtist()).build(),
                "SongTitle", AttributeValue.builder().s(getSongTitle()).build()
        );
    }
}

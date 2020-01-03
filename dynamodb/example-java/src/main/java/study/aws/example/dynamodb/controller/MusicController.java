package study.aws.example.dynamodb.controller;

import com.google.common.collect.ImmutableList;
import org.springframework.web.bind.annotation.*;
import study.aws.example.dynamodb.dto.MusicDto;
import study.aws.example.dynamodb.service.MusicService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/music")
public class MusicController {

    @Resource
    private MusicService musicService;

    @GetMapping(value = "/collections", produces = "application/json")
    public ImmutableList<MusicDto> collections() {
        return musicService.listCollections();
    }

    @PostMapping(value = "/collections", produces = "application/json")
    public MusicDto postCollection(@RequestBody MusicDto musicDto) {
        return musicService.putItem(musicDto);
    }
}

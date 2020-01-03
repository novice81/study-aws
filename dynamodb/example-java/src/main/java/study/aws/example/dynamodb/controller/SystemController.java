package study.aws.example.dynamodb.controller;

import com.google.common.collect.ImmutableList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import study.aws.example.dynamodb.service.SystemService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/system")
public class SystemController {

    @Resource
    SystemService systemService;

    @GetMapping(value = "/ping", produces = "application/json")
    public String ping() {
        return "ok";
    }

    @GetMapping(value = "/tables", produces = "application/json")
    public ImmutableList<String> tables() {
        return systemService.listTables();
    }
}

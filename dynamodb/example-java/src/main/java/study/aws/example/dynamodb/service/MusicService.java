package study.aws.example.dynamodb.service;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import study.aws.example.dynamodb.dto.MusicDto;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class MusicService {

    private static final String TABLE_NAME_MUSIC_COLLECTION = "MusicCollection";


    @Resource
    private DynamoDbClient dynamoDbClient;

    private static Logger logger = LoggerFactory.getLogger(MusicService.class);

    private MusicService() {
    }

    public ImmutableList<String> listTables() {

        Optional<String> lastEvaluatedTableName = Optional.empty();
        ImmutableList.Builder<String> tableNamesBuilder = ImmutableList.builder();
        ListTablesResponse response = null;

        while (response == null || lastEvaluatedTableName.isPresent()) {
            ListTablesRequest.Builder listTableRequestBuilder = ListTablesRequest.builder();
            lastEvaluatedTableName.ifPresent(listTableRequestBuilder::exclusiveStartTableName);

            response = dynamoDbClient.listTables(listTableRequestBuilder.build());
            tableNamesBuilder.addAll(response.tableNames());

            lastEvaluatedTableName = response.lastEvaluatedTableName() == null ?
                    Optional.empty() : Optional.of(response.lastEvaluatedTableName());
        }

        return tableNamesBuilder.build();
    }

    public ImmutableList<MusicDto> listCollections() {
        logger.info("listCollections()");

        ScanResponse scanResponse = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(TABLE_NAME_MUSIC_COLLECTION)
                .build());

        return scanResponse.items().stream()
                .map(MusicDto::fromAttributeMap)
                .collect(ImmutableList.toImmutableList());
    }

    public MusicDto putItem(MusicDto musicDto) {
        logger.info("putItem()");

        PutItemResponse putItemResponse = dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME_MUSIC_COLLECTION)
                .item(musicDto.toAttributeValues())
                .build());

        return putItemResponse.sdkHttpResponse().isSuccessful() ? musicDto : null;
    }
}

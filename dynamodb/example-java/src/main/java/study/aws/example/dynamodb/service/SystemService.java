package study.aws.example.dynamodb.service;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import javax.annotation.Resource;
import java.util.Optional;

@Service
public class SystemService {

    @Resource
    private DynamoDbClient dynamoDbClient;

    public static SystemService create() {
        return new SystemService();
    }

    private SystemService() {
    }

    public ImmutableList<String> listTables() {
        Optional<String> lastEvaluatedTableName = Optional.empty();
        ImmutableList.Builder<String> tableNamesBuilder = ImmutableList.builder();

        ListTablesResponse response;

        do {
            ListTablesRequest.Builder listTableRequestBuilder = ListTablesRequest.builder();
            lastEvaluatedTableName.ifPresent(listTableRequestBuilder::exclusiveStartTableName);

            response = dynamoDbClient.listTables(listTableRequestBuilder.build());
            tableNamesBuilder.addAll(response.tableNames());

            lastEvaluatedTableName = response.lastEvaluatedTableName() == null ?
                    Optional.empty() : Optional.of(response.lastEvaluatedTableName());
        } while (lastEvaluatedTableName.isPresent());

        return tableNamesBuilder.build();
    }
}

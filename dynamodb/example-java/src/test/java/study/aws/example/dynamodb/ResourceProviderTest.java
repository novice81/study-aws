package study.aws.example.dynamodb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import javax.annotation.Resource;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ResourceProviderTest {

    @Resource
    private DynamoDbClient dynamoDbClient;

    @Resource
    private DynamoDbClient dynamoDbClient1;

    @Resource
    private DynamoDbClient dynamoDbClient2;

    @Resource
    private Environment environment;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Test
    public void testDynamoDbClientCreation() {
        assertThat(dynamoDbClient).isNotNull();
    }

    @Test
    public void testDynamoDbClientInstancesAreAlwaysSame() {
        assertThat(dynamoDbClient).isNotNull();
        assertThat(dynamoDbClient).isEqualTo(dynamoDbClient1);
        assertThat(dynamoDbClient).isEqualTo(dynamoDbClient2);
    }

    @Test
    public void testDynamoDbGetTables() {
        ListTablesResponse listTableResponse = dynamoDbClient.listTables();
        assertThat(listTableResponse.tableNames()).isNotEmpty();
    }

    @Test
    public void testEnvironment() {
        assertThat(environment.getProperty("app.message")).isNotEqualTo("COMMON");
        assertThat(environment.getProperty("app.message")).isEqualTo(activeProfile.toUpperCase());
    }
}

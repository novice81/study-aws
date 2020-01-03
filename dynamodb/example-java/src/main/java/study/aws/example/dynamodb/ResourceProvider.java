package study.aws.example.dynamodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class ResourceProvider {

    @Autowired
    private Environment environment;

    public Environment environment() {
        return this.environment;
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return ProfileCredentialsProvider.create("dynamodb");
    }

    @Bean
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider awsCredentialsProvider) {
        return DynamoDbClient.builder().region(Region.EU_NORTH_1).credentialsProvider(awsCredentialsProvider).build();
    }
}

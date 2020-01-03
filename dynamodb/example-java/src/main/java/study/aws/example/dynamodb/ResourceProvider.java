package study.aws.example.dynamodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class ResourceProvider {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    private Environment environment;

    public Environment environment() {
        return this.environment;
    }

    public String getActiveProfile() {
        return this.activeProfile;
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (activeProfile.equalsIgnoreCase("stage")) {
            return InstanceProfileCredentialsProvider.create();
        }

        return ProfileCredentialsProvider.create("dynamodb");
    }

    @Bean
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider awsCredentialsProvider) {
        return DynamoDbClient.builder().region(Region.EU_NORTH_1).credentialsProvider(awsCredentialsProvider).build();
    }
}

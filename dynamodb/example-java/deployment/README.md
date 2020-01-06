# Deploy the dynamo-example app

## Create EC2 instance

[AWS Document Link](https://docs.aws.amazon.com/cli/latest/userguide/cli-services-ec2.html)

### Setup varaibles

```zsh
% NOVICE_KEY_NAME={key_name}
% NOVICE_KEY_FILE_NAME={key_file_name}

% NOVICE_SG_GROUP_NAME={security_group_name}
% NOVICE_MY_IP=$(curl https://checkip.amazonaws.com)
% NOVICE_SERVICE_PORT=13580
```

### Create a Key Pair

```zsh
% aws ec2 create-key-pair \
    --profile ec2-user \
    --key-name $NOVICE_KEY_NAME \
    --query 'KeyMaterial' \
    --output text > $NOVICE_KEY_FILE_NAME
```

```zsh
% chmod 400 $NOVICE_KEY_FILE_NAME
```

### Create Security Group

#### Create Security Group with classic way

```zsh
% aws ec2 create-security-group \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME \
    --description "DevJog security group"
{
    "GroupId": ...
}
```

#### Describe to check created Security Group

```zsh
% aws ec2 describe-security-groups
{
    "SecurityGroups": [
        {
            ...
        }
    ]
}
```

#### Add a rule for SSH

##### Check IP to allow

```zsh
% echo $NOVICE_MY_IP
xxx.xxx.xxx.xxx
```

##### Add IP to security group ingress

```zsh
% aws ec2 authorize-security-group-ingress \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME \
    --protocol tcp \
    --port 22 \
    --cidr $NOVICE_MY_IP/32
```

#### Open the service port

##### Allow

```zsh
% aws ec2 authorize-security-group-ingress \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME \
    --protocol tcp \
    --port $NOVICE_SERVICE_PORT \
    --cidr 0.0.0.0/0
```

##### Describe to check allowed authorization

```zsh
% aws ec2 describe-security-groups \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME
{
    "SecurityGroups": [
        {
            ...
        }
    ]
}
```

### Finding a Quick Start AMI

[AWS Document Link](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html#finding-quick-start-ami)

```zsh
% NOVICE_AMI_ID=$(aws ec2 describe-images --profile ec2-user \
    --owners self amazon \
    --filters 'Name=name,Values=amzn2-ami-hvm-2.0.????????.?-x86_64-gp2' 'Name=state,Values=available' \
    --query 'reverse(sort_by(Images, &CreationDate))[:1].ImageId' | jq -r '.[0]')
```

### And.. Create EC2 instance

#### Fire the command to create the EC2 instance

```zsh
% aws ec2 run-instances --profile ec2-user \
    --image-id $NOVICE_AMI_ID \
    --count 1 \
    --instance-type t3.nano \
    --key-name $NOVICE_KEY_NAME \
    --security-groups $NOVICE_SG_GROUP_NAME \
    --iam-instance-profile Name=ec2-dynamo-user
{
    "Groups": [],
    "Instances": [
        {
            ...
        }
    ],
    ...
}
```

#### Check the state with `jq`

```zsh
% aws ec2 describe-instances --profile ec2-user | jq '.Reservations[0].Instances[0].State.Name'
```

#### Get PublicDnsName by `jq`

```zsh
% NOVICE_PUBLIC_DNS_NAME=$(aws ec2 describe-instances --profile ec2-user | jq -r '.Reservations[0].Instances[0].PublicDnsName')
```

#### Get the InstanceID by `jq`

```zsh
% NOVICE_INSTANCE_ID=$(aws ec2 describe-instances --profile ec2-user | jq -r '.Reservations[0].Instances[0].InstanceId')
```

#### Connect to the EC2 with SSH

```zsh
% ssh -i $NOVICE_KEY_FILE_NAME ec2-user@$NOVICE_PUBLIC_DNS_NAME
The authenticity of host 'xxx (xxx.xxx.xxx.xxx)' can't be established.
ECDSA key fingerprint is SHA256:xxxxxxxxxxx.
Are you sure you want to continue connecting (yes/no)? yes

       __|  __|_  )
       _|  (     /   Amazon Linux 2 AMI
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
8 package(s) needed for security, out of 17 available
Run "sudo yum update" to apply all updates.
[ec2-user@ip-xxx-xxx-xxx-xxx ~]$

# leave the instance to get ready
[ec2-user@ip-xxx-xxx-xxx-xxx ~]$ exit
```

### Deploy the jar file

#### Build jar file

```zsh
# in the example-java directory
% gradle build
```

#### Transfer the file with SCP

```zsh
# in the deployment directory
% scp -i $NOVICE_KEY_FILE_NAME ../build/libs/dynamodb-sample-0.1.0.jar ec2-user@$NOVICE_PUBLIC_DNS_NAME:dynamodb-sample-0.1.0.jar
dynamodb-sample-0.1.0.jar         100%   41MB   1.9MB/s   00:21
```

#### Connect again and get ready to run it

```zsh
# in the deployment directory
% ssh -i $NOVICE_KEY_FILE_NAME ec2-user@$NOVICE_PUBLIC_DNS_NAME
```

```bash
# after you connect,
$ ls -al
...
-rw-r--r-- 1 ec2-user ec2-user 42654763 Jan  4 21:57 dynamodb-sample-0.1.0.jar
...
$
```

```bash
$ sudo yum -y update
Loaded plugins: extras_suggestions, langpacks, priorities, update-motd
Resolving Dependencies
--> Running transaction check
---> Package amazon-ssm-agent.x86_64 0:2.3.662.0-1.amzn2 will be updated
---> Package amazon-ssm-agent.x86_64 0:2.3.714.0-1.amzn2 will be an update
...

$ sudo yum install java
...
---> Package java-11-amazon-corretto.x86_64 1:11.0.5+10-1.amzn2 will be installed
...
Is this ok [y/d/N]: y
...
Installed:
  java-11-amazon-corretto.x86_64 1:11.0.5+10-1.amzn2  
...
```

#### Execute the jar file

```bash
$ java -jar dynamodb-sample-0.1.0.jar --spring.profiles.active=stage --server.port=13580
...
2020-01-06 19:37:00.903  INFO 15728 --- [           main] study.aws.example.dynamodb.Application   : The following profiles are active: stage
2020-01-06 19:37:02.821  INFO 15728 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 13580 (http)
...
```

#### Check the API call

```zsh
% curl $NOVICE_PUBLIC_DNS_NAME:$NOVICE_SERVICE_OPEN_PORT/system/tables
["MusicCollection"]

% curl $NOVICE_PUBLIC_DNS_NAME:$NOVICE_SERVICE_OPEN_PORT/music/collections
[{"artist":"John Mayer","songTitle":"Carry Me Away"}]
```

#### Terminate EC2 Instance

```zsh
% aws ec2 terminate-instances --profile ec2-user \
    --instance-id $NOVICE_INSTANCE_ID
```

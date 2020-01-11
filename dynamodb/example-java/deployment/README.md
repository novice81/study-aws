# Deploy the dynamo-example app

이 문서에서는 `jar` 파일을 AWS CLI를 이용하여 EC2 Instance에 배포하는 방법을 알아본다.

최근 CloudFormation / Terraform 등 배포를 위한 간편한 도구들이 많이 있지만,\
AWS가 제공하는 시스템들의 속성을 하나씩 기록해두기 위해, 번거롭지만 CLI를 통해 배포하는 방향을 선택했다.

[AWS Document Link](https://docs.aws.amazon.com/cli/latest/userguide/cli-services-ec2.html)

Amazon EC2는 Amazon Elastic Compute Cloud의 약자인데, 소개글에 보면 scalable computing capacity라는 말이 나온다.\
확장 가능한 컴퓨팅 자원이라고 할 수 있는데, 하나의 EC2 Instance를 만든다는건 하나의 가상 서버를 생성함을 의미한다.

여기서부터는 서버 응용프로그램과 이를 구동해주는 장치를 구분하기 위해 서버 응용프로그램은 Application, 장치는 Instance라고 구분하여 기록한다.\
이 문서는 EC2를 CLI로 만드는 방법을 소개한 문서를 기반으로 작성했으며, `zsh` / `bash` 기반에서 테스트 되었다.

이 문서에서는 `%`는 zsh, `$`는 bash 기반의 환경임을 나타낸다.\
`ec2-user`라는 profile을 이용했는데, EC2 관련 권한을 할당한 IAM User를 `aws configure --profile ec2-user`로 설정했다.

## Create EC2 Instance

EC2 인스턴스를 생성하는 방법을 기록해본다.

### Setup varaibles

인스턴스 생성에 앞서 편의를 위해 몇가지 변수를 설정한다.

```zsh
# 생성할 KeyPair의 이름
% NOVICE_KEY_PAIR_NAME={key_pair_name}
# 저장할 KeyPair 파일의 이름
% NOVICE_KEY_FILE_NAME={key_file_name}

# EC2 Instance에 적용할 보안 그룹(Security Group) 이름
% NOVICE_SG_GROUP_NAME={security_group_name}

# EC2 Instance에 연결할 환경의 IP
% NOVICE_MY_IP=$(curl https://checkip.amazonaws.com)

# 우리가 배포할 Application이 사용하는 Port
% NOVICE_SERVICE_PORT=13580
```

### Create a Key Pair

Key Pair를 생성한다. Key Pair는 EC2 Instance를 생성하기 위해 반드시 필요하다.\
Instance 생성 후 EC2에 접속할 수 있는 인증 정보이다.

```zsh
% aws ec2 create-key-pair \
    --profile ec2-user \
    --key-name $NOVICE_KEY_PAIR_NAME \
    --query 'KeyMaterial' \
    --output text > $NOVICE_KEY_FILE_NAME
```

생성한 Key Pair파일을 보안을 위해 내 계정의 읽기 전용 권한으로 변경한다.

```zsh
% chmod 400 $NOVICE_KEY_FILE_NAME
```

### Create Security Group

Security Group을 생성한다. EC2 Instance는 Security Group을 통해 네트워크 접근 정책을 정의한다.\
클라우드 환경 이전의 방화벽(Firewall)에 해당하는 역할로, 트래픽의 들어오고 나가는 정책을 관리할 수 있다.

#### Create Security Group in Default VPC

아래 예제는 Default VPC에 Security Group을 생성하는 방법을 보여준다.

```zsh
% aws ec2 create-security-group \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME \
    --description "DevJog security group"
{
    "GroupId": ...
}
```

VPC를 지정하고 싶은 경우 아래 옵션을 추가할 수 있다.

```zsh
--vpc-id {vpc_id}
```

해당하는 VPC ID는 `describe-vpcs`명령을 이용하여 확인할 수 있다.

```zsh
% aws ec2 describe-vpcs --profile ec2-user
```

#### Describe to check created Security Group

생성한 Security Group은 이렇게 확인할 수 있다.

```zsh
% aws ec2 describe-security-groups --profile ec2-user
{
    "SecurityGroups": [
        {
            ...
        }
    ]
}
```

#### Add a rule for SSH

생성한 Security Group에 규칙을 추가한다. 이를 통해 Instance에 `ssh`로 접속할 수 있다.

##### Check IP to allow

어떤 IP를 허용하게 되는지 확인한다.

```zsh
% echo $NOVICE_MY_IP
xxx.xxx.xxx.xxx
```

22번 port를 통한 접속은 나의 IP만 가능하도록 규칙을 추가한다.

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

서버 Application이 이용할 포트에 접속 가능하도록 규칙을 추가한다.\
해당 포트는 Application용 포트이므로 모든 IP에서 접근 가능하도록 열어보자.

```zsh
% aws ec2 authorize-security-group-ingress \
    --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME \
    --protocol tcp \
    --port $NOVICE_SERVICE_PORT \
    --cidr 0.0.0.0/0
```

적용 여부를 확인한다.

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

EC2는 동작하는 운영체제를 필요로 한다. 우리는 Linux 환경을 설치하는데, 그 중에도 AMI(Amazone Machine Image)를 이용한다.\
빠른 시작을 위해 아마존에서 추천하는 이미지를 이용하기로 하고, AWS Document의 링크에 나온 방법을 그대로 이용한다.

[AWS Document Link](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/finding-an-ami.html#finding-quick-start-ami)

```zsh
% NOVICE_AMI_ID=$(aws ec2 describe-images --profile ec2-user \
    --owners self amazon \
    --filters 'Name=name,Values=amzn2-ami-hvm-2.0.????????.?-x86_64-gp2' 'Name=state,Values=available' \
    --query 'reverse(sort_by(Images, &CreationDate))[:1].ImageId' | jq -r '.[0]')
```

### And.. Create EC2 Instance

드디어 EC2 Instance 생성 명령을 수행한다.\
수행 후 몇가지 값을 확인하기 위해 `jq`를 이용할 예정이다.

#### Fire the command to create the EC2 Instance

생성 명령에 전달되는 속성들이 필요한데, 대부분은 위 과정을 통해 생성되었고,\
여기서 새로 확인해야 하는 내용은 `--iam-instance-profile`에 전달하는 `Name=xxx` 부분이다.

EC2 Instance에 별도의 인증정보를 설정하지 않고 AWS의 기능들을 이용하려면,\
이와같이 Instance Profile이 필요하다. [Using Instance Profiles](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html) 문서를 통해 IAM 권한을 설정하자.

이 예제에서는 `ec2-dynamo-user` 라는 미리 설정된 Instance profile을 이용한다.\
이 Role은 Dynamo DB Access 권한이 설정된 Role이다.

```zsh
% aws ec2 run-instances --profile ec2-user \
    --image-id $NOVICE_AMI_ID \
    --count 1 \
    --instance-type t3.nano \
    --key-name $NOVICE_KEY_PAIR_NAME \
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

생성 후 `jq`를 통해 Instance의 실행상태를 확인한다.

```zsh
% aws ec2 describe-instances --profile ec2-user | jq '.Reservations[0].Instances[0].State.Name'
```

서버를 배포한 후 사용할 Public DNS를 확인한다.

```zsh
% NOVICE_PUBLIC_DNS_NAME=$(aws ec2 describe-instances --profile ec2-user | jq -r '.Reservations[0].Instances[0].PublicDnsName')
```

모든 테스트가 마무리된 후 EC2 Instance를 종료(Terminate)하기 위해 Instance ID를 변수에 담아놓자.

```zsh
% NOVICE_INSTANCE_ID=$(aws ec2 describe-instances --profile ec2-user | jq -r '.Reservations[0].Instances[0].InstanceId')
```

#### Connect to the EC2 with SSH

EC2 Instance에 `ssh` 와 KeyPair를 이용해 접속한다. Instance를 만들면 `ec2-user`라는 계정이 기본으로 생성된다.

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

접속을 확인했으니, 이제 서버 Application을 배포해보자.

## Deploy the `jar` file

### Build `jar` file

이 프로젝트를 빌드하면 `jar`파일이 나온다.

```zsh
# in the example-java directory
% gradle clean build
```

### Transfer the file with SCP

빌드된 `jar`파일을 `scp`를 이용하여 EC2 Instance에 배포해보자.

```zsh
# in the deployment directory
% scp -i $NOVICE_KEY_FILE_NAME ../build/libs/dynamodb-sample-0.1.0.jar ec2-user@$NOVICE_PUBLIC_DNS_NAME:dynamodb-sample-0.1.0.jar
dynamodb-sample-0.1.0.jar         100%   41MB   1.9MB/s   00:21
```

## Connect again and get ready to run it

EC2 Instance가 서버 Application을 실행할 준비를 해보자.

```zsh
# in the deployment directory
% ssh -i $NOVICE_KEY_FILE_NAME ec2-user@$NOVICE_PUBLIC_DNS_NAME
```

먼저, 접속 후 `jar`파일이 정확히 복사됐는지 확인한다.

```bash
# after you connect,
$ ls -al
...
-rw-r--r-- 1 ec2-user ec2-user 42654763 Jan  4 21:57 dynamodb-sample-0.1.0.jar
...
$
```

먼저 Linux를 업데이트 해준다.

```bash
$ sudo yum -y update
Loaded plugins: extras_suggestions, langpacks, priorities, update-motd
Resolving Dependencies
--> Running transaction check
---> Package amazon-ssm-agent.x86_64 0:2.3.662.0-1.amzn2 will be updated
---> Package amazon-ssm-agent.x86_64 0:2.3.714.0-1.amzn2 will be an update
...
```

Java Runtime을 설치하는데, Amazon Correto를 이용해보자.

```bash
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

## Execute the Server Application

우리가 배포한 서버를 실행한다.\
Instance Profile을 사용하기 위해 `stage` profile을 이용하고,\
우리가 Security Group에 등록한 13580포트를 열어 Application을 실행시킨다.

```bash
$ java -jar dynamodb-sample-0.1.0.jar --spring.profiles.active=stage --server.port=13580
...
2020-01-06 19:37:00.903  INFO 15728 --- [           main] study.aws.example.dynamodb.Application   : The following profiles are active: stage
2020-01-06 19:37:02.821  INFO 15728 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 13580 (http)
...
```

실행된 서버와의 연결을 끊어도 서버 프로세스가 종료되지 않게 하려면, 서버를 `screen` 혹은 `nohup` 명령을 통해 실행시켜야 한다.

[screen, nohup 관련글](https://superuser.com/questions/632205/continue-ssh-background-task-jobs-when-closing-ssh)

## Check the API call

`curl`로 API를 호출하여, 서버가 정상적으로 동작하는지 확인한다.

```zsh
% curl $NOVICE_PUBLIC_DNS_NAME:$NOVICE_SERVICE_OPEN_PORT/system/tables
["MusicCollection"]

% curl $NOVICE_PUBLIC_DNS_NAME:$NOVICE_SERVICE_OPEN_PORT/music/collections
[{"artist":"John Mayer","songTitle":"Carry Me Away"}]
```

## Terminate EC2 Instance

테스트를 마친 후 EC2 Instance를 완전히 종료한다.

EC2 Instance를 Terminate 한다.

```zsh
% aws ec2 terminate-instances --profile ec2-user \
    --instance-id $NOVICE_INSTANCE_ID
```

EC2 Instance에 할당했던 Security Group도 삭제한다.

```zsh
% aws ec2 delete-security-group --profile ec2-user \
    --group-name $NOVICE_SG_GROUP_NAME
```

KeyPair도 삭제한다.

```zsh
% aws ec2 delete-key-pairs --profile ec2-user \
    --key-name $NOVICE_KEY_PAIR_NAME
```

## 정리

EC2 Instance를 AWS CLI를 통해 생성하고, 서버 Application을 배포하는 방법을 기록해봤다.\
다른 도구들로 간편히 배포하는 방법도 유용하겠지만, 지금은 EC2 Instance와 관련된 다른 내용들이 어떤 것들이 있는지 알고 싶었다.

추후 Terraform 등을 이용한 배포나, Serverless 형태의 배포도 이어서 다뤄볼 예정이다.

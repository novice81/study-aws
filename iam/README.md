# IAM 사용하기

AWS는 IAM (Identity and Access Management)라는, AWS 리소스에 대한 접근을 제어할 수 있는 웹 서비스를 제공한다.\
이 문서는 추상적인 권한 관리는 생략하고, [이 문서를 통해](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html#id_users_create_cliwpsapi) IAM 사용자를 추가하는 방법을 이야기한다.

- [IAM 사용하기](#iam-%ec%82%ac%ec%9a%a9%ed%95%98%ea%b8%b0)
  - [IAM Group 생성](#iam-group-%ec%83%9d%ec%84%b1)
    - [Group 생성](#group-%ec%83%9d%ec%84%b1)
    - [Group 삭제](#group-%ec%82%ad%ec%a0%9c)
    - [Group에 Policy 연결](#group%ec%97%90-policy-%ec%97%b0%ea%b2%b0)
  - [IAM User 생성](#iam-user-%ec%83%9d%ec%84%b1)
    - [User 생성](#user-%ec%83%9d%ec%84%b1)
    - [Group에 User 추가](#group%ec%97%90-user-%ec%b6%94%ea%b0%80)
    - [Group에서 User 삭제](#group%ec%97%90%ec%84%9c-user-%ec%82%ad%ec%a0%9c)
    - [User 삭제](#user-%ec%82%ad%ec%a0%9c)
  - [IAM User의 Access Key ID &amp; Secret Access Key 발급](#iam-user%ec%9d%98-access-key-id-amp-secret-access-key-%eb%b0%9c%ea%b8%89)
  - [IAM Role 생성](#iam-role-%ec%83%9d%ec%84%b1)
  - [Instance Profile 생성](#instance-profile-%ec%83%9d%ec%84%b1)
  - [Instance Profile 삭제](#instance-profile-%ec%82%ad%ec%a0%9c)

## IAM Group 생성

IAM의 User에 직접 Policy를 부여할 수도 있다.\
그러나 Group에 Policy를 부여하면 User가 추가될 때마다 Policy를 부여하지 않아도 된다.\
User를 Group에 추가해 주기만 하면 된다.

### Group 생성

Group을 만들어보자. 간단히 이름만 정해주면 된다.\
우리는 서버를 EC2 Instance에 배포할 예정이어서, 기억하기 편하도록 `ec2-group`이라고 정한다.

```zsh
% aws iam create-group --group-name ec2-group --profile iam-user
{
    "Group": {
        "Path": "/",
        "GroupName": "ec2-group",
        "GroupId": "XXXXXXXXXXXXXXXXXXXXX",
        "Arn": "arn:aws:iam::000000000000:group/ec2-group",
        "CreateDate": "2020-02-12T21:43:41Z"
    }
}
```

### Group 삭제

나중에 삭제할 일이 생기면 여기를 참고하자.

```zsh
% aws iam delete-group --group-name ec2-group --profile iam-user
```

### Group에 Policy 연결

Group은 그 자체만으로는 아무 권한이 없다.\
Group에 속한 User들이 사용할 수 있는 정책을 연결해 주어야 한다.

먼저 현재 Policy를 확인해본다.

```zsh
% aws iam list-attached-group-policies --group-name ec2-group --profile iam-user
{
    "AttachedPolicies": []
}
```

Group에 Policy를 부여할 때에는 해당 Policy에 대한 ARN(Amazon Resource Name)을 알아야 한다.\
이 문서에서는 [Amazon Managed Policy](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_managed-vs-inline.html#aws-managed-policies)를 이용한다.

먼저 Group에 부여할 Policy들의 ARN을 변수에 담는다.

우리는 편의상 하나의 그룹에 `AmazonEC2FullAccess`, `AmazonRoute53FullAccess`, `AmazonDynamoDBFullAccess` Policy들을 부여한다.

```zsh
% NOVICE_EC2_FULL_ACCESS_POLICY_ARN=$(aws iam list-policies --query 'Policies[?PolicyName==`AmazonEC2FullAccess`].Arn' --output text --profile iam-user)
% NOVICE_ROUTE53_FULL_ACCESS_POLICY_ARN=$(aws iam list-policies --query 'Policies[?PolicyName==`AmazonRoute53FullAccess`].Arn' --output text --profile iam-user)
% NOVICE_DYNAMODB_FULL_ACCESS_POLICY_ARN=$(aws iam list-policies --query 'Policies[?PolicyName==`AmazonDynamoDBFullAccess`].Arn' --output text --profile iam-user)
```

변수에 담은 ARN들을 이용하여 Policy들을 부여한다.

```zsh
% aws iam attach-group-policy --group-name ec2-group --policy-arn $NOVICE_EC2_FULL_ACCESS_POLICY_ARN --profile iam-user
% aws iam attach-group-policy --group-name ec2-group --policy-arn $NOVICE_ROUTE53_FULL_ACCESS_POLICY_ARN --profile iam-user
% aws iam attach-group-policy --group-name ec2-group --policy-arn $NOVICE_DYNAMODB_FULL_ACCESS_POLICY_ARN --profile iam-user
```

아래와 같이 부여된 Policy를 확인할 수 있다.

```zsh
% aws iam list-attached-group-policies --group-name ec2-group --profile iam-user
{
    "AttachedPolicies": [
        {
            "PolicyName": "AmazonEC2FullAccess",
            "PolicyArn": "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
        },
        {
            "PolicyName": "AmazonDynamoDBFullAccess",
            "PolicyArn": "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
        },
        {
            "PolicyName": "AmazonRoute53FullAccess",
            "PolicyArn": "arn:aws:iam::aws:policy/AmazonRoute53FullAccess"
        }
    ]
}
```

부여했던 Policy를 회수하는 경우엔 이렇게 하면 된다.

```zsh
aws iam detach-group-policy --group-name ec2-group --policy-arn $NOVICE_EC2_FULL_ACCESS_POLICY_ARN --profile iam-user
aws iam detach-group-policy --group-name ec2-group --policy-arn $NOVICE_ROUTE53_FULL_ACCESS_POLICY_ARN --profile iam-user
aws iam detach-group-policy --group-name ec2-group --policy-arn $NOVICE_DYNAMODB_FULL_ACCESS_POLICY_ARN --profile iam-user
```

## IAM User 생성

Group에 참석시킬 User를 생성하는 단계이다.

### User 생성

이 역시 이름만 부여하면 쉽게 생성이 가능하지만, Group과 마찬가지로 User를 생성하는 것만으로는 아무것도 할 수 없다.\
기억하기 편하도록 `ec2-user`라는 이름으로 생성한다.

```zsh
% aws iam create-user --user-name ec2-user --profile iam-user
{
    "User": {
        "Path": "/",
        "UserName": "ec2-user",
        "UserId": "XXXXXXXXXXXXXXXXXXXXX",
        "Arn": "arn:aws:iam::000000000000:user/ec2-user",
        "CreateDate": "2020-02-12T22:25:46Z"
    }
}
```

### Group에 User 추가

위에서 언급한대로 User에게 Policy를 직접 부여하지 않고, 원하는 Policy를 가진 Group에 참가시킨다.\
여기서는 `ec2-user`라는 User를 `ec2-group` 이라는 Group에 참가시킨다.

```zsh
% aws iam add-user-to-group --user-name ec2-user --group-name ec2-group --profile iam-user
```

Group에 소속된 User의 정보를 확인한다.

```zsh
% aws iam get-group --group-name ec2-group --profile iam-user
{
    "Users": [
        {
            "Path": "/",
            "UserName": "ec2-user",
            "UserId": "XXXXXXXXXXXXXXXXXXXXX",
            "Arn": "arn:aws:iam::000000000000:user/ec2-user",
            "CreateDate": "2020-02-12T22:25:46Z"
        }
    ],
    "Group": {
        "Path": "/",
        "GroupName": "ec2-group",
        "GroupId": "XXXXXXXXXXXXXXXXXXXXX",
        "Arn": "arn:aws:iam::000000000000:group/ec2-group",
        "CreateDate": "2020-02-12T21:43:41Z"
    }
}
```

### Group에서 User 삭제

삭제할 경우 사용하자. User를 먼저 삭제하면 Group에서 먼저 지워야한다는 메시지를 볼 수 있다.

```zsh
% aws iam remove-user-from-group --user-name ec2-user --group-name ec2-group --profile iam-user
```

### User 삭제

삭제할 경우 사용하자.

```zsh
% aws iam delete-user --user-name ec2-user --profile iam-user
```

## IAM User의 Access Key ID & Secret Access Key 발급

이렇게 추가한 `ec2-user`는 `ec2-group`의 권한을 따른다.\
우리는 이어서 DynamoDB, EC2, Route53 등의 자원을 접근할 예정이고,\
이 역시 모두 AWS CLI를 통해 진행할 예정이다.

따라서, Programmatic Access를 위한 Access Key ID와 Secret Access Key를 발급해야 하는데, CLI에서 가능한다.

```zsh
% aws iam create-access-key --user-name ec2-user --profile iam-user
{
    "AccessKey": {
        "UserName": "ec2-user",
        "AccessKeyId": "XXXXXXXXXXXXXXXXXXXX",
        "Status": "Active",
        "SecretAccessKey": "XxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXx",
        "CreateDate": "2020-02-12T22:34:18Z"
    }
}
```

처음 `iam-user` Profile을 설정했을때와 동일하게 이번에는 `ec2-user` Profile을 설정해본다.

행여 Access Key 정보를 분실했거나 유출되는 등, 재발급 할 일이 있는 경우 아래와 같이 삭제하고 다시 발급하면 된다.

```zsh
% aws iam delete-access-key --user-name ec2-user --access-key-id XXXXXXXXXXXXXXXXXXXX --profile iam-user
```

아래와 같이 CLI에 `ec2-user` Profile을 생성하여 CLI 환경에서 `--profile ec2-user`를 사용할 수 있게 준비한다.

```zsh
% aws configure --profile ec2-user
AWS Access Key ID [None]: XXXXXXXXXXXXXXXXXXXX
AWS Secret Access Key [None]: XxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXxXx
Default region name [eu-north-1]: eu-north-1
Default output format [json]:
```

`iam-user`와 이번에 만든 `ec2-user` Profile간 권한은 이렇게 비교해볼 수 있다.

```zsh
% aws ec2 describe-instances
An error occurred (AuthFailure) when calling the DescribeInstances operation: AWS was not able to validate the provided access credentials

% aws ec2 describe-instances --profile iam-user
An error occurred (UnauthorizedOperation) when calling the DescribeInstances operation: You are not authorized to perform this operation.

% aws ec2 describe-instances --profile ec2-user
{
    "Reservations": []
}

% aws iam list-users --profile ec2-user
An error occurred (AccessDenied) when calling the ListUsers operation: User: arn:aws:iam::000000000000:user/ec2-user is not authorized to perform: iam:ListUsers on resource: arn:aws:iam::000000000000:user/
```

## IAM Role 생성

Group과 User이외에 Role이 필요한데, 이는 특정 Service에 Policy를 부여하기 위해 존재한다.\
주로 AssumeRole을 특정 서비스에 허용하는데, 이를 통해 해당 서비스는 임시 권한을 가질 수 있다.

`ec2-dynamodb-role`이라는 이름으로 해당 Role을 만들어보자.

```zsh
% aws iam create-role --role-name ec2-dynamodb-role --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": {
    "Effect": "Allow",
    "Principal": {"Service": "ec2.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }
}' --profile iam-user
{
    "Role": {
        "Path": "/",
        "RoleName": "ec2-dynamodb-role",
        "RoleId": "XXXXXXXXXXXXXXXXXXXXX",
        "Arn": "arn:aws:iam::000000000000:role/ec2-dynamodb-role",
        "CreateDate": "2020-02-12T22:56:43Z",
        "AssumeRolePolicyDocument": {
            "Version": "2012-10-17",
            "Statement": {
                "Effect": "Allow",
                "Principal": {
                    "Service": "ec2.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        }
    }
}
```

방금 만든 Role에 `AmazonDynamoDBFullAccess` Policy를 적용한다.\
Policy Document의 양이 제법 긴데, 이건 AWS에 있는 Policy를 그대로 가져온 내용이다.

```zsh
% aws iam put-role-policy --role-name ec2-dynamodb-role --policy-name AmazonDynamoDBFullAccess --profile iam-user --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "dynamodb:*",
                "dax:*",
                "application-autoscaling:DeleteScalingPolicy",
                "application-autoscaling:DeregisterScalableTarget",
                "application-autoscaling:DescribeScalableTargets",
                "application-autoscaling:DescribeScalingActivities",
                "application-autoscaling:DescribeScalingPolicies",
                "application-autoscaling:PutScalingPolicy",
                "application-autoscaling:RegisterScalableTarget",
                "cloudwatch:DeleteAlarms",
                "cloudwatch:DescribeAlarmHistory",
                "cloudwatch:DescribeAlarms",
                "cloudwatch:DescribeAlarmsForMetric",
                "cloudwatch:GetMetricStatistics",
                "cloudwatch:ListMetrics",
                "cloudwatch:PutMetricAlarm",
                "datapipeline:ActivatePipeline",
                "datapipeline:CreatePipeline",
                "datapipeline:DeletePipeline",
                "datapipeline:DescribeObjects",
                "datapipeline:DescribePipelines",
                "datapipeline:GetPipelineDefinition",
                "datapipeline:ListPipelines",
                "datapipeline:PutPipelineDefinition",
                "datapipeline:QueryObjects",
                "ec2:DescribeVpcs",
                "ec2:DescribeSubnets",
                "ec2:DescribeSecurityGroups",
                "iam:GetRole",
                "iam:ListRoles",
                "kms:DescribeKey",
                "kms:ListAliases",
                "sns:CreateTopic",
                "sns:DeleteTopic",
                "sns:ListSubscriptions",
                "sns:ListSubscriptionsByTopic",
                "sns:ListTopics",
                "sns:Subscribe",
                "sns:Unsubscribe",
                "sns:SetTopicAttributes",
                "lambda:CreateFunction",
                "lambda:ListFunctions",
                "lambda:ListEventSourceMappings",
                "lambda:CreateEventSourceMapping",
                "lambda:DeleteEventSourceMapping",
                "lambda:GetFunctionConfiguration",
                "lambda:DeleteFunction",
                "resource-groups:ListGroups",
                "resource-groups:ListGroupResources",
                "resource-groups:GetGroup",
                "resource-groups:GetGroupQuery",
                "resource-groups:DeleteGroup",
                "resource-groups:CreateGroup",
                "tag:GetResources"
            ],
            "Effect": "Allow",
            "Resource": "*"
        },
        {
            "Action": "cloudwatch:GetInsightRuleReport",
            "Effect": "Allow",
            "Resource": "arn:aws:cloudwatch:*:*:insight-rule/DynamoDBContributorInsights*"
        },
        {
            "Action": [
                "iam:PassRole"
            ],
            "Effect": "Allow",
            "Resource": "*",
            "Condition": {
                "StringLike": {
                    "iam:PassedToService": [
                        "application-autoscaling.amazonaws.com",
                        "dax.amazonaws.com"
                    ]
                }
            }
        },
        {
            "Effect": "Allow",
            "Action": [
                "iam:CreateServiceLinkedRole"
            ],
            "Resource": "*",
            "Condition": {
                "StringEquals": {
                    "iam:AWSServiceName": [
                        "replication.dynamodb.amazonaws.com",
                        "dax.amazonaws.com",
                        "dynamodb.application-autoscaling.amazonaws.com",
                        "contributorinsights.dynamodb.amazonaws.com"
                    ]
                }
            }
        }
    ]
}' --profile iam-user
```

## Instance Profile 생성

Instance Profile은 Access Key 없이도 EC2에서 AWS 서비스를 이용할 수 있도록 신뢰관계를 맺어준다.\
이를 통해 사용자는 Access Key를 Production 환경에 노출시키지 않고도 AWS 서비스를 사용할 수 있다.\
적용되는 Policy는 해당 Instance Profile에 연결된 Role에 의해 결정된다.

`ec2-dynamodb-instance-profile`라는 이름으로 Instance Profile을 생성한다.

```zsh
% aws iam create-instance-profile \
    --instance-profile-name ec2-dynamodb-instance-profile \
    --profile iam-user
{
    "InstanceProfile": {
        "Path": "/",
        "InstanceProfileName": "ec2-dynamodb-instance-profile",
        "InstanceProfileId": "XXXXXXXXXXXXXXXXXXXXX",
        "Arn": "arn:aws:iam::000000000000:instance-profile/ec2-dynamodb-instance-profile",
        "CreateDate": "2020-02-12T23:07:27Z",
        "Roles": []
    }
}
```

해당 Instance Profile을 `ec2-dynamodb-role`과 연결한다.

```zsh
% aws iam add-role-to-instance-profile \
    --instance-profile-name ec2-dynamodb-instance-profile \
    --role-name ec2-dynamodb-role \
    --profile iam-user
```

적용된 Instance Profile을 확인한다.

```zsh
% aws iam list-instance-profiles --profile iam-user
```

## Instance Profile 삭제

Instance Profile을 삭제해야 할 경우, 먼저 Role을 Instance Profile에서 지운다.

```zsh
% aws iam remove-role-from-instance-profile \
    --instance-profile-name ec2-dynamodb-instance-profile \
    --role-name ec2-dynamodb-role \
    --profile iam-user
```

이후 해당 Instance Profile을 지울 수 있다.

```zsh
% aws iam delete-instance-profile \
    --instance-profile-name ec2-dynamodb-instance-profile \
    --profile iam-user
```

이렇게 해서, EC2에 서버를 배포하는 CLI를 수행할 준비가 되었다.

import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';

export class DataPlatformStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const imageTag = this.node.tryGetContext('imageTag') ?? 'latest';

    const vpc = new ec2.Vpc(this, 'DataPlatformVpc', {
      maxAzs: 2,
      natGateways: 1
    });

    const cluster = new ecs.Cluster(this, 'DataPlatformCluster', {
      vpc,
      clusterName: 'data-platform-cluster'
    });

    const ingestionRepo = new ecr.Repository(this, 'IngestionRepo', {
      repositoryName: 'data-platform-ingestion'
    });

    const controlRepo = new ecr.Repository(this, 'ControlRepo', {
      repositoryName: 'data-platform-control'
    });

    const consumerRepo = new ecr.Repository(this, 'ConsumerRepo', {
      repositoryName: 'data-platform-consumer'
    });

    const retryRepo = new ecr.Repository(this, 'RetryRepo', {
      repositoryName: 'data-platform-retry'
    });

    const stream = new kinesis.Stream(this, 'PaymentEventStream', {
      streamName: 'payment-events-stream',
      shardCount: 1,
      retentionPeriod: cdk.Duration.hours(24)
    });

    const dlq = new sqs.Queue(this, 'RetryDlq', {
      queueName: 'payment-retry-dlq',
      retentionPeriod: cdk.Duration.days(14)
    });

    const retryQueue = new sqs.Queue(this, 'RetryQueue', {
      queueName: 'payment-retry-queue',
      visibilityTimeout: cdk.Duration.seconds(60),
      retentionPeriod: cdk.Duration.days(7),
      deadLetterQueue: {
        maxReceiveCount: 5,
        queue: dlq
      }
    });

    const dataLakeBucket = new s3.Bucket(this, 'DataLakeBucket', {
      bucketName: `${cdk.Stack.of(this).stackName.toLowerCase()}-data-lake-${this.account}`,
      encryption: s3.BucketEncryption.S3_MANAGED,
      versioned: true,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      enforceSSL: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      autoDeleteObjects: false
    });

    const frontendBucket = new s3.Bucket(this, 'FrontendBucket', {
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      versioned: false,
      enforceSSL: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      autoDeleteObjects: false
    });

    const distribution = new cloudfront.Distribution(this, 'FrontendDistribution', {
      defaultBehavior: {
        origin: new origins.S3Origin(frontendBucket)
      },
      defaultRootObject: 'index.html'
    });

    const dbSecurityGroup = new ec2.SecurityGroup(this, 'DbSecurityGroup', {
      vpc,
      description: 'Security group for RDS PostgreSQL'
    });

    const appSecurityGroup = new ec2.SecurityGroup(this, 'AppSecurityGroup', {
      vpc,
      description: 'Security group for ECS services'
    });

    dbSecurityGroup.addIngressRule(appSecurityGroup, ec2.Port.tcp(5432), 'Allow ECS tasks to connect to Postgres');

    const db = new rds.DatabaseInstance(this, 'DataPlatformPostgres', {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_16
      }),
      vpc,
      credentials: rds.Credentials.fromGeneratedSecret('dataplatform'),
      allocatedStorage: 20,
      maxAllocatedStorage: 100,
      databaseName: 'dataplatform',
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      multiAz: false,
      publiclyAccessible: false,
      securityGroups: [dbSecurityGroup],
      backupRetention: cdk.Duration.days(1),
      removalPolicy: cdk.RemovalPolicy.SNAPSHOT
    });

    const alb = new elbv2.ApplicationLoadBalancer(this, 'ApiAlb', {
      vpc,
      internetFacing: true,
      securityGroup: appSecurityGroup,
      loadBalancerName: 'data-platform-alb'
    });

    const listener = alb.addListener('HttpListener', {
      port: 80,
      open: true,
      defaultAction: elbv2.ListenerAction.fixedResponse(404, {
        contentType: 'text/plain',
        messageBody: 'Not found'
      })
    });

    const ingestionLogGroup = new logs.LogGroup(this, 'IngestionLogs', {
      logGroupName: '/ecs/data-platform/ingestion',
      retention: logs.RetentionDays.ONE_MONTH
    });

    const controlLogGroup = new logs.LogGroup(this, 'ControlLogs', {
      logGroupName: '/ecs/data-platform/control',
      retention: logs.RetentionDays.ONE_MONTH
    });

    const consumerLogGroup = new logs.LogGroup(this, 'ConsumerLogs', {
      logGroupName: '/ecs/data-platform/consumer',
      retention: logs.RetentionDays.ONE_MONTH
    });

    const retryLogGroup = new logs.LogGroup(this, 'RetryLogs', {
      logGroupName: '/ecs/data-platform/retry',
      retention: logs.RetentionDays.ONE_MONTH
    });

    const ingestionTaskRole = new iam.Role(this, 'IngestionTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    stream.grantWrite(ingestionTaskRole);
    dataLakeBucket.grantPut(ingestionTaskRole, 'raw/*');

    const controlTaskRole = new iam.Role(this, 'ControlTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    retryQueue.grantSendMessages(controlTaskRole);
    dlq.grantConsumeMessages(controlTaskRole);
    dataLakeBucket.grantRead(controlTaskRole, 'raw/*');
    stream.grantWrite(controlTaskRole);

    const consumerTaskRole = new iam.Role(this, 'ConsumerTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    stream.grantRead(consumerTaskRole);
    retryQueue.grantSendMessages(consumerTaskRole);
    dlq.grantSendMessages(consumerTaskRole);
    dataLakeBucket.grantPut(consumerTaskRole, 'processed/*');
    consumerTaskRole.addToPolicy(new iam.PolicyStatement({
      actions: [
        'dynamodb:CreateTable',
        'dynamodb:DescribeTable',
        'dynamodb:UpdateItem',
        'dynamodb:GetItem',
        'dynamodb:PutItem',
        'dynamodb:DeleteItem',
        'dynamodb:Scan',
        'dynamodb:Query'
      ],
      resources: ['*']
    }));
    consumerTaskRole.addToPolicy(new iam.PolicyStatement({
      actions: ['cloudwatch:PutMetricData'],
      resources: ['*']
    }));

    const retryTaskRole = new iam.Role(this, 'RetryTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    retryQueue.grantConsumeMessages(retryTaskRole);
    retryQueue.grantSendMessages(retryTaskRole);
    dlq.grantSendMessages(retryTaskRole);
    dataLakeBucket.grantPut(retryTaskRole, 'processed/*');

    const dbUrl = `jdbc:postgresql://${db.instanceEndpoint.hostname}:5432/dataplatform`;

    const ingestionTask = new ecs.FargateTaskDefinition(this, 'IngestionTaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
      taskRole: ingestionTaskRole
    });

    ingestionTask.addContainer('IngestionContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ingestionRepo, imageTag),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'ingestion',
        logGroup: ingestionLogGroup
      }),
      environment: {
        SERVER_PORT: '8080',
        AWS_REGION: this.region,
        KINESIS_STREAM_NAME: stream.streamName,
        DATA_LAKE_BUCKET: dataLakeBucket.bucketName
      },
      portMappings: [{ containerPort: 8080 }]
    });

    const controlTask = new ecs.FargateTaskDefinition(this, 'ControlTaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
      taskRole: controlTaskRole
    });

    const controlContainer = controlTask.addContainer('ControlContainer', {
      image: ecs.ContainerImage.fromEcrRepository(controlRepo, imageTag),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'control',
        logGroup: controlLogGroup
      }),
      environment: {
        SERVER_PORT: '8081',
        AWS_REGION: this.region,
        RETRY_QUEUE_URL: retryQueue.queueUrl,
        DLQ_QUEUE_URL: dlq.queueUrl,
        KINESIS_STREAM_NAME: stream.streamName,
        DATA_LAKE_BUCKET: dataLakeBucket.bucketName,
        DB_URL: dbUrl,
        SECURITY_ENABLED: 'false'
      },
      secrets: {
        DB_USERNAME: ecs.Secret.fromSecretsManager(db.secret!, 'username'),
        DB_PASSWORD: ecs.Secret.fromSecretsManager(db.secret!, 'password')
      },
      portMappings: [{ containerPort: 8081 }]
    });

    const consumerTask = new ecs.FargateTaskDefinition(this, 'ConsumerTaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
      taskRole: consumerTaskRole
    });

    consumerTask.addContainer('ConsumerContainer', {
      image: ecs.ContainerImage.fromEcrRepository(consumerRepo, imageTag),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'consumer',
        logGroup: consumerLogGroup
      }),
      environment: {
        AWS_REGION: this.region,
        DB_URL: dbUrl,
        DATA_LAKE_BUCKET: dataLakeBucket.bucketName,
        RETRY_QUEUE_URL: retryQueue.queueUrl,
        DLQ_QUEUE_URL: dlq.queueUrl,
        KINESIS_STREAM_NAME: stream.streamName,
        KCL_APPLICATION_NAME: 'payment-processor-app'
      },
      secrets: {
        DB_USERNAME: ecs.Secret.fromSecretsManager(db.secret!, 'username'),
        DB_PASSWORD: ecs.Secret.fromSecretsManager(db.secret!, 'password')
      }
    });

    const retryTask = new ecs.FargateTaskDefinition(this, 'RetryTaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
      taskRole: retryTaskRole
    });

    retryTask.addContainer('RetryContainer', {
      image: ecs.ContainerImage.fromEcrRepository(retryRepo, imageTag),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'retry',
        logGroup: retryLogGroup
      }),
      environment: {
        AWS_REGION: this.region,
        DB_URL: dbUrl,
        DATA_LAKE_BUCKET: dataLakeBucket.bucketName,
        RETRY_QUEUE_URL: retryQueue.queueUrl,
        DLQ_QUEUE_URL: dlq.queueUrl
      },
      secrets: {
        DB_USERNAME: ecs.Secret.fromSecretsManager(db.secret!, 'username'),
        DB_PASSWORD: ecs.Secret.fromSecretsManager(db.secret!, 'password')
      }
    });

    const ingestionService = new ecs.FargateService(this, 'IngestionService', {
      cluster,
      taskDefinition: ingestionTask,
      desiredCount: 1,
      assignPublicIp: false,
      securityGroups: [appSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    const controlService = new ecs.FargateService(this, 'ControlService', {
      cluster,
      taskDefinition: controlTask,
      desiredCount: 1,
      assignPublicIp: false,
      securityGroups: [appSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    const consumerService = new ecs.FargateService(this, 'ConsumerService', {
      cluster,
      taskDefinition: consumerTask,
      desiredCount: 1,
      assignPublicIp: false,
      securityGroups: [appSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    const retryService = new ecs.FargateService(this, 'RetryService', {
      cluster,
      taskDefinition: retryTask,
      desiredCount: 1,
      assignPublicIp: false,
      securityGroups: [appSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    listener.addTargets('IngestionTargets', {
      priority: 10,
      conditions: [elbv2.ListenerCondition.pathPatterns(['/events'])],
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [ingestionService],
      healthCheck: {
        path: '/actuator/health',
        healthyHttpCodes: '200'
      }
    });

    listener.addTargets('ControlTargets', {
      priority: 20,
      conditions: [
        elbv2.ListenerCondition.pathPatterns([
          '/summary*',
          '/errors*',
          '/dlq*',
          '/replay/*',
          '/schemas*',
          '/config*'
        ])
      ],
      port: 8081,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [controlService],
      healthCheck: {
        path: '/actuator/health',
        healthyHttpCodes: '200'
      }
    });

    new cdk.CfnOutput(this, 'AlbUrl', {
      value: `http://${alb.loadBalancerDnsName}`
    });

    new cdk.CfnOutput(this, 'FrontendBucketName', {
      value: frontendBucket.bucketName
    });

    new cdk.CfnOutput(this, 'CloudFrontUrl', {
      value: `https://${distribution.distributionDomainName}`
    });

    new cdk.CfnOutput(this, 'KinesisStreamName', {
      value: stream.streamName
    });

    new cdk.CfnOutput(this, 'RetryQueueUrl', {
      value: retryQueue.queueUrl
    });

    new cdk.CfnOutput(this, 'DlqQueueUrl', {
      value: dlq.queueUrl
    });

    new cdk.CfnOutput(this, 'DataLakeBucketName', {
      value: dataLakeBucket.bucketName
    });

    new cdk.CfnOutput(this, 'RdsEndpoint', {
      value: db.instanceEndpoint.hostname
    });

    new cdk.CfnOutput(this, 'IngestionEcrRepoUri', {
      value: ingestionRepo.repositoryUri
    });

    new cdk.CfnOutput(this, 'ControlEcrRepoUri', {
      value: controlRepo.repositoryUri
    });

    new cdk.CfnOutput(this, 'ConsumerEcrRepoUri', {
      value: consumerRepo.repositoryUri
    });

    new cdk.CfnOutput(this, 'RetryEcrRepoUri', {
      value: retryRepo.repositoryUri
    });
  }
}

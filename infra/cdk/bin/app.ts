#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { DataPlatformStack } from '../lib/data-platform-stack';

const app = new cdk.App();

new DataPlatformStack(app, 'DataPlatformStack', {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION ?? 'us-east-1'
  }
});

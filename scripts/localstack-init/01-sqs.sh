#!/bin/sh
# Runs inside the LocalStack container once it is ready (awslocal = aws CLI already
# pointed at it). The DLQ comes first: the main queue's redrive policy needs its ARN.
# 000000000000 / us-east-1 are LocalStack's default account and region.
awslocal sqs create-queue --queue-name dbook-booking-expiration-dlq

awslocal sqs create-queue --queue-name dbook-booking-expiration \
  --attributes '{"VisibilityTimeout":"30","RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:dbook-booking-expiration-dlq\",\"maxReceiveCount\":\"3\"}"}'
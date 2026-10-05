# One queue per kind of work, each with its dead-letter queue: a message that fails max_receive_count times
# is moved there, where it is seen (the dbook.sqs.dlq.depth gauge and the DeadLetterQueueNotEmpty alert) instead of
# being retried forever.
resource "aws_sqs_queue" "dlq" {
  for_each = var.queues

  name                      = "${var.name}-${each.key}-dlq"
  message_retention_seconds = 1209600 # 14 days, the maximum: time to look at what failed
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "main" {
  for_each = var.queues

  name                       = "${var.name}-${each.key}"
  visibility_timeout_seconds = var.visibility_timeout_seconds
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

# A message in a dead-letter queue is work that did not get done (an expiration, a notification): the alarm is the
# AWS-side twin of the dbook.sqs.dlq.depth gauge and its DeadLetterQueueNotEmpty alert.
resource "aws_cloudwatch_metric_alarm" "dead_letters" {
  for_each = var.queues

  alarm_name          = "${var.name}-${each.key}-dlq-not-empty"
  alarm_description   = "Messages are landing in the dead-letter queue of ${each.key}"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  dimensions          = { QueueName = aws_sqs_queue.dlq[each.key].name }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = var.alarm_actions
}

# What the application may do with the queues: send and receive and delete on the main ones (the outbox relay and the
# consumers) and read the depth of the dead-letter ones (the gauge). Nothing else.
data "aws_iam_policy_document" "app" {
  statement {
    sid       = "UseTheQueues"
    actions   = ["sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl"]
    resources = [for queue in aws_sqs_queue.main : queue.arn]
  }

  statement {
    sid       = "ReadTheDeadLetterDepth"
    actions   = ["sqs:GetQueueAttributes", "sqs:GetQueueUrl"]
    resources = [for queue in aws_sqs_queue.dlq : queue.arn]
  }
}

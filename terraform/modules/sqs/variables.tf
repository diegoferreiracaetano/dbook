variable "name" {
  description = "Name prefix for the queues."
  type        = string
}

variable "queues" {
  description = "Queue names (without the prefix); each gets a dead-letter queue."
  type        = set(string)
  default     = ["booking-expiration", "notifications"]
}

variable "max_receive_count" {
  description = "How many times a message is delivered before it is moved to the dead-letter queue."
  type        = number
  default     = 3
}

variable "visibility_timeout_seconds" {
  description = "How long a received message stays invisible while the consumer handles it."
  type        = number
  default     = 30
}

variable "alarm_actions" {
  description = "ARNs notified (an SNS topic, for instance) when a dead-letter queue is not empty."
  type        = list(string)
  default     = []
}

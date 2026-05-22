-- IAM: transactional outbox for email (SMTP relay, templates). Separate from Kafka domain outbox.

CREATE TABLE iam_email_outbox (
  id UUID PRIMARY KEY,
  template_id VARCHAR(120) NOT NULL,
  to_address VARCHAR(320) NOT NULL,
  subject VARCHAR(998),
  payload_json TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  sent_at TIMESTAMPTZ
);

CREATE INDEX iam_email_outbox_status_created_idx ON iam_email_outbox (status, created_at);

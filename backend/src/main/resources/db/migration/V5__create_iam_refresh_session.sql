-- IAM3: server-side refresh sessions (opaque tokens, rotation, reuse detection via family revoke).

CREATE TABLE iam_refresh_session (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
  family_id UUID NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX iam_refresh_session_token_hash_uidx ON iam_refresh_session (token_hash);
CREATE INDEX iam_refresh_session_user_id_idx ON iam_refresh_session (user_id);
CREATE INDEX iam_refresh_session_family_id_idx ON iam_refresh_session (family_id);
CREATE INDEX iam_refresh_session_expires_at_idx ON iam_refresh_session (expires_at);

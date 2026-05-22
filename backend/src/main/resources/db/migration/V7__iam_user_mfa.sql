-- MFA (TOTP) for privileged admin accounts; secrets stored encrypted at rest.
ALTER TABLE iam_user ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE iam_user ADD COLUMN mfa_secret_encrypted VARCHAR(4000);
ALTER TABLE iam_user ADD COLUMN mfa_pending_secret_encrypted VARCHAR(4000);
